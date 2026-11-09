package com.circuitcalc.electronics.domain

import kotlin.math.*

enum class FieldInputType { NUMBER, TEXT }
data class Field(
    val name: String,
    val unit: String = "",
    val initial: String = "",
    val inputType: FieldInputType = FieldInputType.NUMBER,
    val options: List<String> = emptyList()
)
data class Calculator(val name: String, val category: String, val formula: String, val fields: List<Field>)

/** The v1.0 calculation catalog and formulas ported from lib/calculations.dart. */
object CalculatorEngine {
    private val resistorColorDigits = mapOf("Black" to 0, "Brown" to 1, "Red" to 2, "Orange" to 3, "Yellow" to 4, "Green" to 5, "Blue" to 6, "Violet" to 7, "Gray" to 8, "White" to 9)
    private val resistorMultiplier = mapOf("Pink" to 1e-3, "Silver" to 1e-2, "Gold" to 1e-1, "Black" to 1.0, "Brown" to 10.0, "Red" to 100.0, "Orange" to 1e3, "Yellow" to 1e4, "Green" to 1e5, "Blue" to 1e6, "Violet" to 1e7, "Gray" to 1e8, "White" to 1e9)
    private val resistorTolerance = mapOf("Brown" to 1.0, "Red" to 2.0, "Orange" to .05, "Yellow" to .02, "Green" to .5, "Blue" to .25, "Violet" to .1, "Gray" to .01, "Gold" to 5.0, "Silver" to 10.0)
    private val resistorTcr = mapOf("Black" to 250, "Brown" to 100, "Red" to 50, "Orange" to 15, "Yellow" to 25, "Green" to 20, "Blue" to 10, "Violet" to 5, "Gray" to 1)
    val RESISTOR_COLORS = resistorColorDigits.keys.toList()
    val RESISTOR_MULTIPLIER_COLORS = resistorMultiplier.keys.toList()
    val RESISTOR_TOLERANCE_COLORS = resistorTolerance.keys.toList()
    val RESISTOR_TCR_COLORS = resistorTcr.keys.toList()

    val calculators = listOf(
        Calculator("Ohm’s Law", "Calculators", "V = I × R; P = V × I. Enter any two values.", listOf(Field("Voltage","V"),Field("Current","A"),Field("Resistance","Ω"))),
        Calculator("Resistor Color Code (4-band)", "Components", "R = (10 × first digit + second digit) × multiplier.", listOf(Field("Band 1 digit (0–9)"),Field("Band 2 digit (0–9)"),Field("Multiplier (Ω)"),Field("Tolerance (%)","","5"))),
        Calculator("LED Series Resistor", "Calculators", "R = (Vs − Vf) / I; resistor power = I²R.", listOf(Field("Supply voltage","V"),Field("LED forward voltage","V"),Field("LED current (mA)","mA","20"))),
        Calculator("Voltage Divider", "Calculators", "Vout = Vin × R2 / (R1 + R2), unloaded divider.", listOf(Field("Input voltage","V"),Field("R1 (top)","Ω"),Field("R2 (bottom)","Ω"))),
        Calculator("Series / Parallel Resistors", "Calculators", "Series: ΣR. Parallel: 1 / Σ(1/R).", listOf(Field("R1","Ω"),Field("R2","Ω"),Field("R3 (optional)","Ω","0"))),
        Calculator("Capacitor from Charge", "Calculators", "C = Q / V.", listOf(Field("Charge","C"),Field("Voltage","V"))),
        Calculator("RC / RL Time Constant", "Calculators", "RC: τ = RC. RL: τ = L/R. First-order ideal circuit.", listOf(Field("Resistance","Ω"),Field("Capacitance","F"),Field("Inductance (optional)","H","0"))),
        Calculator("Op-Amp Non-Inverting Gain", "Calculators", "Av = 1 + Rf/Rg; ideal op-amp, no saturation.", listOf(Field("R feedback","Ω"),Field("R ground","Ω"),Field("Input voltage","V"))),
        Calculator("555 Astable Frequency", "Calculators", "f ≈ 1.44 / ((RA + 2RB)C); ideal 555 approximation.", listOf(Field("RA","Ω"),Field("RB","Ω"),Field("Capacitance","F"))),
        Calculator("Electrical Power", "Calculators", "P = V × I × PF; single-phase or DC. Not three-phase.", listOf(Field("Voltage RMS / DC","V"),Field("Current RMS / DC","A"),Field("Power factor (1 for DC)","","1"))),
        Calculator("Capacitor Energy", "Calculators", "E = ½ C V².", listOf(Field("Capacitance","F"),Field("Voltage","V"))),
        Calculator("Inductive Reactance", "Calculators", "XL = 2πfL; ideal inductor.", listOf(Field("Frequency","Hz"),Field("Inductance","H"))),
        Calculator("Capacitive Reactance", "Calculators", "XC = 1 / (2πfC); ideal capacitor.", listOf(Field("Frequency","Hz"),Field("Capacitance","F"))),
        Calculator("LC Resonant Frequency", "Calculators", "f₀ = 1 / (2π√LC); ideal lumped LC.", listOf(Field("Inductance","H"),Field("Capacitance","F"))),
        Calculator("Transformer Turns Ratio", "Calculators", "Vs/Vp = Ns/Np for ideal transformer.", listOf(Field("Primary turns"),Field("Secondary turns"),Field("Primary RMS voltage","V"))),
        Calculator("Battery Runtime Estimate", "Calculators", "Hours ≈ capacity × usable fraction / load current. Estimate only.", listOf(Field("Capacity","mAh"),Field("Load current","mA"),Field("Usable fraction (0–1)","","0.8"))),
        Calculator("DC Wire Voltage Drop", "Calculators", "ΔV = I × 2Lρ/A; copper-like resistivity is user supplied.", listOf(Field("Current","A"),Field("One-way length","m"),Field("Conductor area","mm²"),Field("Resistivity","Ω·mm²/m","0.0175"))),
        Calculator("dBm to Watts", "Converters", "P(W) = 10^((dBm − 30)/10).", listOf(Field("Power","dBm"))),
        Calculator("Voltage Gain to Decibels", "Converters", "dB = 20 log₁₀(|Vout/Vin|), equal impedance.", listOf(Field("Voltage gain (Vout/Vin)"))),
        Calculator("Frequency / Wavelength", "Converters", "λ = v/f. Enter medium propagation velocity.", listOf(Field("Frequency","Hz"),Field("Propagation velocity","m/s","299792458"))),
        Calculator("Duty Cycle", "Calculators", "D = Thigh / (Thigh + Tlow) × 100%.", listOf(Field("High time","s"),Field("Low time","s"))),
        Calculator("Parallel Capacitors", "Calculators", "Ctotal = C1 + C2 + C3.", listOf(Field("C1","F"),Field("C2","F"),Field("C3 (optional)","F","0"))),
        Calculator("Zener Series Resistor", "Calculators", "R = (Vs − Vz) / (IL + Iz). Check power and regulation.", listOf(Field("Supply voltage","V"),Field("Zener voltage","V"),Field("Load current (mA)","mA"),Field("Minimum zener current (mA)","mA"))),
        Calculator("Diode String Resistor", "Calculators", "R = (Vs − N Vf) / I; identical forward drops assumed.", listOf(Field("Supply voltage","V"),Field("Diode forward voltage","V"),Field("Diode count"),Field("Current (mA)","mA"))),
        Calculator("BJT Base Resistor", "Calculators", "Ib = Ic/βforced; Rb = (Vdrive − Vbe)/Ib.", listOf(Field("Drive voltage","V"),Field("Base-emitter voltage","V"),Field("Collector current (mA)","mA"),Field("Forced beta","","10"))),
        Calculator("MOSFET Conduction Loss", "Calculators", "P ≈ I²RDS(on)D; ignores switching and temperature effects.", listOf(Field("RDS(on)","Ω"),Field("RMS current","A"),Field("Duty cycle (0–1)","","1"))),
        Calculator("RC Charging Voltage", "Calculators", "Vc(t) = Vs(1 − e^(−t/RC)); ideal step response.", listOf(Field("Supply voltage","V"),Field("Resistance","Ω"),Field("Capacitance","F"),Field("Elapsed time","s"))),
        Calculator("RC Filter Cutoff", "Calculators", "fc = 1 / (2πRC), first-order RC low-pass/high-pass.", listOf(Field("Resistance","Ω"),Field("Capacitance","F"))),
        Calculator("Power Supply Efficiency", "Calculators", "η = Pout / Pin × 100%.", listOf(Field("Output power","W"),Field("Input power","W"))),
        Calculator("Resistor Power Rating", "Calculators", "P = V²/R. Choose a rated part with suitable derating.", listOf(Field("Voltage across resistor","V"),Field("Resistance","Ω")))
        ,Calculator("SMD Resistor Decoder", "Components", "3-digit: AB × 10^C Ω; 4-digit: ABC × 10^D Ω; EIA-96: indexed E96 value × multiplier.", listOf(Field("Marking", initial="103", inputType=FieldInputType.TEXT)))
        ,Calculator("Resistor Color Code (5/6-band)", "Components", "Bands 1–3 are significant digits; band 4 is multiplier; band 5 is tolerance; band 6 is TCR in ppm/K.", listOf(
            Field("Band count", initial="5", options=listOf("5", "6")),
            Field("Band 1", initial="Brown", options=RESISTOR_COLORS), Field("Band 2", initial="Black", options=RESISTOR_COLORS),
            Field("Band 3", initial="Black", options=RESISTOR_COLORS), Field("Band 4 multiplier", initial="Red", options=RESISTOR_MULTIPLIER_COLORS),
            Field("Band 5 tolerance", initial="Brown", options=RESISTOR_TOLERANCE_COLORS), Field("Band 6 TCR (ppm/K)", initial="Brown", options=RESISTOR_TCR_COLORS)
        ))
        ,Calculator("Nearest E-Series Resistor", "Components", "Select the closest IEC 60063 preferred resistance value. Deviation is signed relative to the requested value.", listOf(Field("E-series", initial="E24", options=listOf("E6", "E12", "E24", "E48", "E96", "E192")), Field("Desired resistance", "Ω")))
        ,Calculator("Series Capacitors", "Calculators", "1/Ctotal = Σ(1/Ci). Ideal capacitors in series; leakage and tolerances are ignored.", listOf(Field("C1", "F"), Field("C2", "F"), Field("C3 (optional)", "F"), Field("C4 (optional)", "F"), Field("C5 (optional)", "F"), Field("C6 (optional)", "F")))
        ,Calculator("555 Monostable Calculator", "Calculators", "T ≈ 1.1 × R × C. Ideal 555 timing approximation; real pulse width depends on device and component tolerances.", listOf(Field("Timing resistance", "Ω"), Field("Timing capacitance", "F")))
        ,Calculator("Inverting Op-Amp Calculator", "Calculators", "Av = −Rf/Rin; Vout = Av × Vin. Ideal closed-loop model; actual output must remain within the op-amp's supply-dependent output swing.", listOf(Field("Feedback resistance Rf", "Ω"), Field("Input resistance Rin", "Ω"), Field("Input voltage", "V")))
        ,Calculator("AC RLC Impedance (Series)", "Calculators", "XL = 2πfL; XC = 1/(2πfC); |Z| = √(R² + (XL−XC)²). Series topology only; ideal lumped components.", listOf(Field("Resistance", "Ω"), Field("Inductance", "H"), Field("Capacitance", "F"), Field("Frequency", "Hz")))
        ,Calculator("Three-Phase Power (Balanced)", "Calculators", "P = √3 VL IL PF; S = √3 VL IL; Q = ±√(S²−P²). Balanced sinusoidal RMS line quantities.", listOf(Field("Line voltage", "V"), Field("Line current", "A"), Field("Power factor (0–1)", initial="1"), Field("Load characteristic", initial="Lagging", options=listOf("Lagging", "Leading"))))
        ,Calculator("Battery Pack Calculator", "Calculators", "Series: Vpack = Ns Vcell. Parallel: capacity = Np Ccell. Nominal energy = Vpack × Ah.", listOf(Field("Nominal cell voltage", "V"), Field("Cell capacity", "mAh"), Field("Series cells (Ns)"), Field("Parallel cells (Np)")))
        ,Calculator("ADC Resolution Calculator", "Calculators", "Levels = 2^N; maximum code = 2^N−1; 1 LSB = Vref/2^N; ideal quantization error = ±0.5 LSB.", listOf(Field("ADC bit depth"), Field("Reference voltage", "V")))
    )

    fun calculate(name: String, input: Map<String, Double>, textInputs: Map<String, String> = emptyMap()): String {
        fun v(k: String, positive: Boolean = false, negative: Boolean = false): Double {
            val n = input[k] ?: throw IllegalArgumentException("Enter $k.")
            if (!n.isFinite() || (!negative && n < 0) || (positive && n == 0.0)) throw IllegalArgumentException("$k must be ${if (positive) "greater than zero" else "zero or greater"}.")
            return n
        }
        fun f(n: Double, unit: String = ""): String {
            if (!n.isFinite()) throw IllegalArgumentException("Result is outside the valid range.")
            if (n == 0.0) return "0$unit"
            val a = abs(n)
            val s = if (a != 0.0 && (a >= 1e6 || a < 1e-3)) String.format(java.util.Locale.US, "%.4e", n) else {
                val raw = String.format(java.util.Locale.US, "%.5g", n)
                val pieces = raw.split('e')
                val mantissa = pieces[0].trimEnd('0').trimEnd('.')
                if (pieces.size == 2) "$mantissa e${pieces[1]}".replace(" e", "e") else mantissa
            }
            return "$s$unit"
        }
        fun resistance(n: Double): String = when {
            !n.isFinite() -> throw IllegalArgumentException("Result is outside the valid range.")
            abs(n) >= 1e6 -> "${f(n / 1e6)} MΩ"
            abs(n) >= 1e3 -> "${f(n / 1e3)} kΩ"
            else -> "${f(n)} Ω"
        }
        val result = when (name) {
            "Ohm’s Law" -> { val voltage=input["Voltage"]; val current=input["Current"]; val resistance=input["Resistance"]; val (vv,ii,rr)=when {voltage!=null&&current!=null->Triple(voltage,current,voltage/current);voltage!=null&&resistance!=null->Triple(voltage,voltage/resistance,resistance);current!=null&&resistance!=null->Triple(current*resistance,current,resistance);else->throw IllegalArgumentException("Enter any two of voltage, current and resistance.")}; if(listOf(vv,ii,rr).any{!it.isFinite()}||rr<=0)throw IllegalArgumentException("Inputs must produce a finite result and positive resistance."); "Voltage ${f(vv," V")}  •  Current ${f(ii," A")}\nResistance ${f(rr," Ω")}  •  Power ${f(vv*ii," W")}" }
            "Resistor Color Code (4-band)" -> { val a=v("Band 1 digit (0–9)");val b=v("Band 2 digit (0–9)");val m=v("Multiplier (Ω)");val t=v("Tolerance (%)");if(a !in 1.0..9.0||a%1!=0.0||b>9||b%1!=0.0||t>100)throw IllegalArgumentException("Digits must be 0–9; tolerance must be at most 100%.");val r=(10*a+b)*m;"Resistance ${f(r," Ω")} ±${f(t,"%\nRange ")}${f(r*(1-t/100)," Ω")} – ${f(r*(1+t/100)," Ω")}" }
            "LED Series Resistor" -> {val s=v("Supply voltage");val vf=v("LED forward voltage");val i=v("LED current (mA)",true)/1000;if(s<=vf)throw IllegalArgumentException("Supply voltage must exceed LED forward voltage.");val r=(s-vf)/i;"Series resistor ${f(r," Ω")}\nPower ${f(i*i*r," W")}"}
            "Voltage Divider" -> {val vin=v("Input voltage");val a=v("R1 (top)",true);val b=v("R2 (bottom)",true);"Output ${f(vin*b/(a+b)," V")}\nDivider current ${f(vin/(a+b)," A")}"}
            "Series / Parallel Resistors" -> {val a=v("R1");val b=v("R2");val c=v("R3 (optional)");if(a==0.0||b==0.0)throw IllegalArgumentException("R1 and R2 must be greater than zero.");"Series ${f(a+b+c," Ω")}\nParallel ${f(1/(1/a+1/b+if(c>0)1/c else 0.0)," Ω")}"}
            "Capacitor from Charge" -> "Capacitance ${f(v("Charge")/v("Voltage",true)," F")}" 
            "RC / RL Time Constant" -> {val r=v("Resistance",true);val c=v("Capacitance",true);val l=v("Inductance (optional)");"RC τ ${f(r*c," s")}\nRL τ ${f(l/r," s")}"}
            "Op-Amp Non-Inverting Gain" -> {val gain=1+v("R feedback")/v("R ground",true);"Gain ${f(gain)} V/V\nIdeal output ${f(gain*v("Input voltage")," V")}"}
            "555 Astable Frequency" -> {val a=v("RA");val b=v("RB");val c=v("Capacitance");if(a==0.0||b==0.0||c==0.0)throw IllegalArgumentException("All timing components must be positive.");val hz=1.44/((a+2*b)*c);"Frequency ${f(hz," Hz")}\nPeriod ${f(1/hz," s")}"}
            "Electrical Power" -> {val volts=v("Voltage RMS / DC");val amps=v("Current RMS / DC");val pf=v("Power factor (1 for DC)");if(pf>1)throw IllegalArgumentException("Power factor must be between 0 and 1.");"Real power ${f(volts*amps*pf," W")}\nApparent power ${f(volts*amps," VA")}"}
            "Capacitor Energy" -> "Stored energy ${f(.5*v("Capacitance")*v("Voltage").pow(2)," J")}" 
            "Inductive Reactance" -> "Reactance ${f(2*PI*v("Frequency")*v("Inductance")," Ω")}" 
            "Capacitive Reactance" -> "Reactance ${f(1/(2*PI*v("Frequency",true)*v("Capacitance",true))," Ω")}" 
            "LC Resonant Frequency" -> "Resonance ${f(1/(2*PI*sqrt(v("Inductance",true)*v("Capacitance",true)))," Hz")}" 
            "Transformer Turns Ratio" -> {val p=v("Primary turns",true);val s=v("Secondary turns",true);val volts=v("Primary RMS voltage");"Turns ratio ${f(p/s)}:1\nIdeal secondary ${f(volts*s/p," V")}"}
            "Battery Runtime Estimate" -> {val c=v("Capacity");val i=v("Load current",true);val u=v("Usable fraction (0–1)");if(u>1)throw IllegalArgumentException("Usable fraction must not exceed 1.");"Estimated runtime ${f(c*u/i," h")}"}
            "DC Wire Voltage Drop" -> "Round-trip drop ${f(v("Current")*2*v("One-way length")*v("Resistivity",true)/v("Conductor area",true)," V")}" 
            "dBm to Watts" -> f(10.0.pow((v("Power",negative=true)-30)/10)," W")
            "Voltage Gain to Decibels" -> f(20*ln(v("Voltage gain (Vout/Vin)",true))/ln(10.0)," dB")
            "Frequency / Wavelength" -> "Wavelength ${f(v("Propagation velocity",true)/v("Frequency",true)," m")}" 
            "Duty Cycle" -> {val h=v("High time");val l=v("Low time");if(h+l==0.0)throw IllegalArgumentException("Total period must be greater than zero.");"Duty cycle ${f(100*h/(h+l),"%")}"}
            "Parallel Capacitors" -> "Total ${f(v("C1")+v("C2")+v("C3 (optional)")," F")}" 
            "Zener Series Resistor" -> {val s=v("Supply voltage");val z=v("Zener voltage");val load=v("Load current (mA)");val iz=v("Minimum zener current (mA)");if(s<=z||load+iz==0.0)throw IllegalArgumentException("Supply must exceed zener voltage; total current must be positive.");val r=(s-z)/((load+iz)/1000);"Series resistor ${f(r," Ω")}\nResistor dissipation ${f((s-z)*(s-z)/r," W")}"}
            "Diode String Resistor" -> {val s=v("Supply voltage");val vf=v("Diode forward voltage");val n=v("Diode count",true);val i=v("Current (mA)",true)/1000;if(n%1!=0.0||s<=n*vf)throw IllegalArgumentException("Diode count must be a whole number and supply must exceed total drop.");val r=(s-n*vf)/i;"Series resistor ${f(r," Ω")}\nPower ${f(i*i*r," W")}"}
            "BJT Base Resistor" -> {val supply=v("Drive voltage");val be=v("Base-emitter voltage");val ic=v("Collector current (mA)",true)/1000;val beta=v("Forced beta",true);if(supply<=be)throw IllegalArgumentException("Drive voltage must exceed base-emitter voltage.");"Base current ${f(ic/beta," A")}\nBase resistor ${f((supply-be)*beta/ic," Ω")}"}
            "MOSFET Conduction Loss" -> {val r=v("RDS(on)");val i=v("RMS current");val d=v("Duty cycle (0–1)");if(d>1)throw IllegalArgumentException("Duty cycle must be 0–1.");"Conduction loss ${f(i*i*r*d," W")}"}
            "RC Charging Voltage" -> {val s=v("Supply voltage");val r=v("Resistance",true);val c=v("Capacitance");val t=v("Elapsed time");"Capacitor voltage ${f(s*(1-exp(-t/(r*c)))," V")}"}
            "RC Filter Cutoff" -> "Cutoff ${f(1/(2*PI*v("Resistance",true)*v("Capacitance",true))," Hz")}" 
            "Power Supply Efficiency" -> {val out=v("Output power");val inp=v("Input power",true);if(out>inp)throw IllegalArgumentException("Output power cannot exceed input power.");"Efficiency ${f(100*out/inp,"%\nLoss ")}${f(inp-out," W")}"}
            "Resistor Power Rating" -> "Dissipation ${f(v("Voltage across resistor").pow(2)/v("Resistance",true)," W")}" 
            "SMD Resistor Decoder" -> decodeSmd(textInputs["Marking"] ?: throw IllegalArgumentException("Enter the resistor marking."), ::resistance)
            "Resistor Color Code (5/6-band)" -> {
                val bands = textInputs["Band count"]?.toIntOrNull() ?: throw IllegalArgumentException("Select 5 or 6 bands.")
                if (bands !in 5..6) throw IllegalArgumentException("Select 5 or 6 bands.")
                val first = resistorColorDigits[textInputs["Band 1"]] ?: throw IllegalArgumentException("Band 1 must be a significant-digit color.")
                if (first == 0) throw IllegalArgumentException("The first significant digit cannot be black (zero).")
                val second = resistorColorDigits[textInputs["Band 2"]] ?: throw IllegalArgumentException("Band 2 must be a significant-digit color.")
                val third = resistorColorDigits[textInputs["Band 3"]] ?: throw IllegalArgumentException("Band 3 must be a significant-digit color.")
                val multiplier = resistorMultiplier[textInputs["Band 4 multiplier"]] ?: throw IllegalArgumentException("Choose a valid multiplier color.")
                val tolerance = resistorTolerance[textInputs["Band 5 tolerance"]] ?: throw IllegalArgumentException("Choose a valid tolerance color.")
                val ohms = ((first * 100 + second * 10 + third) * multiplier)
                if (!ohms.isFinite() || ohms <= 0.0) throw IllegalArgumentException("Color bands produce an invalid resistance.")
                val toleranceText = "±${f(tolerance, "%")}"
                val rangeText = "Range ${resistance(ohms * (1 - tolerance / 100))} – ${resistance(ohms * (1 + tolerance / 100))}"
                val tcr = if (bands == 6) {
                    val ppm = resistorTcr[textInputs["Band 6 TCR (ppm/K)"]] ?: throw IllegalArgumentException("Choose a supported sixth-band TCR color.")
                    "\nTemperature coefficient ±$ppm ppm/K"
                } else ""
                "Resistance ${resistance(ohms)} $toleranceText\n$rangeText$tcr"
            }
            "Nearest E-Series Resistor" -> {
                val series = textInputs["E-series"] ?: throw IllegalArgumentException("Select an E-series.")
                val desired = v("Desired resistance", true)
                val resultValues = nearestEValues(series, desired)
                "Nearest ${resistance(resultValues.nearest)}\nLower ${resistance(resultValues.lower)}\nHigher ${resistance(resultValues.higher)}\nDeviation ${f((resultValues.nearest - desired) / desired * 100, "%")}"
            }
            "Series Capacitors" -> {
                val keys = listOf("C1", "C2", "C3 (optional)", "C4 (optional)", "C5 (optional)", "C6 (optional)")
                val caps = keys.mapNotNull { key -> input[key]?.let { value ->
                    if (value == 0.0 && key.contains("optional", true)) null
                    else { if (!value.isFinite() || value <= 0.0) throw IllegalArgumentException("$key must be greater than zero."); value }
                } }
                if (caps.size < 2) throw IllegalArgumentException("Enter at least two positive capacitor values.")
                val total = 1.0 / caps.sumOf { 1.0 / it }
                "Equivalent capacitance ${f(total, " F")}\nCapacitors in series ${caps.size}"
            }
            "555 Monostable Calculator" -> {
                val duration = 1.1 * v("Timing resistance", true) * v("Timing capacitance", true)
                "Ideal pulse width ${f(duration, " s")}\nAssumes the approximate 555 monostable timing model T ≈ 1.1RC."
            }
            "Inverting Op-Amp Calculator" -> {
                val rf = v("Feedback resistance Rf", true); val rin = v("Input resistance Rin", true); val vin = v("Input voltage", negative = true)
                val gain = -rf / rin; val output = gain * vin
                "Ideal gain ${f(gain, " V/V")}\nIdeal output ${f(output, " V")}\nReal output is limited by the selected op-amp supply rails, output swing, load, bandwidth and slew rate."
            }
            "AC RLC Impedance (Series)" -> {
                val r = v("Resistance"); val l = v("Inductance", true); val c = v("Capacitance", true); val frequency = v("Frequency")
                val xl = 2 * PI * frequency * l
                if (frequency == 0.0) "Inductive reactance ${f(0.0, " Ω")}\nCapacitive reactance infinite (ideal series capacitor is open at DC)\nImpedance magnitude infinite\nPhase angle −90°"
                else {
                    val xc = 1 / (2 * PI * frequency * c)
                    val rawNet = xl - xc
                    val net = if (abs(rawNet) <= max(xl, xc) * 1e-12) 0.0 else rawNet
                    val magnitude = hypot(r, net)
                    val phase = if (r == 0.0 && net == 0.0) null else Math.toDegrees(atan2(net, r))
                    "Inductive reactance ${f(xl, " Ω")}\nCapacitive reactance ${f(xc, " Ω")}\nImpedance magnitude ${f(magnitude, " Ω")}\nPhase angle ${phase?.let { f(it, "°") } ?: "undefined at ideal zero-impedance resonance"}"
                }
            }
            "Three-Phase Power (Balanced)" -> {
                val voltage = v("Line voltage", true); val current = v("Line current", true); val pf = v("Power factor (0–1)")
                if (pf > 1.0) throw IllegalArgumentException("Power factor must be between 0 and 1.")
                val characteristic = textInputs["Load characteristic"] ?: "Lagging"
                if (characteristic !in listOf("Lagging", "Leading")) throw IllegalArgumentException("Select leading or lagging load.")
                val apparent = sqrt(3.0) * voltage * current
                val real = apparent * pf
                val reactive = apparent * sqrt(max(0.0, 1 - pf * pf)) * if (characteristic == "Leading") -1 else 1
                val angle = Math.toDegrees(acos(pf)) * if (characteristic == "Leading") -1 else 1
                "Real power ${f(real, " W")}\nReactive power ${f(reactive, " var")} (${characteristic.lowercase()})\nApparent power ${f(apparent, " VA")}\nPower factor ${f(pf)}; phase angle ${f(angle, "°")}\nBalanced three-phase sinusoidal RMS line quantities."
            }
            "Battery Pack Calculator" -> {
                val voltage = v("Nominal cell voltage", true); val capacityMah = v("Cell capacity", true)
                val series = wholeCount("Series cells (Ns)", v("Series cells (Ns)", true)); val parallel = wholeCount("Parallel cells (Np)", v("Parallel cells (Np)", true))
                val packVoltage = voltage * series; val packCapacity = capacityMah * parallel; val energy = packVoltage * packCapacity / 1000.0
                "Nominal pack voltage ${f(packVoltage, " V")}\nPack capacity ${f(packCapacity, " mAh")}\nNominal energy ${f(energy, " Wh")}\nTotal cells ${series.toLong() * parallel.toLong()}\nActual performance depends on matched cells, discharge limits, protection circuitry and operating conditions. This is not a charging recommendation."
            }
            "ADC Resolution Calculator" -> {
                val bits = wholeCount("ADC bit depth", v("ADC bit depth", true))
                if (bits !in 1..64) throw IllegalArgumentException("ADC bit depth must be a whole number from 1 through 64.")
                val reference = v("Reference voltage", true)
                val levels = java.math.BigInteger.ONE.shiftLeft(bits)
                val lsb = reference / levels.toDouble()
                if (!lsb.isFinite() || lsb == 0.0) throw IllegalArgumentException("Reference voltage and bit depth exceed the supported numeric range.")
                "Quantization levels $levels\nVoltage per LSB ${f(lsb, " V")}\nMaximum digital code ${levels - java.math.BigInteger.ONE}\nIdeal quantization error ±${f(lsb / 2, " V")}\nAssumes a single-ended ideal transfer from 0 V to Vref with 2^N code bins; actual ADC errors are excluded."
            }
            else -> throw IllegalArgumentException("Unknown calculator.")
        }
        return result
    }

    private data class ESeriesNeighbors(val lower: Double, val nearest: Double, val higher: Double)

    private fun wholeCount(label: String, value: Double): Int {
        if (!value.isFinite() || value % 1.0 != 0.0 || value > 1_000_000.0) throw IllegalArgumentException("$label must be a whole number from 1 to 1,000,000.")
        return value.toInt()
    }

    private fun nearestEValues(series: String, desired: Double): ESeriesNeighbors {
        val values = when (series) {
            "E6" -> listOf(1.0, 1.5, 2.2, 3.3, 4.7, 6.8)
            "E12" -> listOf(1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2)
            "E24" -> listOf(1.0, 1.1, 1.2, 1.3, 1.5, 1.6, 1.8, 2.0, 2.2, 2.4, 2.7, 3.0, 3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6, 6.2, 6.8, 7.5, 8.2, 9.1)
            "E48" -> listOf(100,105,110,115,121,127,133,140,147,154,162,169,178,187,196,205,215,226,237,249,261,274,287,301,316,332,348,365,383,402,422,442,464,487,511,536,562,590,619,649,681,715,750,787,825,866,909,953).map { it / 100.0 }
            "E96" -> listOf(100,102,105,107,110,113,115,118,121,124,127,130,133,137,140,143,147,150,154,158,162,165,169,174,178,182,187,191,196,200,205,210,215,221,226,232,237,243,249,255,261,267,274,280,287,294,301,309,316,324,332,340,348,357,365,374,383,392,402,412,422,432,442,453,464,475,487,499,511,523,536,549,562,576,590,604,619,634,649,665,681,698,715,732,750,768,787,806,825,845,866,887,909,931,953,976).map { it / 100.0 }
            "E192" -> (0 until 192).map { index -> round(100.0 * 10.0.pow(index / 192.0)) / 100.0 }
            else -> throw IllegalArgumentException("Choose E6, E12, E24, E48, E96 or E192.")
        }
        if (desired !in 1e-300..1e300) throw IllegalArgumentException("Desired resistance must be between 1e-300 Ω and 1e300 Ω.")
        val decade = floor(log10(desired)).toInt()
        val candidates = (-1..1).flatMap { offset ->
            val scale = 10.0.pow(decade + offset)
            values.map { it * scale }
        }.filter { it.isFinite() && it > 0.0 }.distinct().sorted()
        val lower = candidates.lastOrNull { it <= desired } ?: throw IllegalArgumentException("Resistance is below the supported range.")
        val higher = candidates.firstOrNull { it >= desired } ?: throw IllegalArgumentException("Resistance is above the supported range.")
        val nearest = if (desired - lower <= higher - desired) lower else higher
        return ESeriesNeighbors(lower, nearest, higher)
    }

    private fun decodeSmd(marking: String, formatResistance: (Double) -> String): String {
        val code = marking.trim().uppercase(java.util.Locale.US)
        val resistance = when {
            code.matches(Regex("[0-9]{3}")) -> {
                if (code == "000") 0.0 else code.substring(0, 2).toDouble() * 10.0.pow(code.last().digitToInt())
            }
            code.matches(Regex("[0-9]{4}")) -> code.substring(0, 3).toDouble() * 10.0.pow(code.last().digitToInt())
            code.matches(Regex("[0-9]{2}[A-FXY]")) -> {
                val e96 = listOf(100,102,105,107,110,113,115,118,121,124,127,130,133,137,140,143,147,150,154,158,162,165,169,174,178,182,187,191,196,200,205,210,215,221,226,232,237,243,249,255,261,267,274,280,287,294,301,309,316,324,332,340,348,357,365,374,383,392,402,412,422,432,442,453,464,475,487,499,511,523,536,549,562,576,590,604,619,634,649,665,681,698,715,732,750,768,787,806,825,845,866,887,909,931,953,976)
                val index = code.substring(0, 2).toInt()
                if (index !in 1..96) throw IllegalArgumentException("EIA-96 index must be from 01 through 96.")
                val multiplier = mapOf('Y' to .01, 'X' to .1, 'A' to 1.0, 'B' to 10.0, 'C' to 100.0, 'D' to 1e3, 'E' to 1e4, 'F' to 1e5).getValue(code.last())
                e96[index - 1] * multiplier
            }
            else -> throw IllegalArgumentException("Unsupported or ambiguous marking. Enter 3 digits, 4 digits, or EIA-96 (01–96 plus A–F, X or Y); confirm unusual markings against the manufacturer's data sheet.")
        }
        if (!resistance.isFinite()) throw IllegalArgumentException("Decoded resistance is outside the supported numeric range.")
        return "Decoded $code = ${formatResistance(resistance)}\nSMD marking conventions can vary; verify ambiguous top marks against the part manufacturer's data sheet."
    }
}
