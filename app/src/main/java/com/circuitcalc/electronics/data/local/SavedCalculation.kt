package com.circuitcalc.electronics.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_calculations")
data class SavedCalculation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calculator: String,
    val inputs: String,
    val result: String,
    val savedAt: Long = System.currentTimeMillis()
)
