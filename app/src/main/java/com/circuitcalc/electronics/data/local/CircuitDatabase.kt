package com.circuitcalc.electronics.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SavedCalculation::class], version = 1, exportSchema = false)
abstract class CircuitDatabase : RoomDatabase() { abstract fun savedCalculationDao(): SavedCalculationDao }
