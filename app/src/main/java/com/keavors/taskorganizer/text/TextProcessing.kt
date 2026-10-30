package com.keavors.taskorganizer.text

import com.keavors.taskorganizer.data.DictionaryTerm
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

/** Russian word forms. */
object Russian {
    /** Case and number endings Russian adds to borrowed nouns ("докер" -> "докера", "докером"). Longest first. */
    val NOUN_ENDINGS = listOf(
        "ами", "ями",
        "ов", "ев", "ей", "ам", "ям", "ах", "ях", "ом", "ем", "ём", "ой", "ою", "ую",
        "а", "я", "у", "ю", "е", "ы", "и"
    )

    private val CYRILLIC_WORD = Regex("^[а-яёА-ЯЁ]+$")

    /** Strips one noun ending if a stem of at least [minStem] letters remains. */
    fun stripNounEnding(word: String, minStem: Int = 3): String {
        if (!CYRILLIC_WORD.matches(word)) return word
        val lower = word.lowercase()
        for (ending in NOUN_ENDINGS) {
            if (lower.endsWith(ending) && lower.length - ending.length >= minStem) return word.dropLast(ending.length)
        }
        return word
    }

    /** Strips the ending of the last word ("гугл драйве" -> "гугл драйв"). */
    fun stripPhraseEnding(phrase: String): String {
        val words = phrase.trim().split(Regex("\\s+")).toMutableList()
        words[words.lastIndex] = stripNounEnding(words.last())
        return words.joinToString(" ")
    }

    fun hasCyrillic(s: String) = s.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }
    fun hasLatin(s: String) = s.any { it in 'a'..'z' || it in 'A'..'Z' }
}

/** Replaces misrecognized words with the spelling from the user's dictionary. */
object Dictionary {
    private const val WORD_CHAR = "[\\p{L}\\p{N}]"
    private const val SEPARATOR = "[\\s\\-–—]+"

    private fun charPattern(ch: Char): String = if (ch == 'е' || ch == 'ё') "[её]" else Pattern.quote(ch.toString())

    /**
     * Pattern for [variant] as a whole word or phrase: case-insensitive, "е" = "ё",
     * spaces and hyphens interchangeable; with [inflections] a Russian noun ending may follow.
     */
    fun variantPattern(variant: String, inflections: Boolean): Pattern? {
        val words = variant.lowercase().split(Regex("[\\s\\-–—]+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val body = words.joinToString(SEPARATOR) { w -> w.map(::charPattern).joinToString("") }
        val last = words.last().last()
        val ending = if (inflections && (last in 'а'..'я' || last == 'ё')) "(?:" + Russian.NOUN_ENDINGS.joinToString("|") + ")?" else ""
        // No UNICODE_CHARACTER_CLASS: Android's regex engine rejects it; \p{L} is Unicode-aware anyway.
        return Pattern.compile("(?<!$WORD_CHAR)$body$ending(?!$WORD_CHAR)", Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
    }

    fun apply(text: String, terms: List<DictionaryTerm>, inflections: Boolean = true): String {
        val rules = terms.flatMap { term ->
            val to = term.text.trim()
            term.variants.map { it.trim() }.filter { to.isNotEmpty() && it.isNotEmpty() && !it.equals(to, ignoreCase = true) }
                .map { it to to }
        }.sortedByDescending { it.first.length } // "гугл драйв" before "гугл"
        var out = text
        for ((from, to) in rules) {
            val pattern = variantPattern(from, inflections) ?: continue
            out = pattern.matcher(out).replaceAll(Matcher.quoteReplacement(to))
        }
        return out
    }
}

/** A correction the user made to dictated text, worth remembering: "докер" -> "Docker". */
data class CorrectionSuggestion(val from: String, val to: String)

object Corrections {
    private val WORD = Regex("[\\p{L}\\p{N}]+(?:[-'’+.#][\\p{L}\\p{N}]+)*[+#]*")

    // Frequent Russian words: replacing them everywhere would do more harm than good.
    private val STOP_WORDS = (
        "и в во не что он на я с со как а то все она так его но да ты к у же вы за бы по только ее мне было вот от меня еще нет о из ему " +
            "теперь когда даже ну вдруг ли если уже или ни быть был него до вас нибудь опять уж вам ведь там потом себя ничего ей может они " +
            "тут где есть надо ней для мы тебя их чем была сам чтоб без будто чего раз тоже себе под будет ж тогда кто этот того потому этого " +
            "какой совсем ним здесь этом один почти мой тем чтобы нее сейчас были куда зачем всех никогда можно при наконец два об другой хоть " +
            "после над больше тот через эти нас про всего них какая много разве три эту моя впрочем хорошо свою этой перед иногда лучше чуть " +
            "том нельзя такой им более всегда конечно всю между четыре пять шесть семь восемь девять десять сто тысяча день неделя час"
        ).split(" ").toSet()

    private fun words(s: String): List<String> = WORD.findAll(s).map { it.value }.toList()

    private class Hunk(val removed: MutableList<String> = mutableListOf(), val added: MutableList<String> = mutableListOf())

    /** Word-level diff (longest common subsequence) grouped into hunks of consecutive changes. */
    private fun hunks(a: List<String>, b: List<String>): List<Hunk> {
        val al = a.map { it.lowercase() }
        val bl = b.map { it.lowercase() }
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
            lcs[i][j] = if (al[i] == bl[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val out = mutableListOf<Hunk>()
        var cur: Hunk? = null
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            when {
                i < a.size && j < b.size && al[i] == bl[j] -> {
                    cur?.let { out += it }
                    cur = null
                    i++
                    j++
                }
                j < b.size && (i == a.size || lcs[i][j + 1] >= lcs[i + 1][j]) -> {
                    cur = (cur ?: Hunk()).also { it.added += b[j++] }
                }
                else -> cur = (cur ?: Hunk()).also { it.removed += a[i++] }
            }
        }
        cur?.let { out += it }
        return out
    }

    /** Cyrillic words the user replaced with Latin ones between recognition and saving. */
    fun suggest(recognized: String, edited: String): List<CorrectionSuggestion> {
        val a = words(recognized)
        val b = words(edited)
        if (a.isEmpty() || b.isEmpty() || a.size > 300 || b.size > 300) return emptyList()
        val seen = mutableSetOf<String>()
        return hunks(a, b).mapNotNull { h ->
            if (h.removed.size !in 1..3 || h.added.size !in 1..3) return@mapNotNull null
            val fromRaw = h.removed.joinToString(" ")
            val to = h.added.joinToString(" ")
            if (!Russian.hasCyrillic(fromRaw) || Russian.hasLatin(fromRaw) || !Russian.hasLatin(to)) return@mapNotNull null
            if (h.removed.size == 1 && fromRaw.lowercase() in STOP_WORDS) return@mapNotNull null
            val from = Russian.stripPhraseEnding(fromRaw.lowercase())
            if (seen.add("$from\u0000${to.lowercase()}")) CorrectionSuggestion(from, to) else null
        }
    }
}

/** Typed or pasted text -> entries. */
object Entries {
    private val BULLET = Regex("^(?:[-–—•*·]\\s+|\\d{1,3}[.)]\\s+|\\[\\s?[xх ]?\\s?]\\s+)", RegexOption.IGNORE_CASE)

    /** One line, one entry; list bullets are dropped. */
    fun split(text: String): List<String> = text.split(Regex("\\r?\\n")).map { it.replace(BULLET, "").trim() }.filter { it.isNotEmpty() }

    /** Capital first letter and single spaces; names like "iPhone" stay as they are. */
    fun tidy(text: String): String {
        val t = text.replace(Regex("\\s+"), " ").trim()
        if (t.isEmpty() || Regex("^[a-z]+[A-Z]").containsMatchIn(t)) return t
        return t.replaceFirstChar { it.titlecase(Locale.forLanguageTag("ru")) }
    }

    /** "Обновить Windows." -> "Обновить Windows": one dictated task needs no final period. */
    fun stripLonePeriod(text: String): String = if (Regex("^[^.!?…]+\\.$").matches(text)) text.dropLast(1) else text
}
