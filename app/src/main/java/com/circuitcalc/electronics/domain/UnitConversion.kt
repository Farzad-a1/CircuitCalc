package com.circuitcalc.electronics.domain

data class UnitChoice(val label: String, val factorToEngineUnit: Double)

/** Prefix choices convert entered values to the SI/base unit expected by CalculatorEngine. */
object UnitConversion {
    fun choices(field: Field): List<UnitChoice> = when (field.unit) {
        "V" -> listOf(UnitChoice("V",1.0),UnitChoice("mV",1e-3),UnitChoice("kV",1e3))
        "A" -> listOf(UnitChoice("A",1.0),UnitChoice("mA",1e-3),UnitChoice("µA",1e-6))
        "mA" -> listOf(UnitChoice("mA",1.0),UnitChoice("A",1e3),UnitChoice("µA",1e-3))
        "Ω" -> listOf(UnitChoice("Ω",1.0),UnitChoice("kΩ",1e3),UnitChoice("MΩ",1e6))
        "F" -> listOf(UnitChoice("F",1.0),UnitChoice("mF",1e-3),UnitChoice("µF",1e-6),UnitChoice("nF",1e-9),UnitChoice("pF",1e-12))
        "H" -> listOf(UnitChoice("H",1.0),UnitChoice("mH",1e-3),UnitChoice("µH",1e-6),UnitChoice("nH",1e-9))
        "Hz" -> listOf(UnitChoice("Hz",1.0),UnitChoice("kHz",1e3),UnitChoice("MHz",1e6))
        "s" -> listOf(UnitChoice("s",1.0),UnitChoice("ms",1e-3),UnitChoice("µs",1e-6),UnitChoice("min",60.0))
        "W" -> listOf(UnitChoice("W",1.0),UnitChoice("mW",1e-3),UnitChoice("kW",1e3))
        "C" -> listOf(UnitChoice("C",1.0),UnitChoice("mC",1e-3),UnitChoice("µC",1e-6),UnitChoice("nC",1e-9))
        "mAh" -> listOf(UnitChoice("mAh",1.0),UnitChoice("Ah",1e3))
        "m" -> listOf(UnitChoice("m",1.0),UnitChoice("cm",1e-2),UnitChoice("mm",1e-3))
        "mm²" -> listOf(UnitChoice("mm²",1.0),UnitChoice("cm²",100.0),UnitChoice("m²",1e6))
        "m/s" -> listOf(UnitChoice("m/s",1.0),UnitChoice("km/s",1e3))
        else -> listOf(UnitChoice(field.unit,1.0))
    }

    fun toEngineValue(value: Double, field: Field, selectedUnit: String): Double {
        val choice=choices(field).firstOrNull{it.label==selectedUnit} ?: throw IllegalArgumentException("Unsupported unit $selectedUnit for ${field.name}.")
        return value*choice.factorToEngineUnit
    }
}
