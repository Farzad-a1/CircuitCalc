package com.circuitcalc.electronics.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinoutCatalogTest {
    @Test fun catalogHasExpectedVerifiedEntriesAndNoDuplicateIds() {
        assertEquals(16, PinoutCatalog.entries.size)
        assertEquals(PinoutCatalog.entries.size, PinoutCatalog.entries.map { it.id }.distinct().size)
        assertTrue(PinoutCatalog.integrityErrors().joinToString(), PinoutCatalog.integrityErrors().isEmpty())
    }

    @Test fun dipPackagesHaveContiguousNumberingAndExpectedPinCounts() {
        val counts = mapOf(
            "atmega328p-pu" to 28, "ne555-pdip8" to 8, "lm358ap" to 8,
            "lm324an" to 14, "sn74hc00n" to 14, "sn74hc595n" to 16,
            "uln2003an" to 16, "pc817x" to 4
        )
        counts.forEach { (id, expected) ->
            val entry = PinoutCatalog.entries.single { it.id == id }
            assertEquals("$id pin count", expected, entry.pinCount)
            assertEquals("$id numbering", (1..expected).map(Int::toString), entry.pins.map { it.number })
        }
    }

    @Test fun boardHeaderPinCountsAndConnectorCountsAreStable() {
        assertEquals(32, PinoutCatalog.entries.single { it.id == "arduino-uno-r3" }.pinCount)
        assertEquals(38, PinoutCatalog.entries.single { it.id == "esp32-devkitc-v4" }.pinCount)
        assertEquals(3, PinoutCatalog.entries.single { it.id == "pn2222ata" }.pinCount)
        assertEquals(3, PinoutCatalog.entries.single { it.id == "bc547b-nexperia" }.pinCount)
        assertEquals(3, PinoutCatalog.entries.single { it.id == "irfz44npbf" }.pinCount)
        assertEquals(4, PinoutCatalog.entries.single { it.id == "usb-a-2-receptacle" }.pinCount)
    }

    @Test fun boardConnectorNumberingIsLocalAndContiguous() {
        val uno = PinoutCatalog.entries.single { it.id == "arduino-uno-r3" }
        val unoCounts = mapOf("J_ANALOG" to 14, "J_DIGITAL" to 18)
        unoCounts.forEach { (header, expected) ->
            val pins = uno.pins.filter { it.header == header }
            assertEquals(expected, pins.size)
            assertEquals((1..expected).map(Int::toString), pins.map { it.number })
        }
        val esp = PinoutCatalog.entries.single { it.id == "esp32-devkitc-v4" }
        listOf("J2", "J3").forEach { header ->
            val pins = esp.pins.filter { it.header == header }
            assertEquals(19, pins.size)
            assertEquals((1..19).map(Int::toString), pins.map { it.number })
        }
    }

    @Test fun exactVariantPinAssignmentsArePreserved() {
        fun labels(id: String) = PinoutCatalog.entries.single { it.id == id }.pins.map { it.label }
        assertEquals(listOf("EMITTER", "BASE", "COLLECTOR"), labels("pn2222ata"))
        assertEquals(listOf("EMITTER", "BASE", "COLLECTOR"), labels("bc547b-nexperia"))
        assertEquals(listOf("GATE", "DRAIN", "SOURCE"), labels("irfz44npbf"))
        assertEquals(listOf("INPUT", "GND", "OUTPUT"), labels("l7805cv"))
        assertEquals(listOf("ADJUST", "OUTPUT", "INPUT"), labels("lm317t"))
    }

    @Test fun searchMatchesNamePartNumberAndCategory() {
        assertEquals("LM358", PinoutCatalog.search("lm358").single().name)
        assertEquals("LM317T", PinoutCatalog.search("LM317T").single().name)
        assertEquals(1, PinoutCatalog.search("7805", PinoutCategory.REGULATORS).size)
        assertTrue(PinoutCatalog.search("no such component").isEmpty())
        assertEquals(1, PinoutCatalog.search("pn2222ata", PinoutCategory.TRANSISTORS).size)
        assertFalse(PinoutCatalog.search("ESP32", PinoutCategory.ICS).any())
    }

    @Test fun allEntriesHaveDatasheetAndPowerGroundLabelsAreNotBlank() {
        PinoutCatalog.entries.forEach { entry ->
            assertTrue(entry.datasheetUrl.startsWith("https://"))
            assertTrue(entry.sourceName.isNotBlank())
            assertTrue(entry.pins.all { it.number.isNotBlank() && it.label.isNotBlank() && it.function.isNotBlank() })
        }
        assertTrue(PinoutCatalog.entries.flatMap { it.pins }.any { it.role == PinRole.POWER })
        assertTrue(PinoutCatalog.entries.flatMap { it.pins }.any { it.role == PinRole.GROUND })
    }
}
