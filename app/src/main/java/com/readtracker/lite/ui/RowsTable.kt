package com.readtracker.lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readtracker.lite.data.Calc
import com.readtracker.lite.data.NumRow
import com.readtracker.lite.data.Settings
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Таблица глав/томов — полноэкранный список (II-4): ленивая прокрутка, фиксированная высота строки,
 * «+» (добавить), «Заполнить диапазон», «Удалить главы диапазона», переход к главе.
 * Используется и для томов серии (без диапазона — у томов добавление по одному/диапазон проще).
 */
@Composable
fun RowsTableScreen(
    titleName: String,
    rowsIn: MutableList<NumRow>,
    itemWord: String,      // "главу" / "том"
    pluralWord: String,    // "Глав" / "Томов"
    sortOnSave: Boolean,   // главы сортируются, тома остаются в порядке ввода
    settings: Settings,
    onApply: (MutableList<NumRow>) -> Unit,
    onClose: () -> Unit,
) {
    val rows = remember { mutableListOf<NumRow>().apply { rowsIn.forEach { add(NumRow(it.num, it.words)) } } }
    if (sortOnSave) rows.sortBy { it.num }
    var showRange by remember { mutableStateOf(false) }
    var showDeleteRange by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val totalWords = rows.sumOf { it.words }

    fun persistAndClose() {
        val out = mutableListOf<NumRow>().apply { rows.forEach { add(NumRow(it.num, it.words)) } }
        if (sortOnSave) out.sortBy { it.num }
        onApply(out)
        onClose()
    }

    Column(Modifier.fillMaxSize().background(ThemeColors.background(settings.theme))) {
        LtrTopBar(titleName.ifEmpty { "Список $itemWord" }, settings, actions = {
            TextButton(onClick = ::persistAndClose) {
                Text("Готово", color = settings.palette.accent.toColor(), fontWeight = FontWeight.Bold)
            }
        })

        // Шапка: «Глав: N» и «Слов: сумма»
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("$pluralWord: ${rows.size}", color = ThemeColors.grey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Слов: ${Calc.groupNumber(totalWords.toLong())}", color = ThemeColors.grey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Переход к главе № (если больше 12)
        if (rows.size > 12) {
            var jump by remember { mutableStateOf("") }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IntField(
                    value = jump,
                    onValueChange = { jump = it },
                    settings = settings,
                    modifier = Modifier.width(120.dp),
                    placeholder = "№",
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val target = jump.toIntOrNull() ?: return@TextButton
                    val idx = rows.indexOfFirst { it.num >= target }
                        .let { if (it >= 0) it else rows.lastIndex }
                    if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                }) {
                    Text("Перейти", color = settings.palette.accent.toColor())
                }
            }
        }

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "${itemWord.replaceFirstChar { it.uppercase() }} пока нет.\n«+» — добавить одну, значок справа — заполнить диапазон.",
                    color = ThemeColors.grey,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                items(rows.size) { i ->
                    val row = rows[i]
                    Row(
                        Modifier
                            .height(60.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeColors.card(settings.theme))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DecimalField(
                            value = Calc.rowNum(row.num),
                            onValueChange = { v ->
                                row.num = v.toDoubleOrNull() ?: row.num
                                if (sortOnSave) {
                                    rows.sortBy { it.num }
                                }
                            },
                            settings = settings,
                            modifier = Modifier.width(84.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        IntField(
                            value = if (row.words == 0) "" else row.words.toString(),
                            onValueChange = { v -> row.words = v.toIntOrNull() ?: 0 },
                            settings = settings,
                            modifier = Modifier.weight(1f),
                            placeholder = "слов",
                        )
                        IconButtonLite({
                            rows.removeAt(i)
                            if (sortOnSave) rows.sortBy { it.num }
                        }, settings) {
                            Icon(Icons.Filled.Remove, contentDescription = "Удалить", tint = settings.palette.dropped.toColor())
                        }
                    }
                }
            }
        }

        message?.let { m ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(settings.palette.dropped.toColor().copy(alpha = 0.14f))
                    .border(1.dp, settings.palette.dropped.toColor().copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(m, color = settings.palette.dropped.toColor(), fontSize = 13.sp)
            }
            LaunchedEffect(m) { kotlinx.coroutines.delay(2500); message = null }
        }

        // Нижняя панель: + / диапазон / удалить диапазон
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentPlusButton(onClick = {
                val next = if (rows.isEmpty()) 1.0 else floor(rows.maxOf { it.num }) + 1.0
                if (rows.any { it.num == next }) {
                    message = "${itemWord.replaceFirstChar { it.uppercase() }} ${Calc.rowNum(next)} уже есть"
                } else {
                    rows.add(NumRow(next, 0))
                    if (sortOnSave) rows.sortBy { it.num }
                    scope.launch { listState.animateScrollToItem(max(0, rows.size - 1)) }
                }
            }, settings = settings)
            OutlinedAccentButton("Диапазон", { showRange = true }, settings)
            OutlinedAccentButton("Удалить диапазон", { showDeleteRange = true }, settings)
        }
    }

    if (showRange) {
        RangeFillDialog(
            rows = rows,
            itemWord = itemWord,
            settings = settings,
            onDone = { msg -> message = msg; showRange = false },
            onCancel = { showRange = false },
        )
    }
    if (showDeleteRange) {
        DeleteRangeDialog(
            rows = rows,
            itemWord = itemWord,
            settings = settings,
            onDone = { msg -> message = msg; showDeleteRange = false },
            onCancel = { showDeleteRange = false },
        )
    }
}

@Composable
private fun IconButtonLite(onClick: () -> Unit, settings: Settings, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Заполнение диапазона (II-4): «Всего слов в диапазоне» / «Слов в каждой главе». */
@Composable
private fun RangeFillDialog(
    rows: MutableList<NumRow>,
    itemWord: String,
    settings: Settings,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var words by remember { mutableStateOf("") }
    var totalMode by remember { mutableStateOf(true) }
    val f = from.toIntOrNull()
    val t = to.toIntOrNull()
    val w = words.toIntOrNull() ?: 0
    val valid = f != null && t != null && f >= 1 && t >= f && (t - f + 1) <= 5000
    val count = if (valid) t!! - f!! + 1 else 0
    val per = if (totalMode && count > 0) w / count else w

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = ThemeColors.card(settings.theme),
        title = { Text("Заполнить диапазон", color = ThemeColors.onSurface(settings.theme), fontSize = 16.sp) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField(from, { from = it }, settings, Modifier.weight(1f), placeholder = "С", maxDigits = 5)
                    IntField(to, { to = it }, settings, Modifier.weight(1f), placeholder = "По", maxDigits = 5)
                }
                Spacer(Modifier.height(8.dp))
                IntField(words, { words = it }, settings, placeholder = "Слов")
                if (totalMode && count > 0 && w > 0) {
                    Text(
                        "По ~$per слов на $itemWord ($count шт.)",
                        color = ThemeColors.grey, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow("Всего слов в диапазоне", totalMode, { totalMode = it }, settings)
                SwitchRow("Слов в каждой $itemWord", !totalMode, { totalMode = !it }, settings)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (!valid) return@TextButton
                    val n = count
                    val base = if (totalMode) w / n else w
                    val rem = if (totalMode) w % n else 0
                    for (k in 0 until n) {
                        val num = (f!! + k).toDouble()
                        val wordsForThis = base + if (totalMode && k < rem) 1 else 0
                        val existing = rows.firstOrNull { it.num == num }
                        if (existing != null) existing.words = wordsForThis
                        else rows.add(NumRow(num, wordsForThis))
                    }
                    rows.sortBy { it.num }
                    onDone("Штук в диапазоне: $n")
                },
            ) { Text("Заполнить", color = settings.palette.accent.toColor()) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Отмена", color = ThemeColors.grey) }
        },
    )
}

/** Удаление диапазона с подтверждением (II-4). */
@Composable
private fun DeleteRangeDialog(
    rows: MutableList<NumRow>,
    itemWord: String,
    settings: Settings,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    val f = from.toIntOrNull()
    val t = to.toIntOrNull()
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = ThemeColors.card(settings.theme),
        title = { Text("Удалить $itemWord?", color = ThemeColors.onSurface(settings.theme), fontSize = 16.sp) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField(from, { from = it }, settings, Modifier.weight(1f), placeholder = "С", maxDigits = 5)
                    IntField(to, { to = it }, settings, Modifier.weight(1f), placeholder = "По", maxDigits = 5)
                }
                if (f != null && t != null && t >= f) {
                    val cnt = rows.count { it.num >= f && it.num <= t }
                    Text(
                        if (cnt == 0) "В этом диапазоне нет" else
                            "Будет удалено: $cnt (с $f по $t).",
                        color = ThemeColors.grey, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = f != null && t != null && t >= f && rows.any { it.num >= f!! && it.num <= t!! },
                onClick = {
                    val doomed = rows.filter { it.num >= f!! && it.num <= t!! }.toSet()
                    rows.removeAll(doomed)
                    onDone("Удалено: ${doomed.size}")
                },
            ) { Text("Удалить", color = settings.palette.dropped.toColor()) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Отмена", color = ThemeColors.grey) }
        },
    )
}
