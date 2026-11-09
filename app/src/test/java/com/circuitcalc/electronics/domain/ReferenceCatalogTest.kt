package com.circuitcalc.electronics.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceCatalogTest {
    @Test fun allEightReferenceCategoriesAreSubstantiveAndHaveUniqueIds() {
        assertEquals(8, ReferenceCatalog.categories.size)
        assertEquals(ReferenceCatalog.categories.size, ReferenceCatalog.categories.map { it.id }.distinct().size)
        ReferenceCatalog.categories.forEach { category ->
            assertTrue(category.title.isNotBlank())
            assertTrue(category.summary.length > 20)
            assertFalse(category.sections.isEmpty())
            assertEquals("${category.id} section IDs", category.sections.size, category.sections.map { it.id }.distinct().size)
            category.sections.forEach { section ->
                assertTrue("${category.id}/${section.id}", section.title.isNotBlank() && section.summary.isNotBlank())
                section.source?.let { source ->
                    assertTrue(source.url.startsWith("https://"))
                    assertTrue(source.label.isNotBlank())
                }
            }
        }
    }

    @Test fun eSeriesHaveExpectedCountsValuesAndNoDuplicates() {
        val expectedCounts = mapOf(6 to 6, 12 to 12, 24 to 24, 48 to 48, 96 to 96)
        expectedCounts.forEach { (series, count) ->
            val values = ReferenceCatalog.eSeries(series)
            assertEquals("E$series", count, values.size)
            assertEquals("E$series has unique values", values.size, values.distinct().size)
            assertEquals("10", values.first())
            assertTrue(values.last().toDouble() < 100.0)
        }
        assertEquals(listOf("10", "15", "22", "33", "47", "68"), ReferenceCatalog.eSeries(6))
        assertEquals(listOf("10", "12", "15", "18", "22", "27", "33", "39", "47", "56", "68", "82"), ReferenceCatalog.eSeries(12))
        assertEquals(listOf("10", "10.2", "10.5", "10.7", "11", "11.3"), ReferenceCatalog.eSeries(96).take(6))
    }

    @Test fun formulasIncludeRequiredRelationshipsAndAssumptions() {
        val formulas = ReferenceCatalog.category("formulas")!!
        val content = formulas.sections.flatMap { it.body }.joinToString(" ")
        listOf("V = I × R", "P = V × I", "τ = R × C", "τ = L / R", "Xₗ = 2πfL", "X꜀ = 1 / (2πfC)", "f₀ = 1 / (2π√(LC))").forEach { assertTrue("Missing $it", content.contains(it)) }
        assertTrue(content.contains("sinusoidal steady-state"))
        assertTrue(content.contains("parasitics"))
    }

    @Test fun capacitorReferenceHasVerifiedCodeAndSafetyGuidance() {
        val capacitor = ReferenceCatalog.category("capacitors")!!
        val codeTable = capacitor.sections.first { it.id == "cap-codes" }.tableRows.flatten().joinToString(" ")
        assertTrue(codeTable.contains("100 nF"))
        assertTrue(codeTable.contains("47 nF"))
        val guidance = capacitor.sections.first { it.id == "polarity-rating" }.body.joinToString(" ")
        assertTrue(guidance.contains("polarized"))
        assertTrue(guidance.contains("rated voltage"))
        assertTrue(guidance.contains("no universal derating"))
    }

    @Test fun allLogicGateTablesMatchBooleanDefinitions() {
        val gates = ReferenceCatalog.category("logic")!!.sections
        assertEquals(setOf("AND", "OR", "NOT", "NAND", "NOR", "XOR", "XNOR"), gates.map { it.title.removeSuffix(" gate") }.toSet())
        val twoInput = listOf("AND", "OR", "NAND", "NOR", "XOR", "XNOR")
        val expected = mapOf(
            "AND" to listOf("0", "0", "0", "1"), "OR" to listOf("0", "1", "1", "1"),
            "NAND" to listOf("1", "1", "1", "0"), "NOR" to listOf("1", "0", "0", "0"),
            "XOR" to listOf("0", "1", "1", "0"), "XNOR" to listOf("1", "0", "0", "1")
        )
        twoInput.forEach { name ->
            val rows = gates.first { it.title == "$name gate" }.tableRows
            assertEquals(4, rows.size)
            assertEquals(expected.getValue(name), rows.map { it.last() })
        }
        assertEquals(listOf(listOf("0", "1"), listOf("1", "0")), gates.first { it.title == "NOT gate" }.tableRows)
    }

    @Test fun symbolsPrefixesPackagesAndOfficialLinksAreComplete() {
        val symbolIds = ReferenceCatalog.category("symbols")!!.sections.mapNotNull { it.symbolId }.toSet()
        assertEquals(setOf("resistor", "capacitor", "inductor", "diode", "led", "transistor", "ground", "battery", "switch", "transformer"), symbolIds)
        val prefixes = ReferenceCatalog.category("si")!!.sections.single().tableRows.map { it[1] }.toSet()
        assertTrue(prefixes.containsAll(setOf("p", "n", "µ", "m", "k", "M", "G")))
        val packageNames = ReferenceCatalog.category("packages")!!.sections.map { it.title.substringBefore(" ·") }.toSet()
        assertTrue(packageNames.containsAll(setOf("DIP", "SOIC", "SOT-23", "TO-92", "TO-220", "QFP", "QFN")))
        val datasheets = ReferenceCatalog.category("datasheets")!!.sections
        assertTrue(datasheets.size >= 10)
        assertTrue(datasheets.all { it.source?.url?.startsWith("https://") == true })
        assertNotNull(datasheets.first { it.title.contains("7805") }.source)
    }

    @Test fun searchFindsContentAliasesAndDatasheetsButOmitsNonMatches() {
        assertEquals("Resistor Reference", ReferenceCatalog.search("4R7").single().title)
        assertEquals("Capacitor Reference", ReferenceCatalog.search("C0G").single().title)
        assertEquals("Logic Gates", ReferenceCatalog.search("XNOR").single().title)
        assertEquals("Component Packages", ReferenceCatalog.search("SOT-23").single().title)
        assertEquals("Datasheet Library", ReferenceCatalog.search("ULN2003A").single().title)
        assertTrue(ReferenceCatalog.search("not a reference term").isEmpty())
        assertEquals(8, ReferenceCatalog.search("").size)
    }
}
