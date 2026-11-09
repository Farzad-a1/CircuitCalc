package com.circuitcalc.electronics.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewCalculatorsTest {
    private fun calc(name: String, values: Map<String, Double> = emptyMap(), text: Map<String, String> = emptyMap()) =
        CalculatorEngine.calculate(name, values, text)

    private inline fun assertInvalid(block: () -> Any) {
        try { block(); throw AssertionError("Expected invalid input to be rejected") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun catalogContainsFortyDistinctToolsAndTheOriginalThirty() {
        assertEquals(40, CalculatorEngine.calculators.size)
        assertEquals(40, CalculatorEngine.calculators.map { it.name }.distinct().size)
        val additions = listOf("SMD Resistor Decoder", "Resistor Color Code (5/6-band)", "Nearest E-Series Resistor", "Series Capacitors", "555 Monostable Calculator", "Inverting Op-Amp Calculator", "AC RLC Impedance (Series)", "Three-Phase Power (Balanced)", "Battery Pack Calculator", "ADC Resolution Calculator")
        assertTrue(CalculatorEngine.calculators.map { it.name }.containsAll(additions))
    }

    @Test fun smdDecoderSupportsThreeFourDigitAndEia96Markings() {
        assertTrue(calc("SMD Resistor Decoder", text = mapOf("Marking" to "103")).contains("10 kΩ"))
        assertTrue(calc("SMD Resistor Decoder", text = mapOf("Marking" to "4701")).contains("4.7 kΩ"))
        assertTrue(calc("SMD Resistor Decoder", text = mapOf("Marking" to "01C")).contains("10 kΩ"))
        assertTrue(calc("SMD Resistor Decoder", text = mapOf("Marking" to "000")).contains("0 Ω"))
        assertInvalid { calc("SMD Resistor Decoder", text = mapOf("Marking" to "4R7")) }
        assertInvalid { calc("SMD Resistor Decoder", text = mapOf("Marking" to "97A")) }
    }

    @Test fun fiveAndSixBandResistorsUseToleranceAndTemperatureBand() {
        val base = mapOf("Band 1" to "Brown", "Band 2" to "Black", "Band 3" to "Black", "Band 4 multiplier" to "Red", "Band 5 tolerance" to "Brown")
        assertTrue(calc("Resistor Color Code (5/6-band)", text = base + ("Band count" to "5")).contains("10 kΩ ±1%"))
        assertTrue(calc("Resistor Color Code (5/6-band)", text = base + mapOf("Band count" to "5", "Band 5 tolerance" to "Orange")).contains("±0.05%"))
        assertTrue(calc("Resistor Color Code (5/6-band)", text = base + mapOf("Band count" to "5", "Band 5 tolerance" to "Yellow")).contains("±0.02%"))
        assertTrue(calc("Resistor Color Code (5/6-band)", text = base + mapOf("Band count" to "5", "Band 5 tolerance" to "Gray")).contains("±0.01%"))
        val six = calc("Resistor Color Code (5/6-band)", text = base + mapOf("Band count" to "6", "Band 6 TCR (ppm/K)" to "Blue"))
        assertTrue(six.contains("Temperature coefficient ±10 ppm/K"))
        assertTrue(calc("Resistor Color Code (5/6-band)", text = base + mapOf("Band count" to "6", "Band 6 TCR (ppm/K)" to "Green")).contains("±20 ppm/K"))
        assertInvalid { calc("Resistor Color Code (5/6-band)", text = base + mapOf("Band count" to "6", "Band 1" to "Black", "Band 6 TCR (ppm/K)" to "Blue")) }
        assertInvalid { calc("Resistor Color Code (5/6-band)", text = base + mapOf("Band count" to "6", "Band 6 TCR (ppm/K)" to "Gold")) }
    }

    @Test fun nearestEValuesCoverE6ThroughE192AndValidateBoundaries() {
        val result = calc("Nearest E-Series Resistor", mapOf("Desired resistance" to 4600.0), mapOf("E-series" to "E24"))
        assertTrue(result.contains("Nearest 4.7 kΩ")); assertTrue(result.contains("Lower 4.3 kΩ")); assertTrue(result.contains("Higher 4.7 kΩ")); assertTrue(result.contains("2.1739%"))
        assertTrue(calc("Nearest E-Series Resistor", mapOf("Desired resistance" to 101.0), mapOf("E-series" to "E192")).contains("Nearest 101 Ω"))
        listOf("E6", "E12", "E24", "E48", "E96", "E192").forEach { series ->
            assertTrue(calc("Nearest E-Series Resistor", mapOf("Desired resistance" to 1000.0), mapOf("E-series" to series)).contains("Nearest 1 kΩ"))
        }
        assertInvalid { calc("Nearest E-Series Resistor", mapOf("Desired resistance" to 0.0), mapOf("E-series" to "E96")) }
        assertInvalid { calc("Nearest E-Series Resistor", mapOf("Desired resistance" to 1.0), mapOf("E-series" to "E1920")) }
    }

    @Test fun seriesCapacitanceRequiresAtLeastTwoPositiveValuesAndConvertsUnits() {
        val total = calc("Series Capacitors", mapOf("C1" to 10e-6, "C2" to 20e-6))
        assertTrue(total.contains("6.6667e-06 F"))
        assertTrue(calc("Series Capacitors", mapOf("C1" to 10e-6, "C2" to 20e-6, "C3 (optional)" to 0.0, "C4 (optional)" to 0.0)).contains("6.6667e-06 F"))
        assertEquals(10e-6, UnitConversion.toEngineValue(10.0, Field("C", "F"), "µF"), 1e-15)
        assertEquals(1e-3, UnitConversion.toEngineValue(1.0, Field("C", "F"), "mF"), 0.0)
        assertInvalid { calc("Series Capacitors", mapOf("C1" to 1e-6)) }
        assertInvalid { calc("Series Capacitors", mapOf("C1" to 1e-6, "C2" to 0.0)) }
    }

    @Test fun monostableAndInvertingAmplifierCases() {
        assertTrue(calc("555 Monostable Calculator", mapOf("Timing resistance" to 100e3, "Timing capacitance" to 10e-6)).contains("1.1 s"))
        assertEquals(10e-6, UnitConversion.toEngineValue(10.0, Field("C", "F"), "µF"), 1e-15)
        assertEquals(100e3, UnitConversion.toEngineValue(100.0, Field("R", "Ω"), "kΩ"), 0.0)
        assertInvalid { calc("555 Monostable Calculator", mapOf("Timing resistance" to 0.0, "Timing capacitance" to 1e-6)) }
        val amp = calc("Inverting Op-Amp Calculator", mapOf("Feedback resistance Rf" to 100e3, "Input resistance Rin" to 10e3, "Input voltage" to .2))
        assertTrue(amp.contains("Ideal gain -10 V/V")); assertTrue(amp.contains("Ideal output -2 V")); assertTrue(amp.contains("supply rails"))
        assertTrue(calc("Inverting Op-Amp Calculator", mapOf("Feedback resistance Rf" to 10e3, "Input resistance Rin" to 10e3, "Input voltage" to 0.0)).contains("Ideal output 0 V"))
        assertInvalid { calc("Inverting Op-Amp Calculator", mapOf("Feedback resistance Rf" to 10e3, "Input resistance Rin" to 0.0, "Input voltage" to 1.0)) }
    }

    @Test fun seriesRlcCoversInductiveCapacitiveResonantAndDcCases() {
        val nearResonance = calc("AC RLC Impedance (Series)", mapOf("Resistance" to 10.0, "Inductance" to .1, "Capacitance" to 100e-6, "Frequency" to 50.0))
        assertTrue(nearResonance.contains("Inductive reactance 31.416 Ω")); assertTrue(nearResonance.contains("Capacitive reactance 31.831 Ω")); assertTrue(nearResonance.contains("Impedance magnitude 10.009 Ω"))
        assertEquals(.1, UnitConversion.toEngineValue(100.0, Field("L", "H"), "mH"), 0.0)
        assertEquals(50_000.0, UnitConversion.toEngineValue(50.0, Field("f", "Hz"), "kHz"), 0.0)
        val exactResonanceFrequency = 1.0 / (2.0 * Math.PI * kotlin.math.sqrt(.1 * 100e-6))
        val resonance = calc("AC RLC Impedance (Series)", mapOf("Resistance" to 0.0, "Inductance" to .1, "Capacitance" to 100e-6, "Frequency" to exactResonanceFrequency))
        assertTrue(resonance.contains("undefined at ideal zero-impedance resonance"))
        assertTrue(calc("AC RLC Impedance (Series)", mapOf("Resistance" to 10.0, "Inductance" to .1, "Capacitance" to 100e-6, "Frequency" to 0.0)).contains("infinite (ideal series capacitor is open at DC)"))
        assertInvalid { calc("AC RLC Impedance (Series)", mapOf("Resistance" to -1.0, "Inductance" to .1, "Capacitance" to 1e-6, "Frequency" to 50.0)) }
    }

    @Test fun balancedThreePhaseReportsPowerTriangleForLeadingAndLaggingLoads() {
        val lagging = calc("Three-Phase Power (Balanced)", mapOf("Line voltage" to 400.0, "Line current" to 10.0, "Power factor (0–1)" to .8), mapOf("Load characteristic" to "Lagging"))
        assertTrue(lagging, lagging.contains("Real power 5542.6 W") && lagging.contains("Reactive power 4156.9 var (lagging)") && lagging.contains("Apparent power 6928.2 VA"))
        val leading = calc("Three-Phase Power (Balanced)", mapOf("Line voltage" to 400.0, "Line current" to 10.0, "Power factor (0–1)" to 1.0), mapOf("Load characteristic" to "Leading"))
        assertTrue(leading.contains("Reactive power 0 var (leading)"))
        assertInvalid { calc("Three-Phase Power (Balanced)", mapOf("Line voltage" to 400.0, "Line current" to 10.0, "Power factor (0–1)" to 1.01), mapOf("Load characteristic" to "Lagging")) }
    }

    @Test fun batteryPackCalculatesSeriesParallelVoltageCapacityAndEnergy() {
        val values = mapOf("Nominal cell voltage" to 3.7, "Cell capacity" to 2500.0, "Series cells (Ns)" to 3.0, "Parallel cells (Np)" to 2.0)
        val result = calc("Battery Pack Calculator", values)
        assertTrue(result.contains("Nominal pack voltage 11.1 V")); assertTrue(result.contains("Pack capacity 5000 mAh")); assertTrue(result.contains("Nominal energy 55.5 Wh")); assertTrue(result.contains("Total cells 6")); assertTrue(result.contains("protection circuitry"))
        assertEquals(2500.0, UnitConversion.toEngineValue(2.5, Field("Capacity", "mAh"), "Ah"), 0.0)
        assertInvalid { calc("Battery Pack Calculator", values + ("Series cells (Ns)" to 1.5)) }
    }

    @Test fun adcResolutionUses2ToNLevelsAndHalfLsbError() {
        val adc = calc("ADC Resolution Calculator", mapOf("ADC bit depth" to 12.0, "Reference voltage" to 3.3))
        assertTrue(adc.contains("Quantization levels 4096")); assertTrue(adc.contains("Maximum digital code 4095")); assertTrue(adc.contains("8.0566e-04 V")); assertTrue(adc.contains("4.0283e-04 V"))
        assertTrue(calc("ADC Resolution Calculator", mapOf("ADC bit depth" to 1.0, "Reference voltage" to 2.0)).contains("Maximum digital code 1"))
        assertFalse(calc("ADC Resolution Calculator", mapOf("ADC bit depth" to 64.0, "Reference voltage" to 3.3)).contains("Invalid"))
        assertInvalid { calc("ADC Resolution Calculator", mapOf("ADC bit depth" to 0.0, "Reference voltage" to 3.3)) }
        assertInvalid { calc("ADC Resolution Calculator", mapOf("ADC bit depth" to 12.5, "Reference voltage" to 3.3)) }
        assertInvalid { calc("ADC Resolution Calculator", mapOf("ADC bit depth" to 12.0, "Reference voltage" to 0.0)) }
    }
}
