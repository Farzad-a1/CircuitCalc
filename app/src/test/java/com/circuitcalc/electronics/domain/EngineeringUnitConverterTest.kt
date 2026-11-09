package com.circuitcalc.electronics.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineeringUnitConverterTest {
    @Test fun allTwelveCategoriesConvertInBothDirections() {
        val cases = listOf(
            Triple("voltage", "kV", "V") to 1000.0,
            Triple("current", "kA", "A") to 1000.0,
            Triple("resistance", "MΩ", "kΩ") to 1000.0,
            Triple("capacitance", "µF", "nF") to 1000.0,
            Triple("inductance", "mH", "µH") to 1000.0,
            Triple("frequency", "GHz", "Hz") to 1e9,
            Triple("power", "MW", "W") to 1e6,
            Triple("energy", "kWh", "J") to 3.6e6,
            Triple("time", "h", "s") to 3600.0,
            Triple("length", "ft", "m") to 0.3048,
            Triple("temperature", "°C", "°F") to 32.0,
            Triple("wire-gauge", "AWG", "mm") to 1.628
        )
        cases.forEach { (conversion, expected) ->
            val (category, from, to) = conversion
            val input = if (category == "temperature") 0.0 else if (category == "wire-gauge") 14.0 else 1.0
            assertEquals("$category forward", expected, EngineeringUnitConverter.convert(input, category, from, to), 1e-8)
            val reverse = EngineeringUnitConverter.convert(expected, category, to, from)
            assertEquals("$category reverse", input, reverse, 1e-7)
        }
    }

    @Test fun unitListsCoverRequestedSymbols() {
        val expected = mapOf(
            "voltage" to listOf("V", "mV", "µV", "kV"),
            "current" to listOf("A", "mA", "µA", "kA"),
            "resistance" to listOf("Ω", "mΩ", "kΩ", "MΩ"),
            "capacitance" to listOf("F", "mF", "µF", "nF", "pF"),
            "inductance" to listOf("H", "mH", "µH", "nH"),
            "frequency" to listOf("Hz", "kHz", "MHz", "GHz"),
            "power" to listOf("W", "mW", "kW", "MW"),
            "energy" to listOf("J", "mJ", "Wh", "kWh"),
            "time" to listOf("s", "ms", "µs", "ns", "min", "h"),
            "length" to listOf("m", "mm", "µm", "cm", "km", "in", "ft"),
            "temperature" to listOf("°C", "°F", "K"),
            "wire-gauge" to listOf("AWG", "mm", "in")
        )
        assertEquals(12, EngineeringUnitConverter.categories.size)
        expected.forEach { (id, symbols) -> assertEquals(id, symbols, EngineeringUnitConverter.category(id).units.map { it.symbol }) }
    }

    @Test fun handlesZeroAndSignedLinearValues() {
        assertEquals(0.0, EngineeringUnitConverter.convert(0.0, "voltage", "mV", "kV"), 0.0)
        assertEquals(-2.5, EngineeringUnitConverter.convert(-2500.0, "voltage", "mV", "V"), 1e-12)
        assertEquals(-40.0, EngineeringUnitConverter.convert(-40.0, "temperature", "°C", "°F"), 1e-10)
        assertEquals(0.0, EngineeringUnitConverter.convert(-273.15, "temperature", "°C", "K"), 1e-10)
    }

    @Test fun rejectsNonFiniteOverflowInvalidUnitsAndBelowAbsoluteZero() {
        assertFails { EngineeringUnitConverter.convert(Double.NaN, "voltage", "V", "mV") }
        assertFails { EngineeringUnitConverter.convert(Double.POSITIVE_INFINITY, "current", "A", "mA") }
        assertFails { EngineeringUnitConverter.convert(Double.MAX_VALUE, "voltage", "kV", "µV") }
        assertFails { EngineeringUnitConverter.convert(-273.151, "temperature", "°C", "K") }
        assertFails { EngineeringUnitConverter.convert(-1.0, "temperature", "K", "°C") }
        assertFails { EngineeringUnitConverter.convert(1.0, "voltage", "V", "Ω") }
    }

    @Test fun awgSupportsStandardLabelsAndNearestNominalDiameter() {
        assertEquals(-3.0, EngineeringUnitConverter.parseInput("4/0", "wire-gauge", "AWG")!!, 0.0)
        assertEquals(-1.0, EngineeringUnitConverter.parseInput("00", "wire-gauge", "AWG")!!, 0.0)
        assertEquals(11.68, EngineeringUnitConverter.convert(-3.0, "wire-gauge", "AWG", "mm"), 0.0)
        assertEquals("4/0", EngineeringUnitConverter.formatValue(-3.0, "wire-gauge", "AWG"))
        assertEquals(14.0, EngineeringUnitConverter.convert(1.628, "wire-gauge", "mm", "AWG"), 0.0)
        assertEquals(14.0, EngineeringUnitConverter.convert(0.0641, "wire-gauge", "in", "AWG"), 0.1)
        assertFails { EngineeringUnitConverter.convert(14.5, "wire-gauge", "AWG", "mm") }
        assertFails { EngineeringUnitConverter.convert(-1.0, "wire-gauge", "mm", "AWG") }
        assertFails { EngineeringUnitConverter.convert(0.0, "wire-gauge", "mm", "in") }
        assertFails { EngineeringUnitConverter.convert(-1.0, "wire-gauge", "in", "mm") }
        assertFails { EngineeringUnitConverter.convert(0.01, "wire-gauge", "mm", "AWG") }
        assertFails { EngineeringUnitConverter.convert(41.0, "wire-gauge", "AWG", "mm") }
    }

    @Test fun searchFindsCategoryNamesUnitsAndTerms() {
        assertEquals("Capacitance", EngineeringUnitConverter.searchCategories("capacitor").single().name)
        assertEquals("Frequency", EngineeringUnitConverter.searchCategories("GHz").single().name)
        assertEquals("Wire gauge", EngineeringUnitConverter.searchCategories("american wire gauge").single().name)
        assertEquals(12, EngineeringUnitConverter.searchCategories("").size)
        assertTrue(EngineeringUnitConverter.searchCategories("nothing here").isEmpty())
    }

    @Test fun scientificFormattingKeepsSmallValuesReadable() {
        assertTrue(EngineeringUnitConverter.formatValue(1e-9, "capacitance", "F").contains("e"))
        assertEquals("0", EngineeringUnitConverter.formatValue(0.0, "voltage", "V"))
        assertFalse(EngineeringUnitConverter.formatValue(1234.5, "voltage", "V").isBlank())
    }

    private fun assertFails(action: () -> Unit) {
        var failed = false
        try { action() } catch (_: IllegalArgumentException) { failed = true }
        assertTrue("Expected conversion to reject input", failed)
    }
}
