package com.circuitcalc.electronics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.circuitcalc.electronics.data.PreferencesRepository
import com.circuitcalc.electronics.data.local.CircuitDatabase
import com.circuitcalc.electronics.data.local.SavedCalculation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CircuitCalcViewModel(app: Application) : AndroidViewModel(app) {
    private val database = Room.databaseBuilder(app, CircuitDatabase::class.java, "circuitcalc.db").build()
    private val prefs = PreferencesRepository(app)
    val saved = database.savedCalculationDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = prefs.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val darkMode = prefs.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun save(name: String, inputs: Map<String,String>, result: String) = viewModelScope.launch {
        database.savedCalculationDao().insert(SavedCalculation(calculator=name,inputs=inputs.entries.joinToString("; "){ "${it.key}=${it.value}" },result=result))
    }
    fun delete(id: Long) = viewModelScope.launch { database.savedCalculationDao().delete(id) }
    fun toggleFavorite(name: String) = viewModelScope.launch { prefs.toggleFavorite(name) }
    fun setDark(value: Boolean) = viewModelScope.launch { prefs.setDark(value) }
}
