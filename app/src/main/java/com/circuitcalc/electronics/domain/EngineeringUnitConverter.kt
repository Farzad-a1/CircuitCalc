package com.circuitcalc.electronics.domain

import kotlin.math.abs

enum class ConversionKind { LINEAR, TEMPERATURE, AWG }

data class EngineeringUnit(val symbol: String, val factorToBase: Double = 1.0)

data class EngineeringCategory(
    val id: String,
    val name: String,
    val description: String,
    val units: List<EngineeringUnit>,
    val kind: ConversionKind = ConversionKind.LINEAR,
    val searchTerms: List<String> = emptyList(),
    val defaultFrom: String = units.first().symbol,
    val defaultTo: String = units.getOrElse(1) { units.first() }.symbol
)

/** Offline SI conversions and nominal American Wire Gauge dimensions. */
object EngineeringUnitConverter {
    val categories: List<EngineeringCategory> = listOf(
        EngineeringCategory("voltage", "Voltage", "Volts and submultiples", listOf(EngineeringUnit("V"), EngineeringUnit("mV", 1e-3), EngineeringUnit("µV", 1e-6), EngineeringUnit("kV", 1e3)), searchTerms = listOf("potential")),
        EngineeringCategory("current", "Current", "Amperes and submultiples", listOf(EngineeringUnit("A"), EngineeringUnit("mA", 1e-3), EngineeringUnit("µA", 1e-6), EngineeringUnit("kA", 1e3)), searchTerms = listOf("amperage")),
        EngineeringCategory("resistance", "Resistance", "Ohms and submultiples", listOf(EngineeringUnit("Ω"), EngineeringUnit("mΩ", 1e-3), EngineeringUnit("kΩ", 1e3), EngineeringUnit("MΩ", 1e6)), searchTerms = listOf("ohms")),
        EngineeringCategory("capacitance", "Capacitance", "Farads and submultiples", listOf(EngineeringUnit("F"), EngineeringUnit("mF", 1e-3), EngineeringUnit("µF", 1e-6), EngineeringUnit("nF", 1e-9), EngineeringUnit("pF", 1e-12)), searchTerms = listOf("capacitor")),
        EngineeringCategory("inductance", "Inductance", "Henries and submultiples", listOf(EngineeringUnit("H"), EngineeringUnit("mH", 1e-3), EngineeringUnit("µH", 1e-6), EngineeringUnit("nH", 1e-9)), searchTerms = listOf("inductor")),
        EngineeringCategory("frequency", "Frequency", "Hertz and multiples", listOf(EngineeringUnit("Hz"), EngineeringUnit("kHz", 1e3), EngineeringUnit("MHz", 1e6), EngineeringUnit("GHz", 1e9)), searchTerms = listOf("cycles")),
        EngineeringCategory("power", "Power", "Watts and multiples", listOf(EngineeringUnit("W"), EngineeringUnit("mW", 1e-3), EngineeringUnit("kW", 1e3), EngineeringUnit("MW", 1e6)), searchTerms = listOf("wattage")),
        EngineeringCategory("energy", "Energy", "Joules and watt-hours", listOf(EngineeringUnit("J"), EngineeringUnit("mJ", 1e-3), EngineeringUnit("Wh", 3600.0), EngineeringUnit("kWh", 3.6e6)), searchTerms = listOf("joules", "watt hour")),
        EngineeringCategory("time", "Time", "Seconds, minutes and hours", listOf(EngineeringUnit("s"), EngineeringUnit("ms", 1e-3), EngineeringUnit("µs", 1e-6), EngineeringUnit("ns", 1e-9), EngineeringUnit("min", 60.0), EngineeringUnit("h", 3600.0)), searchTerms = listOf("duration")),
        EngineeringCategory("length", "Length", "Metric and imperial lengths", listOf(EngineeringUnit("m"), EngineeringUnit("mm", 1e-3), EngineeringUnit("µm", 1e-6), EngineeringUnit("cm", 1e-2), EngineeringUnit("km", 1e3), EngineeringUnit("in", 0.0254), EngineeringUnit("ft", 0.3048)), searchTerms = listOf("distance")),
        EngineeringCategory("temperature", "Temperature", "Celsius, Fahrenheit and kelvin", listOf(EngineeringUnit("°C"), EngineeringUnit("°F"), EngineeringUnit("K")), ConversionKind.TEMPERATURE, listOf("celsius", "fahrenheit", "kelvin"), "°C", "°F"),
        EngineeringCategory("wire-gauge", "Wire gauge", "AWG and standard nominal conductor diameter", listOf(EngineeringUnit("AWG"), EngineeringUnit("mm"), EngineeringUnit("in")), ConversionKind.AWG, listOf("american wire gauge", "diameter"), "AWG", "mm")
    )

    // Nominal bare solid-wire diameters transcribed from U.S. Bureau of Standards Circular 31, Table VII.
    // Gauge values -3..0 correspond to 0000 (4/0), 000 (3/0), 00 (2/0), and 0 (1/0).
    private val awgDiameterMm: Map<Int, Double> = listOf(
        11.68, 10.40, 9.266, 8.252, 7.348, 6.544, 5.827, 5.189, 4.621, 4.115,
        3.665, 3.264, 2.906, 2.588, 2.305, 2.053, 1.828, 1.628, 1.450, 1.291,
        1.150, 1.024, 0.9116, 0.8118, 0.7230, 0.6438, 0.5733, 0.5106, 0.4547,
        0.4049, 0.3606, 0.3211, 0.2859, 0.2546, 0.2268, 0.2019, 0.1798, 0.1601,
        0.1426, 0.1270, 0.1131, 0.1007, 0.08969, 0.07987
    ).mapIndexed { index, diameter -> index - 3 to diameter }.toMap()

    fun category(id: String): EngineeringCategory = categories.firstOrNull { it.id == id }
        ?: throw IllegalArgumentException("Unknown conversion category.")

    fun searchCategories(query: String): List<EngineeringCategory> {
        val term = query.trim()
        return categories.filter { item ->
            term.isEmpty() || item.name.contains(term, true) || item.description.contains(term, true) || item.units.any { it.symbol.contains(term, true) } || item.searchTerms.any { it.contains(term, true) }
        }
    }

    /** Converts [value] directly between two units in a category. All values must be finite. */
    fun convert(value: Double, categoryId: String, fromUnit: String, toUnit: String): Double {
        require(value.isFinite()) { "Enter a finite numeric value." }
        val item = category(categoryId)
        require(item.units.any { it.symbol == fromUnit } && item.units.any { it.symbol == toUnit }) { "Select units from this category." }
        val result = when (item.kind) {
            ConversionKind.LINEAR -> {
                val from = item.units.first { it.symbol == fromUnit }
                val to = item.units.first { it.symbol == toUnit }
                (value * from.factorToBase) / to.factorToBase
            }
            ConversionKind.TEMPERATURE -> convertTemperature(value, fromUnit, toUnit)
            ConversionKind.AWG -> convertAwg(value, fromUnit, toUnit)
        }
        require(result.isFinite()) { "The converted value is outside the supported numeric range." }
        return result
    }

    fun parseInput(text: String, categoryId: String, unit: String): Double? {
        val item = category(categoryId)
        if (item.kind == ConversionKind.AWG && unit == "AWG") {
            val trimmed = text.trim().uppercase().removeSuffix("AWG").trim()
            val normalized = when (trimmed) { "0000", "4/0" -> -3; "000", "3/0" -> -2; "00", "2/0" -> -1; "0", "1/0" -> 0; else -> trimmed.toIntOrNull() }
            return normalized?.toDouble()
        }
        return text.trim().toDoubleOrNull()
    }

    fun formatValue(value: Double, categoryId: String, unit: String): String {
        val item = category(categoryId)
        if (item.kind == ConversionKind.AWG && unit == "AWG" && value.isFinite() && value == value.toInt().toDouble()) {
            return awgLabel(value.toInt())
        }
        if (!value.isFinite()) return "Invalid"
        val magnitude = abs(value)
        return if (magnitude != 0.0 && (magnitude >= 1e10 || magnitude < 1e-6)) {
            String.format(java.util.Locale.US, "%.8g", value)
        } else {
            java.text.DecimalFormat("0.##########", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(value)
        }
    }

    fun awgLabel(gauge: Int): String = when (gauge) {
        -3 -> "4/0"
        -2 -> "3/0"
        -1 -> "2/0"
        0 -> "1/0"
        in 1..40 -> gauge.toString()
        else -> throw IllegalArgumentException("AWG is supported from 4/0 through 40.")
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        val kelvin = when (from) {
            "°C" -> value + 273.15
            "°F" -> (value + 459.67) * (5.0 / 9.0)
            "K" -> value
            else -> error("Unsupported temperature unit.")
        }
        require(kelvin >= 0.0) { "Temperature cannot be below absolute zero." }
        return when (to) {
            "°C" -> kelvin - 273.15
            "°F" -> kelvin * (9.0 / 5.0) - 459.67
            "K" -> kelvin
            else -> error("Unsupported temperature unit.")
        }
    }

    private fun convertAwg(value: Double, from: String, to: String): Double {
        if (from == to) {
            if (from == "AWG") validateGauge(value)
            return value
        }
        if (from == "AWG") {
            val gauge = validateGauge(value)
            val diameterMm = awgDiameterMm[gauge] ?: throw IllegalArgumentException("AWG is supported from 4/0 through 40.")
            return when (to) { "mm" -> diameterMm; "in" -> diameterMm / 25.4; else -> error("Unsupported wire-gauge unit.") }
        }
        require(value.isFinite() && value > 0.0) { "Enter a positive wire diameter." }
        if (to == "AWG") {
            val diameterMm = when (from) { "mm" -> value; "in" -> value * 25.4; else -> error("Unsupported wire-gauge unit.") }
            require(diameterMm.isFinite() && diameterMm > 0.0) { "Enter a positive wire diameter." }
            val table = awgDiameterMm
            require(diameterMm <= table.getValue(-3) && diameterMm >= table.getValue(40)) { "Diameter must fall within standard AWG 4/0–40 nominal sizes." }
            return table.minBy { (_, nominal) -> abs(nominal - diameterMm) }.key.toDouble()
        }
        val mm = when (from) { "mm" -> value; "in" -> value * 25.4; else -> error("Unsupported wire-gauge unit.") }
        return when (to) { "mm" -> mm; "in" -> mm / 25.4; else -> error("Unsupported wire-gauge unit.") }
    }

    private fun validateGauge(value: Double): Int {
        require(value.isFinite() && value == value.toInt().toDouble()) { "AWG must be a whole standard size from 4/0 through 40." }
        val gauge = value.toInt()
        require(gauge in -3..40) { "AWG is supported from 4/0 through 40." }
        return gauge
    }
}
