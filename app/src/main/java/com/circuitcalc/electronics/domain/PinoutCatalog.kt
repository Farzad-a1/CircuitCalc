package com.circuitcalc.electronics.domain

enum class PinoutCategory(val label: String) {
    MICROCONTROLLERS("Microcontrollers"), DEVELOPMENT_BOARDS("Development Boards"),
    ICS("ICs"), TRANSISTORS("Transistors"), REGULATORS("Regulators"), CONNECTORS("Connectors")
}

enum class PinRole { POWER, GROUND, SIGNAL, NC }
enum class PinDiagram { DIP, TO220, TO92, BOARD, USB_A }

data class PinFunction(
    val number: String,
    val label: String,
    val function: String,
    val role: PinRole = PinRole.SIGNAL,
    val header: String = ""
)

data class PinoutEntry(
    val id: String,
    val name: String,
    val partNumber: String,
    val category: PinoutCategory,
    val packageOrRevision: String,
    val diagramView: String,
    val diagram: PinDiagram,
    val pins: List<PinFunction>,
    val cautions: List<String>,
    val datasheetUrl: String,
    val sourceName: String
) {
    val pinCount: Int get() = pins.size
}

object PinoutCatalog {
    private fun pin(n: Int, label: String, function: String = label, role: PinRole = PinRole.SIGNAL) = PinFunction(n.toString(), label, function, role)
    private fun dip(labels: List<String>, functions: List<String> = labels, roles: Map<Int, PinRole> = emptyMap()) = labels.mapIndexed { i, label -> pin(i + 1, label, functions[i], roles[i + 1] ?: PinRole.SIGNAL) }

    val entries: List<PinoutEntry> = listOf(
        PinoutEntry("arduino-uno-r3", "Arduino Uno Rev3", "A000066", PinoutCategory.DEVELOPMENT_BOARDS, "UNO Rev3 · ATmega328P DIP version", "J_ANALOG / J_DIGITAL connector numbering", PinDiagram.BOARD,
            (listOf("NC","IOREF","RESET","3V3","5V","GND","GND","VIN","A0","A1","A2","A3","A4/SDA","A5/SCL").mapIndexed { i, label ->
                val role = when(label) { "3V3","5V","VIN","IOREF" -> PinRole.POWER; "GND" -> PinRole.GROUND; "NC" -> PinRole.NC; else -> PinRole.SIGNAL }
                PinFunction((i + 1).toString(), label, when(label) { "NC" -> "Not connected"; "IOREF" -> "Logic reference voltage"; "RESET" -> "Reset input"; "3V3" -> "3.3 V rail"; "5V" -> "5 V rail"; "VIN" -> "Voltage input"; "GND" -> "Ground"; "A4/SDA" -> "Analog input 4 / I²C data"; "A5/SCL" -> "Analog input 5 / I²C clock"; else -> if(label.startsWith("A")) "Analog input / GPIO" else label }, role, "J_ANALOG") } +
                listOf("D0","D1","D2","D3","D4","D5","D6","D7","D8","D9","D10/SS","D11/MOSI","D12/MISO","D13/SCK","GND","AREF","A4/SDA (duplicate)","A5/SCL (duplicate)").mapIndexed { i, label ->
                    val role = if (label == "GND") PinRole.GROUND else PinRole.SIGNAL
                    PinFunction((i + 1).toString(), label, when(i) { 0 -> "Digital I/O · UART receive"; 1 -> "Digital I/O · UART transmit"; in 3..5 -> if(i in listOf(3,5)) "Digital I/O · PWM capable" else "Digital I/O"; 6,9 -> "Digital I/O · PWM capable"; 10 -> "Digital I/O · PWM · SPI select"; 11 -> "Digital I/O · PWM · SPI MOSI"; 12 -> "Digital I/O · SPI MISO"; 13 -> "Digital I/O · SPI clock"; 14 -> "Ground"; 15 -> "Analog reference"; 16 -> "Duplicate of A4/SDA"; 17 -> "Duplicate of A5/SCL"; else -> "Digital I/O" }, role, "J_DIGITAL")
                }), listOf("J_ANALOG and J_DIGITAL pin numbers are local to each connector.", "I/O is 5 V logic. The 3.3 V pin is a limited-current output; check the official board documentation before powering loads."), "https://docs.arduino.cc/hardware/uno-rev3", "Arduino UNO R3 documentation and datasheet"),
        PinoutEntry("esp32-devkitc-v4", "ESP32-DevKitC V4", "ESP32-DevKitC V4 · ESP32-WROOM-32E", PinoutCategory.DEVELOPMENT_BOARDS, "V4 board with ESP32-WROOM-32E · 2×19 headers", "J2 / J3 header numbering", PinDiagram.BOARD,
            listOf("3V3","EN","VP","VN","IO34","IO35","IO32","IO33","IO25","IO26","IO27","IO14","IO12","GND","IO13","D2 / IO9","D3 / IO10","CMD / IO11","5V0").mapIndexed { i,n -> PinFunction((i+1).toString(),n, when(i){0->"3.3 V supply";1->"Chip enable / reset";2->"GPIO36 · ADC1_CH0 · input only";3->"GPIO39 · ADC1_CH3 · input only";4->"GPIO34 · ADC1_CH6 · input only";5->"GPIO35 · ADC1_CH7 · input only";13->"Ground";15->"GPIO9 · SPI flash (reserved)";16->"GPIO10 · SPI flash (reserved)";17->"GPIO11 · SPI flash (reserved)";18->"5 V supply";else->"GPIO ${n.removePrefix("IO")}"},when(i){0,18->PinRole.POWER;13->PinRole.GROUND;else->PinRole.SIGNAL},"J2") } +
                listOf("GND","IO23","IO22","IO1","IO3","IO21","GND","IO19","IO18","IO5","IO17","IO16","IO4","IO0","IO2","IO15","IO8 / D1","IO7 / D0","IO6 / CLK").mapIndexed { i,n -> PinFunction((i+1).toString(),n, when(i){0,6->"Ground";3->"GPIO1 · UART0 TX";4->"GPIO3 · UART0 RX";13->"GPIO0 · boot strapping";14->"GPIO2 · boot strapping";15->"GPIO15 · boot strapping";16->"GPIO8 · SPI flash (reserved)";17->"GPIO7 · SPI flash (reserved)";18->"GPIO6 · SPI flash (reserved)";else->"GPIO ${n.substringAfter("IO")}"},if(i==0||i==6)PinRole.GROUND else PinRole.SIGNAL,"J3") }
            , listOf("GPIO34–GPIO39 are input-only.", "GPIO6–GPIO11 are connected to module flash and should not be used as general GPIO.", "GPIO0, GPIO2, GPIO5, GPIO12 and GPIO15 are strapping pins; attached circuitry can affect boot.", "Use only 3.3 V logic on ESP32 GPIO."), "https://docs.espressif.com/projects/esp-dev-kits/en/latest/esp32/esp32-devkitc/user_guide.html", "Espressif ESP32-DevKitC V4 User Guide"),
        PinoutEntry("atmega328p-pu", "ATmega328P-PU", "ATmega328P-PU", PinoutCategory.MICROCONTROLLERS, "PDIP-28", "Top view · notch up", PinDiagram.DIP,
            dip(listOf("PC6/RESET","PD0/RXD","PD1/TXD","PD2/INT0","PD3/INT1","PD4","VCC","GND","PB6/XTAL1","PB7/XTAL2","PD5","PD6","PD7","PB0","PB1","PB2","PB3","PB4","PB5","AVCC","AREF","GND","PC0/ADC0","PC1/ADC1","PC2/ADC2","PC3/ADC3","PC4/SDA","PC5/SCL"),roles=mapOf(7 to PinRole.POWER,8 to PinRole.GROUND,20 to PinRole.POWER,21 to PinRole.POWER,22 to PinRole.GROUND)), listOf("AVCC must be connected to VCC, including when the ADC is unused.", "Observe the device's supply and I/O voltage limits; this device is not 5 V tolerant when operated at lower supply voltage."), "https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-7810-Automotive-Microcontrollers-ATmega328P_Datasheet.pdf", "Microchip ATmega328P datasheet"),
        PinoutEntry("ne555-pdip8", "TI NE555", "NE555", PinoutCategory.ICS, "PDIP-8", "Top view · notch up", PinDiagram.DIP,
            dip(listOf("GND","TRIG","OUT","RESET","CONT","THRES","DISCH","VCC"),roles=mapOf(1 to PinRole.GROUND,8 to PinRole.POWER)), listOf("Keep RESET high for normal operation.", "Use a bypass capacitor and stay within the supply and output-current ratings in the exact manufacturer's datasheet."), "https://www.ti.com/lit/ds/symlink/ne555.pdf", "Texas Instruments NE555 datasheet"),
        PinoutEntry("lm358ap", "LM358", "LM358AP", PinoutCategory.ICS, "TI PDIP-8", "Top view · notch up", PinDiagram.DIP,
            dip(listOf("OUT1","IN1−","IN1+","V−","IN2+","IN2−","OUT2","V+"),roles=mapOf(4 to PinRole.POWER,8 to PinRole.POWER)), listOf("V− is the negative supply rail; it is ground only in a single-supply circuit.", "Inputs and outputs do not reach the positive supply rail. Do not assume rail-to-rail behavior."), "https://www.ti.com/lit/ds/symlink/lm358.pdf", "Texas Instruments LM358 datasheet"),
        PinoutEntry("lm324an", "LM324", "LM324AN/NOPB", PinoutCategory.ICS, "TI PDIP-14", "Top view · notch up", PinDiagram.DIP,
            dip(listOf("1OUT","1IN−","1IN+","VCC+","2IN+","2IN−","2OUT","3OUT","3IN−","3IN+","VCC−","4IN+","4IN−","4OUT"),roles=mapOf(4 to PinRole.POWER,11 to PinRole.POWER)), listOf("VCC− is the negative supply rail; it is ground only in a single-supply circuit.", "The common-mode range excludes the positive rail; outputs are not rail-to-rail."), "https://www.ti.com/lit/ds/symlink/lm324b.pdf", "Texas Instruments LM324B datasheet"),
        PinoutEntry("l7805cv", "L7805CV", "L7805CV", PinoutCategory.REGULATORS, "ST TO-220", "Front view · leads down", PinDiagram.TO220,
            dip(listOf("INPUT","GND","OUTPUT"),roles=mapOf(1 to PinRole.POWER,2 to PinRole.GROUND,3 to PinRole.POWER)), listOf("The metal tab is connected to ground for this L78 TO-220 package.", "Linear regulation dissipates (VIN − 5 V) × load current as heat; use adequate heatsinking and observe input limits."), "https://www.st.com/resource/en/datasheet/CD00000444.pdf", "STMicroelectronics L78 datasheet"),
        PinoutEntry("lm317t", "LM317T", "LM317T", PinoutCategory.REGULATORS, "ST TO-220", "Front view · leads down", PinDiagram.TO220,
            dip(listOf("ADJUST","OUTPUT","INPUT"),roles=mapOf(2 to PinRole.POWER,3 to PinRole.POWER)), listOf("The metal tab is connected to OUTPUT.", "An external resistor network is required; preserve minimum load and dropout requirements from the datasheet."), "https://www.st.com/resource/en/datasheet/lm317.pdf", "STMicroelectronics LM317 datasheet"),
        PinoutEntry("sn74hc00n", "74HC00", "SN74HC00N", PinoutCategory.ICS, "TI PDIP-14", "Top view · notch up", PinDiagram.DIP,
            dip(listOf("1A","1B","1Y","2A","2B","2Y","GND","3Y","3A","3B","4Y","4A","4B","VCC"),roles=mapOf(7 to PinRole.GROUND,14 to PinRole.POWER)), listOf("Do not leave CMOS inputs floating.", "Use a supply within the part's specified range; HC input thresholds are CMOS-level."), "https://www.ti.com/lit/ds/symlink/sn74hc00.pdf", "Texas Instruments SN74HC00 datasheet"),
        PinoutEntry("sn74hc595n", "74HC595", "SN74HC595N", PinoutCategory.ICS, "TI PDIP-16", "Top view · notch up", PinDiagram.DIP,
            dip(listOf("QB","QC","QD","QE","QF","QG","QH","GND","QH′","SRCLR","SRCLK","RCLK","OE","SER","QA","VCC"),roles=mapOf(8 to PinRole.GROUND,16 to PinRole.POWER)), listOf("OE is active low; SRCLR is active low.", "Outputs are logic outputs, not load drivers; observe per-pin and package current limits."), "https://www.ti.com/lit/ds/symlink/sn74hc595.pdf", "Texas Instruments SN74HC595 datasheet"),
        PinoutEntry("uln2003an", "ULN2003A", "ULN2003AN", PinoutCategory.ICS, "TI PDIP-16", "Top view · notch up", PinDiagram.DIP,
            dip(listOf("IN1","IN2","IN3","IN4","IN5","IN6","IN7","GND","COM","OUT7","OUT6","OUT5","OUT4","OUT3","OUT2","OUT1"),roles=mapOf(8 to PinRole.GROUND,9 to PinRole.POWER)), listOf("COM connects to the clamp-diode common and is normally tied to the inductive load supply, not logic VCC.", "Outputs are low-side Darlington sinks; observe package dissipation and per-channel current limits."), "https://www.ti.com/lit/ds/symlink/uln2003a.pdf", "Texas Instruments ULN2003A datasheet"),
        PinoutEntry("pc817x", "PC817", "Sharp PC817XxNSZ1B", PinoutCategory.ICS, "Sharp DIP-4", "Top view · notch / dot marks pin 1", PinDiagram.DIP,
            dip(listOf("ANODE","CATHODE","EMITTER","COLLECTOR")), listOf("The input LED requires a current-limiting resistor.", "Observe isolation voltage, creepage, clearance, and optocoupler CTR limits for the exact design."), "https://global.sharp/products/device/lineup/data/pdf/datasheet/PC817XxNSZ1B_e.pdf", "Sharp PC817XxNSZ1B datasheet"),
        PinoutEntry("pn2222ata", "PN2222A", "onsemi PN2222ATA", PinoutCategory.TRANSISTORS, "TO-92 3L · onsemi style", "Front view · flat face toward viewer", PinDiagram.TO92,
            dip(listOf("EMITTER","BASE","COLLECTOR")), listOf("Pin order varies among 2N2222-family manufacturers and packages; this entry applies only to PN2222ATA.", "Use a base resistor and observe the specific datasheet current and power limits."), "https://www.onsemi.com/pdf/datasheet/pn2222a-d.pdf", "onsemi PN2222A datasheet"),
        PinoutEntry("bc547b-nexperia", "BC547B", "Nexperia BC547B", PinoutCategory.TRANSISTORS, "TO-92 / SOT54", "Front view · pin numbering per Nexperia pinning figure", PinDiagram.TO92,
            dip(listOf("EMITTER","BASE","COLLECTOR")), listOf("Pin numbering/orientation must follow the cited Nexperia datasheet; other BC547 manufacturers may use different drawings.", "Observe collector current, voltage, and dissipation limits."), "https://assets.nexperia.com/documents/data-sheet/BC546_BC547_BC548.pdf", "Nexperia BC546/BC547/BC548 datasheet"),
        PinoutEntry("irfz44npbf", "IRFZ44N", "Infineon IRFZ44NPbF", PinoutCategory.TRANSISTORS, "TO-220AB", "Front view · leads down", PinDiagram.TO220,
            dip(listOf("GATE","DRAIN","SOURCE"),roles=mapOf(2 to PinRole.POWER)), listOf("The tab is connected to DRAIN.", "A logic-level gate drive is not guaranteed by the IRFZ44N datasheet; check RDS(on) at the actual gate voltage.", "Use gate protection and thermal design appropriate to the circuit."), "https://www.infineon.com/assets/row/public/documents/24/49/infineon-irfz44n-datasheet-en.pdf", "Infineon IRFZ44N datasheet"),
        PinoutEntry("usb-a-2-receptacle", "USB Type-A USB 2.0", "Standard 4-contact receptacle", PinoutCategory.CONNECTORS, "Type-A female receptacle · USB 2.0 · 4 contacts", "Mating face view · contacts visible", PinDiagram.USB_A,
            listOf(pin(1,"VBUS","+5 V bus",PinRole.POWER),pin(2,"D−","USB data −"),pin(3,"D+","USB data +"),pin(4,"GND","Ground",PinRole.GROUND)), listOf("This is the standard 4-contact USB 2.0 Type-A receptacle interface, not a USB 3.x 9-contact connector.", "Do not short VBUS to GND; host current and cable ratings apply."), "https://www.usb.org/document-library/usb-20-specification", "USB-IF USB 2.0 Specification")
    )

    fun search(query: String, category: PinoutCategory? = null): List<PinoutEntry> {
        val q = query.trim()
        return entries.filter { (category == null || it.category == category) && (q.isEmpty() || it.name.contains(q, true) || it.partNumber.contains(q, true)) }
    }

    fun integrityErrors(): List<String> = buildList {
        if (entries.map { it.id }.distinct().size != entries.size) add("Duplicate entry IDs")
        entries.forEach { entry ->
            if (entry.pins.size != entry.pinCount) add("Pin count mismatch for ${entry.id}")
            val pinKeys = if (entry.diagram == PinDiagram.BOARD) entry.pins.map { "${it.header}-${it.number}" } else entry.pins.map { it.number }
            if (pinKeys.distinct().size != entry.pins.size) add("Duplicate pin numbers for ${entry.id}")
            if (entry.name.isBlank() || entry.partNumber.isBlank() || entry.datasheetUrl.isBlank() || entry.pins.isEmpty()) add("Incomplete entry ${entry.id}")
            if (entry.diagram == PinDiagram.DIP && entry.pins.map { it.number } != (1..entry.pins.size).map(Int::toString)) add("Non-sequential DIP pin numbering for ${entry.id}")
        }
    }
}
