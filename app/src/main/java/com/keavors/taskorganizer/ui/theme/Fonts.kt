package com.keavors.taskorganizer.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.keavors.taskorganizer.R

/** A variable font file: every weight comes from the same file (its "wght" axis). */
private fun variable(res: Int) = FontFamily(listOf(100, 200, 300, 400, 500, 600, 700, 800, 900).map { Font(res, FontWeight(it)) })

/** Fonts bundled with the app (SIL Open Font License, texts in assets/licenses). All have Cyrillic. */
object AppFonts {
    val Manrope by lazy { variable(R.font.manrope) }
    val Nunito by lazy { variable(R.font.nunito) }
    val Oswald by lazy { variable(R.font.oswald) }
    val RobotoCondensed by lazy { variable(R.font.roboto_condensed) }
    val JetBrainsMono by lazy { variable(R.font.jetbrains_mono) }
    val Caveat by lazy { variable(R.font.caveat) }
    val Exo2 by lazy { variable(R.font.exo2) }
}
