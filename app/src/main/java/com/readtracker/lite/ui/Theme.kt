package com.readtracker.lite.ui

import androidx.compose.ui.graphics.Color

/** Цветовые утилиты: прозрачность чипов/бейджей из палитры (часть I). */

fun Long.toColor(): Color = Color(this)

/** Акцент с прозрачностью 12–14% для фонов чипов. */
fun Long.withAlpha(alpha: Float): Color {
    val c = Color(this)
    return c.copy(alpha = alpha)
}

object ThemeColors {
    fun background(mode: com.readtracker.lite.data.AppThemeMode): Color = when (mode) {
        com.readtracker.lite.data.AppThemeMode.AMOLED -> Color(0xFF000000)
        com.readtracker.lite.data.AppThemeMode.DARK -> Color(0xFF0F0F0F)
        com.readtracker.lite.data.AppThemeMode.LIGHT -> Color(0xFFF2F2F7)
    }

    fun card(mode: com.readtracker.lite.data.AppThemeMode): Color = when (mode) {
        com.readtracker.lite.data.AppThemeMode.AMOLED -> Color(0xFF141414)
        com.readtracker.lite.data.AppThemeMode.DARK -> Color(0xFF1C1C1E)
        com.readtracker.lite.data.AppThemeMode.LIGHT -> Color(0xFFFFFFFF)
    }

    fun onSurface(mode: com.readtracker.lite.data.AppThemeMode): Color = when (mode) {
        com.readtracker.lite.data.AppThemeMode.LIGHT -> Color.Black
        else -> Color.White
    }

    /** Разделители: белый 10% (тёмные) / чёрный 12% (светлая). */
    fun divider(mode: com.readtracker.lite.data.AppThemeMode): Color = when (mode) {
        com.readtracker.lite.data.AppThemeMode.LIGHT -> Color.Black.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.10f)
    }

    /** Подсказки в полях: белый/чёрный 38%. */
    fun hint(mode: com.readtracker.lite.data.AppThemeMode): Color = when (mode) {
        com.readtracker.lite.data.AppThemeMode.LIGHT -> Color.Black.copy(alpha = 0.38f)
        else -> Color.White.copy(alpha = 0.38f)
    }

    /** Трек выключенного переключателя: белый/чёрный 24–26%. */
    fun switchOffTrack(mode: com.readtracker.lite.data.AppThemeMode): Color = when (mode) {
        com.readtracker.lite.data.AppThemeMode.LIGHT -> Color.Black.copy(alpha = 0.26f)
        else -> Color.White.copy(alpha = 0.24f)
    }

    val grey = Color(0xFF9E9E9E)
}

/** Корневая тема: фон + статус-бар в зависимости от режима (часть I). */
@androidx.compose.runtime.Composable
fun AppTheme(mode: com.readtracker.lite.data.AppThemeMode, content: @androidx.compose.runtime.Composable () -> Unit) {
    val bg = ThemeColors.background(mode)
    androidx.compose.material3.Surface(color = bg, content = content)
}
