package com.circuitcalc.electronics.domain

import kotlin.math.pow
import kotlin.math.round

data class ReferenceSource(val label: String, val url: String, val manufacturer: String? = null)

data class GateTruthTable(val inputs: List<String>)

data class ReferenceSection(
    val id: String,
    val title: String,
    val summary: String,
    val body: List<String> = emptyList(),
    val tableHeaders: List<String> = emptyList(),
    val tableRows: List<List<String>> = emptyList(),
    val source: ReferenceSource? = null,
    val symbolId: String? = null,
    val gate: GateTruthTable? = null
)

data class ReferenceCategory(
    val id: String,
    val title: String,
    val summary: String,
    val keywords: List<String>,
    val sections: List<ReferenceSection>
)

/** Offline electronics reference facts; links are optional online primary sources. */
object ReferenceCatalog {
    private val eSeriesCounts = listOf(6, 12, 24, 48, 96)
    private val eSeriesTolerance = mapOf(6 to "±20% (common association)", 12 to "±10%", 24 to "±5%", 48 to "±2%", 96 to "±1%")

    /** IEC 60063 normalized preferred values. E6-E24 use their canonical IEC values. */
    fun eSeries(count: Int): List<String> {
        require(count in eSeriesCounts) { "Supported E-series are E6, E12, E24, E48 and E96." }
        val canonical = when (count) {
            6 -> listOf("10", "15", "22", "33", "47", "68")
            12 -> listOf("10", "12", "15", "18", "22", "27", "33", "39", "47", "56", "68", "82")
            24 -> listOf("10", "11", "12", "13", "15", "16", "18", "20", "22", "24", "27", "30", "33", "36", "39", "43", "47", "51", "56", "62", "68", "75", "82", "91")
            48 -> listOf(100, 105, 110, 115, 121, 127, 133, 140, 147, 154, 162, 169, 178, 187, 196, 205, 215, 226, 237, 249, 261, 274, 287, 301, 316, 332, 348, 365, 383, 402, 422, 442, 464, 487, 511, 536, 562, 590, 619, 649, 681, 715, 750, 787, 825, 866, 909, 953)
                .map(::normalizeThreeDigitPreferredValue)
            96 -> listOf(100, 102, 105, 107, 110, 113, 115, 118, 121, 124, 127, 130, 133, 137, 140, 143, 147, 150, 154, 158, 162, 165, 169, 174, 178, 182, 187, 191, 196, 200, 205, 210, 215, 221, 226, 232, 237, 243, 249, 255, 261, 267, 274, 280, 287, 294, 301, 309, 316, 324, 332, 340, 348, 357, 365, 374, 383, 392, 402, 412, 422, 432, 442, 453, 464, 475, 487, 499, 511, 523, 536, 549, 562, 576, 590, 604, 619, 634, 649, 665, 681, 698, 715, 732, 750, 768, 787, 806, 825, 845, 866, 887, 909, 931, 953, 976)
                .map(::normalizeThreeDigitPreferredValue)
            else -> null
        }
        if (canonical != null) return canonical
        return checkNotNull(canonical)
    }

    private fun normalizeThreeDigitPreferredValue(value: Int): String =
        if (value % 10 == 0) (value / 10).toString() else "${value / 10}.${value % 10}"

    private fun seriesSection(count: Int) = ReferenceSection(
        id = "e$count", title = "E$count preferred values", summary = "${count} normalized values per decade · ${eSeriesTolerance.getValue(count)} is the common tolerance association.",
        body = listOf("Values shown are normalized to one decade (10–99.9). Multiply by 10ⁿ to scale to another decade, for example 47 × 10² = 4.7 kΩ.", "The tolerance association is customary, not a promise that every part in a series has that tolerance. Check the actual part data sheet."),
        tableHeaders = listOf("Normalized value"), tableRows = listOf(listOf(eSeries(count).joinToString("  ·  "))),
        source = ReferenceSource("Vishay E-Series values (IEC 60063)", "https://www.vishay.com/docs/28372/e-series.pdf", "Vishay")
    )

    val categories: List<ReferenceCategory> = listOf(
        ReferenceCategory("formulas", "Electronic Formulas", "Core circuit relationships, variable definitions and ideal-model assumptions.", listOf("ohm", "power", "rc", "rl", "reactance", "resonance", "time constant"), listOf(
            ReferenceSection("ohm-power", "Ohm’s law and power", "DC or resistive relationships", body = listOf("V = I × R", "P = V × I = I² × R = V² / R", "V is voltage in volts (V), I is current in amperes (A), R is resistance in ohms (Ω), and P is power in watts (W). The power forms are equivalent for an ideal resistor.")),
            ReferenceSection("time-constants", "RC and RL time constants", "First-order step response", body = listOf("RC: τ = R × C", "RL: τ = L / R", "τ is time in seconds (s), R is the equivalent resistance seen by the capacitor or inductor in ohms (Ω), C is capacitance in farads (F), and L is inductance in henries (H). For a first-order step, the response is about 63.2% of its final change after one time constant.")),
            ReferenceSection("reactance", "Capacitive and inductive reactance", "Ideal sinusoidal steady-state", body = listOf("Xₗ = 2πfL", "X꜀ = 1 / (2πfC)", "X is reactance in ohms (Ω), f is frequency in hertz (Hz), L is inductance in henries (H), and C is capacitance in farads (F). These magnitudes assume ideal components and sinusoidal steady-state.")),
            ReferenceSection("resonance", "Ideal LC resonant frequency", "Undamped ideal LC model", body = listOf("f₀ = 1 / (2π√(LC))", "f₀ is hertz (Hz), L is henries (H), and C is farads (F). Real losses, parasitics, loading and component tolerance shift or broaden resonance.")),
            ReferenceSection("formula-notes", "Variables, units and assumptions", "Read before applying a formula", body = listOf("Use SI units in equations unless you explicitly convert first: V, A, Ω, W, s, F, H and Hz.", "These are ideal circuit relationships. They do not include parasitic resistance/inductance, temperature drift, nonlinear behavior, component tolerance, source impedance or safety limits.", "For AC power in non-resistive loads, real power is not generally V × I using RMS magnitudes alone; power factor matters.") )
        )),
        ReferenceCategory("resistors", "Resistor Reference", "Preferred values, tolerances and common chip-resistor markings.", listOf("resistor", "e-series", "e6", "e12", "e24", "e48", "e96", "tolerance", "smd", "4r7"), resistorSections()),
        ReferenceCategory("capacitors", "Capacitor Reference", "Common capacitance codes, dielectric classes, polarity and ratings.", listOf("capacitor", "104", "473", "dielectric", "c0g", "np0", "x7r", "x5r", "polarity", "voltage rating"), listOf(
            ReferenceSection("cap-codes", "Common capacitance markings", "Three-digit pF code and decimal replacement", tableHeaders = listOf("Marking", "Example interpretation"), tableRows = listOf(listOf("104", "10 × 10⁴ pF = 100,000 pF = 100 nF"), listOf("473", "47 × 10³ pF = 47,000 pF = 47 nF"), listOf("4n7", "4.7 nF in RKM notation"), listOf("2µ2", "2.2 µF in RKM notation")), body = listOf("The three-digit pF convention is common on ceramic capacitors, but markings depend on package, maker and family. A bare code can be ambiguous; confirm it against the part data sheet."), source = ReferenceSource("KEMET C0G marking example", "https://content.kemet.com/datasheets/KEM_C1045_AXIAL_MOLDED_C0G.pdf", "KEMET")),
            ReferenceSection("dielectric", "Ceramic dielectric codes", "Typical class-level behavior; exact limits are part-specific", tableHeaders = listOf("Code", "General description"), tableRows = listOf(listOf("C0G / NP0", "Class I ceramic; very stable capacitance and low loss"), listOf("X7R", "Class II ceramic; higher capacitance density, capacitance varies with conditions"), listOf("X5R", "Class II ceramic; verify temperature range and DC-bias behavior in the data sheet"), listOf("Y5V / Z5U", "Class III general-purpose ceramics; comparatively large capacitance variation")), body = listOf("The letter/number codes are temperature-characteristic classifications. They do not by themselves specify every operating limit. Class II/III effective capacitance may vary with DC bias, temperature, frequency, aging and manufacturer."), source = ReferenceSource("KEMET ceramic capacitor guide", "https://www.kemet.com/en/us/capacitors/ceramic/ceramics-faq.html", "KEMET")),
            ReferenceSection("polarity-rating", "Polarity and voltage rating", "Identify capacitor type before fitting or replacing", body = listOf("Ceramic and film capacitors are generally non-polarized. Aluminum electrolytic and many tantalum capacitors are polarized; follow the polarity marking and the exact manufacturer data sheet.", "Never exceed the specified rated voltage. Account for ripple current, surge/transient voltage, temperature, frequency and DC-bias effects. A higher voltage rating is not automatically a drop-in replacement if size, ESR, ripple or stability requirements differ.", "There is no universal derating percentage for every capacitor technology and application. Follow the component data sheet and circuit requirements.") )
        )),
        ReferenceCategory("logic", "Logic Gates", "Boolean functions and complete two-state truth tables.", listOf("logic", "and", "or", "not", "nand", "nor", "xor", "xnor", "truth table"), listOf(
            gateSection("AND", "Y = A ∧ B", "Output is 1 only when both inputs are 1.") { a, b -> a && b },
            gateSection("OR", "Y = A ∨ B", "Output is 1 when either or both inputs are 1.") { a, b -> a || b },
            gateSection("NOT", "Y = ¬A", "Single-input inverter.", unary = true) { a, _ -> !a },
            gateSection("NAND", "Y = ¬(A ∧ B)", "Inverted AND; universal gate.") { a, b -> !(a && b) },
            gateSection("NOR", "Y = ¬(A ∨ B)", "Inverted OR; universal gate.") { a, b -> !(a || b) },
            gateSection("XOR", "Y = A ⊕ B", "Output is 1 when inputs differ.") { a, b -> a xor b },
            gateSection("XNOR", "Y = ¬(A ⊕ B)", "Output is 1 when inputs match.") { a, b -> a == b }
        )),
        ReferenceCategory("symbols", "Circuit Symbols", "Simplified vector symbols for common schematic components.", listOf("circuit symbol", "schematic", "iec", "ansi", "resistor", "capacitor", "transistor", "ground"), listOf(
            symbolSection("resistor", "Resistor", "IEC convention commonly uses a rectangle; ANSI/IEEE schematics commonly use a zigzag. Check the drawing’s stated convention.", "IEC rectangle / ANSI zigzag"),
            symbolSection("capacitor", "Capacitor", "Two plates represent capacitance; polarized types may add a polarity mark. The drawing does not imply a specific dielectric."),
            symbolSection("inductor", "Inductor", "Coil symbol for inductance. Winding details and core symbols depend on the drawing."),
            symbolSection("diode", "Diode", "Anode-to-cathode direction is indicated by the diode bar; check the symbol’s bar orientation."),
            symbolSection("led", "LED", "Diode symbol with outward arrows indicating emitted light."),
            symbolSection("transistor", "BJT transistor", "Illustrative NPN symbol with emitter arrow pointing out; PNP points in. Pin order depends on the exact device/package."),
            symbolSection("ground", "Ground", "Common reference/ground marking. Protective earth and chassis ground use distinct symbols in formal drawings."),
            symbolSection("battery", "Battery", "Unequal parallel plates indicate a cell; repeated pairs indicate multiple cells. Polarity follows the longer/shorter plate convention."),
            symbolSection("switch", "Open switch", "Open contacts indicate the shown state only; actual normally-open/closed behavior should be identified separately."),
            symbolSection("transformer", "Transformer", "Two coupled windings; core bars indicate a magnetic core. Dot/polarity conventions are omitted in this simplified symbol.")
        )),
        ReferenceCategory("si", "SI Units and Prefixes", "Common electronics prefixes from pico through giga.", listOf("si", "prefix", "pico", "nano", "micro", "milli", "kilo", "mega", "giga", "powers of ten"), listOf(
            ReferenceSection("prefix-table", "Prefix table", "Prefix symbols multiply the unit by an exact power of ten", tableHeaders = listOf("Name", "Symbol", "Factor", "Example"), tableRows = listOf(listOf("pico", "p", "10⁻¹²", "pF = 10⁻¹² F"), listOf("nano", "n", "10⁻⁹", "nF = 10⁻⁹ F"), listOf("micro", "µ", "10⁻⁶", "µA = 10⁻⁶ A"), listOf("milli", "m", "10⁻³", "mV = 10⁻³ V"), listOf("base unit", "—", "10⁰", "1 V = 1 V"), listOf("kilo", "k", "10³", "kΩ = 10³ Ω"), listOf("mega", "M", "10⁶", "MHz = 10⁶ Hz"), listOf("giga", "G", "10⁹", "GHz = 10⁹ Hz")), body = listOf("Prefix symbols are case-sensitive: m is milli (10⁻³), while M is mega (10⁶). SI prefixes are decimal powers of ten, not binary powers."), source = ReferenceSource("NIST SI prefixes", "https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-4-two-classes-si-units-and-si-prefixes", "NIST"))
        )),
        ReferenceCategory("packages", "Component Packages", "General package-family descriptions; no dimensions implied.", listOf("package", "dip", "soic", "sot-23", "to-92", "to-220", "qfp", "qfn"), listOf(
            packageSection("DIP", "Dual in-line package", "Two parallel rows of through-hole leads; commonly socketed or breadboard-friendly depending on pitch and body."),
            packageSection("SOIC", "Small-outline integrated circuit", "Molded surface-mount IC with leads extending from two opposite sides."),
            packageSection("SOT-23", "Small-outline transistor family", "Compact surface-mount outline used by transistors and small ICs. Pin count and lead arrangement vary by exact package variant."),
            packageSection("TO-92", "Small through-hole transistor package", "Small molded through-hole package commonly used for low-power semiconductors. Lead assignment is device-specific."),
            packageSection("TO-220", "Power semiconductor package family", "Through-hole power package with a tab or mounting area; pinout, tab connection and insulation requirements depend on the device."),
            packageSection("QFP", "Quad flat package", "Surface-mount IC package with leads on all four sides."),
            packageSection("QFN", "Quad flat no-lead package", "Leadless surface-mount package with perimeter lands and often an exposed thermal pad; pad connection is device-specific.")
        )),
        ReferenceCategory("datasheets", "Datasheet Library", "Verified official manufacturer documents. Links open online; this reference catalog remains offline.", listOf("datasheet", "data sheet", "manufacturer", "arduino", "esp32", "atmega328p", "ne555", "lm358", "lm324", "lm7805", "lm317", "74hc00", "74hc595", "uln2003a"), listOf(
            datasheet("Arduino UNO Rev3", "UNO R3 hardware reference", "https://docs.arduino.cc/hardware/uno-rev3", "Arduino"),
            datasheet("ESP32-DevKitC V4", "Board user guide", "https://docs.espressif.com/projects/esp-dev-kits/en/latest/esp32/esp32-devkitc/user_guide.html", "Espressif"),
            datasheet("ATmega328P", "Microchip device data sheet", "https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-7810-Automotive-Microcontrollers-ATmega328P_Datasheet.pdf", "Microchip"),
            datasheet("TI NE555", "xx555 Precision Timers", "https://www.ti.com/lit/ds/symlink/ne555.pdf", "Texas Instruments"),
            datasheet("TI LM358", "Industry-Standard Dual Operational Amplifiers", "https://www.ti.com/lit/ds/symlink/lm358.pdf", "Texas Instruments"),
            datasheet("TI LM324", "LMx24 Quadruple Operational Amplifiers", "https://www.ti.com/lit/ds/symlink/lm324.pdf", "Texas Instruments"),
            datasheet("TI LM7805 / LM340 family", "LM340 / LM7805 family fixed regulators", "https://www.ti.com/lit/ds/symlink/lm340.pdf", "Texas Instruments"),
            datasheet("TI LM317", "LM317 3-Pin Adjustable Regulator", "https://www.ti.com/lit/ds/symlink/lm317.pdf", "Texas Instruments"),
            datasheet("TI SN74HC00", "Quadruple 2-Input NAND Gates", "https://www.ti.com/lit/ds/symlink/sn74hc00.pdf", "Texas Instruments"),
            datasheet("TI SN74HC595", "8-Bit Shift Registers With 3-State Outputs", "https://www.ti.com/lit/ds/symlink/sn74hc595.pdf", "Texas Instruments"),
            datasheet("TI ULN2003A", "High-Voltage, High-Current Darlington Array", "https://www.ti.com/lit/ds/symlink/uln2003a.pdf", "Texas Instruments")
        ))
    )

    fun search(query: String): List<ReferenceCategory> {
        val term = query.trim()
        if (term.isEmpty()) return categories
        return categories.filter { category ->
            sequenceOf(category.id, category.title, category.summary).plus(category.keywords.asSequence()).plus(
                category.sections.asSequence().flatMap { section ->
                    sequenceOf(section.title, section.summary).plus(section.body.asSequence())
                        .plus(section.tableHeaders.asSequence()).plus(section.tableRows.flatten().asSequence())
                        .plus(sequenceOf(section.source?.label.orEmpty(), section.source?.manufacturer.orEmpty()))
                }
            ).any { it.contains(term, ignoreCase = true) }
        }
    }

    fun category(id: String): ReferenceCategory? = categories.firstOrNull { it.id == id }

    private fun resistorSections(): List<ReferenceSection> = buildList {
        eSeriesCounts.forEach { add(seriesSection(it)) }
        val toleranceRows = eSeriesCounts.map { listOf("E$it", eSeriesTolerance.getValue(it)) } + listOf(
            listOf("Gold band", "±5%"), listOf("Silver band", "±10%"),
            listOf("Brown band", "±1%"), listOf("Red band", "±2%")
        )
        add(ReferenceSection(
            id = "tolerance", title = "Tolerance guide", summary = "Common E-series associations and resistor color-band tolerances",
            tableHeaders = listOf("Series", "Common tolerance"), tableRows = toleranceRows,
            body = listOf("Color-band and E-series associations are common references. Verify the manufacturer’s marking scheme and part data sheet; special tolerances exist.")
        ))
        val smdRows = listOf(
            listOf("472", "47 × 10² Ω = 4.7 kΩ (3-digit)"),
            listOf("1001", "100 × 10¹ Ω = 1 kΩ (4-digit)"),
            listOf("4R7", "4.7 Ω; R marks the decimal point"),
            listOf("R47", "0.47 Ω"),
            listOf("01C", "May be an EIA-96 code; decode only with the manufacturer’s matching table")
        )
        add(ReferenceSection(
            id = "smd-codes", title = "SMD marking examples", summary = "Common numeric and RKM-style forms; not universal",
            tableHeaders = listOf("Marking", "Example interpretation"), tableRows = smdRows,
            body = listOf("For a common 3-digit code, the first two digits are significant and the last digit is the power-of-ten multiplier. A 4-digit code commonly uses three significant digits and a multiplier.", "SMD codes can be reused across manufacturers and series. EIA-96 uses a separate value table and multiplier letter; markings are not globally unique. Do not identify a part from a short top mark alone.")
        ))
    }

    private fun gateSection(name: String, equation: String, summary: String, unary: Boolean = false, output: (Boolean, Boolean) -> Boolean) =
        ReferenceSection("gate-${name.lowercase()}", "$name gate", summary, body = listOf(equation), tableHeaders = if (unary) listOf("A", "Y") else listOf("A", "B", "Y"), tableRows = if (unary) (listOf(false, true).map { a -> listOf(bit(a), bit(output(a, false))) }) else listOf(false to false, false to true, true to false, true to true).map { (a,b) -> listOf(bit(a), bit(b), bit(output(a,b))) }, gate = GateTruthTable(if (unary) listOf("A") else listOf("A", "B")))

    private fun symbolSection(symbolId: String, title: String, summary: String, convention: String? = null) =
        ReferenceSection("symbol-$symbolId", title, summary, body = listOfNotNull(convention, "Simplified educational vector. Actual drawing standards, labels and device pin functions may vary by project or manufacturer."), symbolId = symbolId)

    private fun packageSection(name: String, title: String, summary: String) =
        ReferenceSection("pkg-${name.lowercase()}", "$name · $title", summary, body = listOf("This is a package-family description, not a mechanical drawing. Check the exact manufacturer package code and land pattern before layout or assembly."), source = ReferenceSource("TI package terminology", "https://www.ti.com/en-us/design-development/packaging/packaging-terminology", "Texas Instruments"))

    private fun datasheet(name: String, title: String, url: String, manufacturer: String) =
        ReferenceSection("ds-${name.lowercase().replace(' ', '-')}", name, title, body = listOf("Online official manufacturer document. Internet access is required to open it; the link target is not bundled for offline use."), source = ReferenceSource("Open $title", url, manufacturer))

    private fun bit(value: Boolean) = if (value) "1" else "0"
}
