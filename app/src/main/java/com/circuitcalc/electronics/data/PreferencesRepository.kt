package com.circuitcalc.electronics.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore("circuitcalc_preferences")
class PreferencesRepository(private val context: Context) {
    private val darkKey = booleanPreferencesKey("dark_mode")
    private val favoritesKey = stringSetPreferencesKey("favorites")
    val darkMode = context.preferences.data.map { it[darkKey] ?: false }
    val favorites = context.preferences.data.map { it[favoritesKey] ?: emptySet() }
    suspend fun setDark(value: Boolean) { context.preferences.edit { it[darkKey] = value } }
    suspend fun toggleFavorite(name: String) { context.preferences.edit { val next=(it[favoritesKey] ?: emptySet()).toMutableSet(); if(!next.add(name))next.remove(name);it[favoritesKey]=next } }
}
