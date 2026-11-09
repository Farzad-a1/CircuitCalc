package com.circuitcalc.electronics.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    private data class Case(val name:String,val values:Map<String,Double>,val expected:String)
    private val cases = listOf(
        Case("Ohm’s Law",mapOf("Voltage" to 12.0,"Current" to .02),"Resistance 600 Ω"),
        Case("Resistor Color Code (4-band)",mapOf("Band 1 digit (0–9)" to 1.0,"Band 2 digit (0–9)" to 0.0,"Multiplier (Ω)" to 100.0,"Tolerance (%)" to 5.0),"Resistance 1000 Ω"),
        Case("LED Series Resistor",mapOf("Supply voltage" to 12.0,"LED forward voltage" to 2.0,"LED current (mA)" to 20.0),"Series resistor 500 Ω"),
        Case("Voltage Divider",mapOf("Input voltage" to 12.0,"R1 (top)" to 10000.0,"R2 (bottom)" to 10000.0),"Output 6 V"),
        Case("Series / Parallel Resistors",mapOf("R1" to 100.0,"R2" to 220.0,"R3 (optional)" to 330.0),"Series 650 Ω"),
        Case("Capacitor from Charge",mapOf("Charge" to .0001,"Voltage" to 5.0),"2.0000e-"),
        Case("RC / RL Time Constant",mapOf("Resistance" to 10000.0,"Capacitance" to .0001,"Inductance (optional)" to .02),"RC τ 1 s"),
        Case("Op-Amp Non-Inverting Gain",mapOf("R feedback" to 9000.0,"R ground" to 1000.0,"Input voltage" to 1.0),"Gain 10 V/V"),
        Case("555 Astable Frequency",mapOf("RA" to 1000.0,"RB" to 1000.0,"Capacitance" to .000001),"Frequency 480 Hz"),
        Case("Electrical Power",mapOf("Voltage RMS / DC" to 230.0,"Current RMS / DC" to 2.0,"Power factor (1 for DC)" to .8),"Real power 368 W"),
        Case("Capacitor Energy",mapOf("Capacitance" to .001,"Voltage" to 12.0),"0.072 J"),
        Case("Inductive Reactance",mapOf("Frequency" to 50.0,"Inductance" to .1),"31.416 Ω"),
        Case("Capacitive Reactance",mapOf("Frequency" to 50.0,"Capacitance" to .0001),"31.831 Ω"),
        Case("LC Resonant Frequency",mapOf("Inductance" to .01,"Capacitance" to .0001),"159.15 Hz"),
        Case("Transformer Turns Ratio",mapOf("Primary turns" to 1000.0,"Secondary turns" to 100.0,"Primary RMS voltage" to 230.0),"Ideal secondary 23 V"),
        Case("Battery Runtime Estimate",mapOf("Capacity" to 2000.0,"Load current" to 200.0,"Usable fraction (0–1)" to .8),"8 h"),
        Case("DC Wire Voltage Drop",mapOf("Current" to 5.0,"One-way length" to 10.0,"Conductor area" to 2.5,"Resistivity" to .0175),"0.7 V"),
        Case("dBm to Watts",mapOf("Power" to 0.0),"0.001 W"),
        Case("Voltage Gain to Decibels",mapOf("Voltage gain (Vout/Vin)" to 10.0),"20 dB"),
        Case("Frequency / Wavelength",mapOf("Frequency" to 1000000.0,"Propagation velocity" to 299792458.0),"299.79 m"),
        Case("Duty Cycle",mapOf("High time" to .003,"Low time" to .007),"30%"),
        Case("Parallel Capacitors",mapOf("C1" to .000001,"C2" to .000002,"C3 (optional)" to 0.0),"3.0000e-"),
        Case("Zener Series Resistor",mapOf("Supply voltage" to 12.0,"Zener voltage" to 5.1,"Load current (mA)" to 20.0,"Minimum zener current (mA)" to 5.0),"Series resistor 276 Ω"),
        Case("Diode String Resistor",mapOf("Supply voltage" to 12.0,"Diode forward voltage" to 2.0,"Diode count" to 3.0,"Current (mA)" to 20.0),"Series resistor 300 Ω"),
        Case("BJT Base Resistor",mapOf("Drive voltage" to 5.0,"Base-emitter voltage" to .7,"Collector current (mA)" to 20.0,"Forced beta" to 10.0),"Base resistor 2150 Ω"),
        Case("MOSFET Conduction Loss",mapOf("RDS(on)" to .05,"RMS current" to 10.0,"Duty cycle (0–1)" to .5),"2.5 W"),
        Case("RC Charging Voltage",mapOf("Supply voltage" to 5.0,"Resistance" to 100000.0,"Capacitance" to .0001,"Elapsed time" to 10.0),"3.1606 V"),
        Case("RC Filter Cutoff",mapOf("Resistance" to 1000.0,"Capacitance" to .000001),"159.15 Hz"),
        Case("Power Supply Efficiency",mapOf("Output power" to 8.0,"Input power" to 10.0),"Efficiency 80%"),
        Case("Resistor Power Rating",mapOf("Voltage across resistor" to 12.0,"Resistance" to 100.0),"1.44 W")
    )

    @Test fun everyV1ReferenceCaseMatches() {
        assertEquals(40, CalculatorEngine.calculators.size)
        assertEquals(30, cases.size)
        cases.forEach { case -> assertTrue("${case.name}: ${CalculatorEngine.calculate(case.name,case.values)}", CalculatorEngine.calculate(case.name,case.values).contains(case.expected)) }
    }

    @Test fun validatesMissingAndInvalidInputs() {
        listOf(
            "Ohm’s Law" to emptyMap(),
            "LED Series Resistor" to mapOf("Supply voltage" to 1.0,"LED forward voltage" to 2.0,"LED current (mA)" to 20.0),
            "Capacitive Reactance" to mapOf("Frequency" to 0.0,"Capacitance" to 1.0),
            "Power Supply Efficiency" to mapOf("Output power" to 11.0,"Input power" to 10.0)
        ).forEach { (name,values) -> try { CalculatorEngine.calculate(name,values); throw AssertionError("Expected validation for $name") } catch (_: IllegalArgumentException) { } }
    }
}
