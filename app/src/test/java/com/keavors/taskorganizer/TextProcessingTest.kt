package com.keavors.taskorganizer

import com.keavors.taskorganizer.data.DictionaryTerm
import com.keavors.taskorganizer.text.Cleanup
import com.keavors.taskorganizer.text.CorrectionSuggestion
import com.keavors.taskorganizer.text.Corrections
import com.keavors.taskorganizer.text.Dictionary
import com.keavors.taskorganizer.text.Entries
import com.keavors.taskorganizer.text.RecognitionPrompt
import com.keavors.taskorganizer.text.Russian
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextProcessingTest {
    private fun term(text: String, vararg variants: String) = DictionaryTerm(text, text, variants.toList())

    private val dict = listOf(term("Docker", "докер"), term("Google Drive", "гугл драйв"), term("Steam", "стим"), term("Notion", "наушен"))

    @Test fun replacesWholeWordsIgnoringCase() =
        assertEquals("Настроить Docker на сервере", Dictionary.apply("Настроить Докер на сервере", dict))

    @Test fun handlesRussianEndings() {
        assertEquals("урок по Docker и в Docker", Dictionary.apply("урок по докеру и в докере", dict))
        assertEquals("файлы в Google Drive", Dictionary.apply("файлы в гугл драйве", dict))
    }

    @Test fun leavesLongerWordsAlone() =
        assertEquals("нужен стимул поиграть в Steam", Dictionary.apply("нужен стимул поиграть в стим", dict))

    @Test fun hyphensAndSpacesAreInterchangeable() =
        assertEquals("скинуть в Google Drive", Dictionary.apply("скинуть в гугл-драйв", dict))

    @Test fun yoIsTheSameAsYe() = assertEquals("заметки из Notion", Dictionary.apply("заметки из наушён", dict))

    @Test fun exactFormsOnly() = assertEquals("урок по докеру", Dictionary.apply("урок по докеру", dict, inflections = false))

    @Test fun dollarSignIsLiteral() = assertEquals("купить \$cache", Dictionary.apply("купить кеш", listOf(term("\$cache", "кеш"))))

    @Test fun suggestsCorrectionWithoutEnding() = assertEquals(
        listOf(CorrectionSuggestion("докер", "Docker")),
        Corrections.suggest("Посмотреть урок по докеру", "Посмотреть урок по Docker")
    )

    @Test fun suggestsMultiWordCorrection() = assertEquals(
        listOf(CorrectionSuggestion("гугл драйв", "Google Drive")),
        Corrections.suggest("Скинуть файлы в гугл драйв", "Скинуть файлы в Google Drive")
    )

    @Test fun ignoresOrdinaryEditsAndFrequentWords() {
        assertEquals(emptyList<CorrectionSuggestion>(), Corrections.suggest("Купить молоко", "Купить хлеб"))
        assertEquals(emptyList<CorrectionSuggestion>(), Corrections.suggest("нажать три раза", "нажать Retry раза"))
        assertEquals(emptyList<CorrectionSuggestion>(), Corrections.suggest("Обновить Windows", "Обновить Windows"))
    }

    @Test fun dropsWhisperHallucinations() {
        assertEquals("", Cleanup.clean("Продолжение следует..."))
        assertEquals("", Cleanup.clean("Субтитры сделал DimaTorzok"))
        assertEquals("Купить хлеб.", Cleanup.clean("Купить хлеб. Редактор субтитров А.Синецкая Корректор А.Егорова"))
        assertEquals("", Cleanup.clean("[Музыка]"))
    }

    @Test fun keepsNormalText() {
        assertEquals("Позвонить маме", Cleanup.clean("  Позвонить   маме "))
        assertEquals("Сказать спасибо за внимание коллегам", Cleanup.clean("Сказать спасибо за внимание коллегам"))
    }

    @Test fun promptListsUniqueTermsWithinLimit() {
        assertEquals(RecognitionPrompt.STYLE, RecognitionPrompt.build(emptyList()))
        assertEquals("${RecognitionPrompt.STYLE} Термины: Docker, GitHub.", RecognitionPrompt.build(listOf("Docker", "docker", " GitHub ")))
        assertTrue(RecognitionPrompt.build((1..200).map { "Term$it" }).length <= RecognitionPrompt.MAX_CHARS)
    }

    @Test fun splitsLinesAndBullets() = assertEquals(
        listOf("купить хлеб", "позвонить", "написать", "прочитать"),
        Entries.split("- купить хлеб\n\n2) позвонить\n• написать\n[ ] прочитать")
    )

    @Test fun tidiesDictation() {
        assertEquals("Купить хлеб", Entries.tidy("  купить   хлеб "))
        assertEquals("iPhone зарядить", Entries.tidy("iPhone зарядить"))
        assertEquals("Обновить Windows", Entries.stripLonePeriod("Обновить Windows."))
        assertEquals("Раз. Два.", Entries.stripLonePeriod("Раз. Два."))
    }

    @Test fun stripsNounEndingsOnlyWithStem() {
        assertEquals("докер", Russian.stripNounEnding("докера"))
        assertEquals("гитхаб", Russian.stripNounEnding("гитхабе"))
        assertEquals("зум", Russian.stripNounEnding("зуме"))
        assertEquals("ему", Russian.stripNounEnding("ему"))
        assertEquals("Docker", Russian.stripNounEnding("Docker"))
    }
}
