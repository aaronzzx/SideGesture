package com.aaron.sidegesture.ui.widget

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SliderFormatterTest {

    @Test
    fun integerFormatterRoundsAndAppendsSuffix() {
        assertEquals("12 ms", formatSliderInteger(12.4f, " ms"))
        assertEquals("13 ms", formatSliderInteger(12.5f, " ms"))
        assertEquals("0", formatSliderInteger(0f))
    }

    @Test
    fun decimalFormatterUsesRequestedPrecisionAndRounding() {
        assertEquals("1.24", formatSliderDecimal(1.235f, 2))
        assertEquals("1.24×", formatSliderDecimal(1.235f, 2, "×"))
        assertEquals("0.00", formatSliderDecimal(0f, 2))
    }

    @Test
    fun percentageFormattersCoverZeroAndOne() {
        assertEquals("0%", formatSliderPercentage(0f))
        assertEquals("100%", formatSliderPercentage(1f))
    }

    @Test
    fun formattersAlwaysUseRootLocaleAndRestoreLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1.50", formatSliderDecimal(1.5f, 2))
            assertEquals("150%", formatSliderPercentage(1.5f))
        } finally {
            Locale.setDefault(original)
        }
        assertEquals(original, Locale.getDefault())
    }
}
