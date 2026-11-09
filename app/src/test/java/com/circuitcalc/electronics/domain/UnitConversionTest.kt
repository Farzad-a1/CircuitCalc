package com.circuitcalc.electronics.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionTest {
    @Test fun prefixesConvertToCalculatorBaseUnits() {
        assertEquals(0.02,UnitConversion.toEngineValue(20.0,Field("Current","A"),"mA"),1e-12)
        assertEquals(1500.0,UnitConversion.toEngineValue(1.5,Field("Resistance","Ω"),"kΩ"),1e-12)
        assertEquals(1e-6,UnitConversion.toEngineValue(1.0,Field("Capacitance","F"),"µF"),1e-15)
        assertEquals(0.25,UnitConversion.toEngineValue(250.0,Field("Elapsed time","s"),"ms"),1e-12)
        assertEquals(2000.0,UnitConversion.toEngineValue(2.0,Field("Battery capacity","mAh"),"Ah"),1e-12)
    }
}
