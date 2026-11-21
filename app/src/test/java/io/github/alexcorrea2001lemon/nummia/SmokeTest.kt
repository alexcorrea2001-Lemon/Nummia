package io.github.alexcorrea2001lemon.nummia

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun versionCodeSigueLaFormula() {
        val (mayor, menor, correccion) = BuildConfig.VERSION_NAME.removeSuffix("-debug").split(".").map { it.toInt() }
        assertEquals(mayor * 10000 + menor * 100 + correccion, BuildConfig.VERSION_CODE)
    }
}
