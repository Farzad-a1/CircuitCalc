package com.circuitcalc.electronics.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCalculationDao {
    @Query("SELECT * FROM saved_calculations ORDER BY savedAt DESC") fun observeAll(): Flow<List<SavedCalculation>>
    @Insert suspend fun insert(item: SavedCalculation)
    @Query("DELETE FROM saved_calculations WHERE id = :id") suspend fun delete(id: Long)
}
