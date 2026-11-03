package com.readtracker.lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readtracker.lite.data.AppThemeMode
import com.readtracker.lite.data.Settings

/** Поле ввода: заливка цветом карточки, без рамки; в фокусе рамка 1.5dp акцента (часть I). */
@Composable
fun LtrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    settings: Settings,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val cardBg = ThemeColors.card(settings.theme)
    val accent = settings.palette.accent.toColor()
    val onSurf = ThemeColors.onSurface(settings.theme)
    val shape = RoundedCornerShape(8.dp)
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        singleLine = singleLine,
        minLines = minLines,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = onSurf,
            fontSize = 14.sp,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        placeholder = {
            Text(placeholder, color = ThemeColors.hint(settings.theme), fontSize = 14.sp)
        },
        label = if (label != null) {
            { Text(label, color = ThemeColors.grey, fontSize = 12.sp) }
        } else null,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = cardBg,
            unfocusedContainerColor = cardBg,
            disabledContainerColor = cardBg,
            errorContainerColor = cardBg,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = accent,
            focusedLabelColor = accent,
            selectionColors = TextSelectionColors(
                handleColor = accent,
                backgroundColor = accent.copy(alpha = 0.3f),
            ),
        ),
        shape = shape,
    )
}

/** Числовое поле: только цифры, пустое = 0. */
@Composable
fun IntField(
    value: String,
    onValueChange: (String) -> Unit,
    settings: Settings,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "0",
    maxDigits: Int = 9,
) {
    LtrTextField(
        value = value.filter { it.isDigit() }.take(maxDigits),
        onValueChange = onValueChange,
        settings = settings,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        keyboardType = KeyboardType.Number,
    )
}

/** Поле номера тома/главы: цифры и точка (дробные 1.5 допустимы). */
@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    settings: Settings,
    modifier: Modifier = Modifier,
    placeholder: String = "1",
) {
    LtrTextField(
        value = value.filter { it.isDigit() || it == '.' }.let { v ->
            // не более одной точки
            val i = v.indexOf('.')
            if (i >= 0 && v.lastIndexOf('.') != i) v.substring(0, v.lastIndexOf('.')) else v
        },
        onValueChange = onValueChange,
        settings = settings,
        modifier = modifier,
        placeholder = placeholder,
        keyboardType = KeyboardType.Decimal,
    )
}

/** Переключатель: белый бегунок, трек — акцент во включённом состоянии (часть I). */
@Composable
fun LtrSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, settings: Settings) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = settings.palette.accent.toColor(),
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = ThemeColors.switchOffTrack(settings.theme),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Строка настройки с переключателем. */
@Composable
fun SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    settings: Settings,
    subtext: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text,
                color = ThemeColors.onSurface(settings.theme),
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
            )
            if (subtext != null) {
                Text(subtext, color = ThemeColors.grey, fontSize = 11.sp)
            }
        }
        LtrSwitch(checked, onCheckedChange, settings)
    }
}

/** Бейдж-чип цвета (статус/формат): фон 14% + рамка 30%. */
@Composable
fun StatusChip(text: String, color: Color, backgroundAlpha: Float = 0.14f, borderAlpha: Float = 0.30f) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = backgroundAlpha))
            .border(1.dp, color.copy(alpha = borderAlpha), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Фильтр-чип для вкладок статусов / выбора. */
@Composable
fun FilterChipLite(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    settings: Settings,
    color: Color? = null,
) {
    val accent = settings.palette.accent.toColor()
    val fg = color ?: if (selected) accent else ThemeColors.grey
    val bg = if (selected) accent.copy(alpha = 0.13f) else ThemeColors.card(settings.theme)
    Box(
        Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.3f) else ThemeColors.divider(settings.theme),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Квадратная акцентная кнопка «+» (радиус 16). */
@Composable
fun AccentPlusButton(onClick: () -> Unit, settings: Settings, size: Int = 40) {
    val accent = settings.palette.accent.toColor()
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Add, contentDescription = "Добавить", tint = Color.Black)
    }
}

/** Кнопка-контур акцентного цвета («Редактировать», «Сохранить»). */
@Composable
fun OutlinedAccentButton(
    text: String,
    onClick: () -> Unit,
    settings: Settings,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val accent = settings.palette.accent.toColor()
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) accent else Color.Transparent)
            .border(1.5.dp, accent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (filled) (if (settings.theme == AppThemeMode.LIGHT) Color.Black else Color.Black) else accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Компактная шапка ≈48dp, заголовок жирный. */
@Composable
fun LtrTopBar(
    titleText: String,
    settings: Settings,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titleText,
            color = ThemeColors.onSurface(settings.theme),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = if (settings.centeredHeaders) Modifier.weight(1f) else Modifier.weight(1f),
            textAlign = if (settings.centeredHeaders) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
        )
        actions()
    }
}

/** Заголовок раздела настроек/блока — подпись капсом мелким серым. */
@Composable
fun SectionLabel(text: String, settings: Settings) {
    Text(
        text.uppercase(),
        color = ThemeColors.grey,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

/** Карточка-контейнер блока (фон карточки, радиус 12). */
@Composable
fun CardBlock(
    settings: Settings,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ThemeColors.card(settings.theme))
            .padding(12.dp),
        content = content,
    )
}

/** Крестик очистки поля поиска. */
@Composable
fun ClearIcon(onClick: () -> Unit, settings: Settings) {
    Icon(
        AppIcons.Close,
        contentDescription = "Очистить",
        tint = ThemeColors.grey,
        modifier = Modifier.size(18.dp).clickable(onClick = onClick),
    )
}
