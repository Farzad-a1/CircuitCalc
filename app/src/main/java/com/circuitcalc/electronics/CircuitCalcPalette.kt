package com.circuitcalc.electronics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class CircuitCalcPalette(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val onSurface: Color,
    val secondary: Color,
    val iconContainer: Color,
    val outline: Color,
    val bottomNavigation: Color,
    val ground: Color,
    val signal: Color,
    val diagramInset: Color
)

internal val CircuitCalcLightPalette = CircuitCalcPalette(
    background = Color(0xFFFAFAF8),
    surface = Color.White,
    primary = Color(0xFFE86A17),
    onSurface = Color(0xFF252525),
    secondary = Color(0xFF737780),
    iconContainer = Color(0xFFFFF1E6),
    outline = Color(0xFFECEAE7),
    bottomNavigation = Color.White,
    ground = Color(0xFF46515C),
    signal = Color(0xFFBAC0C6),
    diagramInset = Color(0xFFF8F7F4)
)

internal val CircuitCalcDarkPalette = CircuitCalcPalette(
    background = Color(0xFF121212),
    surface = Color(0xFF242426),
    primary = Color(0xFFFF8A3D),
    onSurface = Color(0xFFF5F5F5),
    secondary = Color(0xFFB5B5B8),
    iconContainer = Color(0xFF38291F),
    outline = Color(0xFF424246),
    bottomNavigation = Color(0xFF1B1B1D),
    ground = Color(0xFF8FD6A8),
    signal = Color(0xFFD0D0D3),
    diagramInset = Color(0xFF1B1B1D)
)

internal val LocalCircuitCalcPalette = staticCompositionLocalOf { CircuitCalcLightPalette }

internal val ScreenBackground: Color @Composable get() = LocalCircuitCalcPalette.current.background
internal val CardWhite: Color @Composable get() = LocalCircuitCalcPalette.current.surface
internal val Orange: Color @Composable get() = LocalCircuitCalcPalette.current.primary
internal val PaleOrange: Color @Composable get() = LocalCircuitCalcPalette.current.iconContainer
internal val Ink: Color @Composable get() = LocalCircuitCalcPalette.current.onSurface
internal val Muted: Color @Composable get() = LocalCircuitCalcPalette.current.secondary
internal val SubtleBorder: Color @Composable get() = LocalCircuitCalcPalette.current.outline

internal val PinOrange: Color @Composable get() = LocalCircuitCalcPalette.current.primary
internal val PinPaleOrange: Color @Composable get() = LocalCircuitCalcPalette.current.iconContainer
internal val PinInk: Color @Composable get() = LocalCircuitCalcPalette.current.onSurface
internal val PinMuted: Color @Composable get() = LocalCircuitCalcPalette.current.secondary
internal val PinLine: Color @Composable get() = LocalCircuitCalcPalette.current.outline
internal val PinGround: Color @Composable get() = LocalCircuitCalcPalette.current.ground
internal val PinSignal: Color @Composable get() = LocalCircuitCalcPalette.current.signal
internal val PinDiagramInset: Color @Composable get() = LocalCircuitCalcPalette.current.diagramInset

internal val ConverterOrange: Color @Composable get() = LocalCircuitCalcPalette.current.primary
internal val ConverterPale: Color @Composable get() = LocalCircuitCalcPalette.current.iconContainer
internal val ConverterInk: Color @Composable get() = LocalCircuitCalcPalette.current.onSurface
internal val ConverterMuted: Color @Composable get() = LocalCircuitCalcPalette.current.secondary
internal val ConverterBorder: Color @Composable get() = LocalCircuitCalcPalette.current.outline
