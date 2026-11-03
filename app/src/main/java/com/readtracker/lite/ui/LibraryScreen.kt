package com.readtracker.lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readtracker.lite.data.Calc
import com.readtracker.lite.data.Format
import com.readtracker.lite.data.Settings
import com.readtracker.lite.data.Status
import com.readtracker.lite.data.Title
import kotlinx.coroutines.delay

/**
 * Вкладка «Библиотека» (II-5): вкладки статусов, поиск/фильтр, карточки, кнопка «+»,
 * долгое нажатие — удаление с подтверждением.
 */
@Composable
fun LibraryScreen(
    titles: List<Title>,
    settings: Settings,
    onOpenTitle: (Title) -> Unit,   // тап: экран тайтла или редактор (решает вызывающий)
    onEdit: (Title) -> Unit,
    onDelete: (Title) -> Unit,
    onAdd: () -> Unit,
    onSelectTab: (String) -> Unit,
) {
    val tabKey = settings.lastStatusTab
    var search by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    var formatSheet by remember { mutableStateOf(false) }
    val formats = remember { mutableStateListOf<Format>() }
    var deleteTarget by remember { mutableStateOf<Title?>(null) }

    // задержка ввода поиска 250 мс (II-5)
    LaunchedEffect(search) {
        delay(250)
        debounced = search
    }

    var visible = titles.filter { t -> if (tabKey == "ALL") true else t.status.name == tabKey }
    if (settings.searchEnabled && debounced.isNotBlank()) {
        val q = debounced.trim().lowercase()
        visible = visible.filter { it.name.lowercase().contains(q) }
    }
    if (formats.isNotEmpty()) {
        visible = visible.filter { formats.contains(it.format) }
    }
    if (tabKey == "ALL") {
        visible = Calc.sortedForAllTab(visible, settings.sortAllByStatus)
    }

    Column(Modifier.fillMaxSize().background(ThemeColors.background(settings.theme))) {
        // Вкладки статусов — горизонтальная полоса чипов
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (settings.alignedFilters) 16.dp else 0.dp)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            LazyRowChips(
                tabs = LibraryTabs.TABS,
                selected = tabKey,
                settings = settings,
                onSelect = { key -> onSelectTab(key) },
            )
        }

        if (settings.searchEnabled) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LtrTextField(
                    value = search,
                    onValueChange = { search = it },
                    settings = settings,
                    modifier = Modifier.weight(1f),
                    placeholder = "Поиск по названию...",
                )
                Spacer(Modifier.width(8.dp))
                FilterFormatsButton(formats.isNotEmpty(), settings) { formatSheet = true }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
                FilterFormatsButton(formats.isNotEmpty(), settings) { formatSheet = true }
            }
        }

        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (titles.isEmpty()) "Библиотека пуста" else "Ничего не найдено",
                        color = ThemeColors.grey, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Нажмите «+», чтобы добавить тайтл",
                        color = ThemeColors.grey.copy(alpha = 0.7f), fontSize = 12.sp,
                    )
                }
            }
        } else {
            // Широкие экраны: колонки ~340dp минимум, максимум 4; телефон — одна колонка (II-5)
            val screenWidthDp = LocalConfiguration.current.screenWidthDp
            val columns = when {
                screenWidthDp >= 1360 -> 4
                screenWidthDp >= 1020 -> 3
                screenWidthDp >= 680 -> 2
                else -> 1
            }
            if (columns > 1) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    items(visible, key = { it.id }) { t ->
                        TitleCard(
                            title = t,
                            settings = settings,
                            onClick = { onOpenTitle(t) },
                            onLongClick = { deleteTarget = t },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 100.dp), // запас под кнопку «+»
                ) {
                    items(visible, key = { it.id }) { t ->
                        TitleCard(
                            title = t,
                            settings = settings,
                            onClick = { onOpenTitle(t) },
                            onLongClick = { deleteTarget = t },
                        )
                    }
                }
            }
        }
    }

    // Плавающая акцентная кнопка «+» (скруглённый квадрат, радиус 16)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        AccentPlusButton(
            onClick = onAdd,
            settings = settings,
            size = 56,
        )
        Spacer(Modifier.width(16.dp))
    }

    if (formatSheet) {
        FormatFilterSheet(settings, formats.toList(),
            onApply = { list -> formats.clear(); formats.addAll(list); formatSheet = false },
            onDismiss = { formatSheet = false },
        )
    }

    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = ThemeColors.card(settings.theme),
            title = { Text("Удалить тайтл?", color = ThemeColors.onSurface(settings.theme), fontSize = 16.sp) },
            text = {
                Text(
                    "«${t.name}» будет удалён без возможности восстановления.",
                    color = ThemeColors.grey, fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(t); deleteTarget = null }) {
                    Text("Удалить", color = settings.palette.dropped.toColor(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена", color = ThemeColors.grey) }
            },
        )
    }
}

@Composable
private fun LazyRowChips(
    tabs: List<Pair<String, String>>,
    selected: String,
    settings: Settings,
    onSelect: (String) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(tabs.size) { i ->
            val (key, label) = tabs[i]
            val statusColor = runCatching { Status.valueOf(key) }.getOrNull()
                ?.let { settings.palette.statusColor(it).toColor() }
            FilterChipLite(
                label = label,
                selected = key == selected,
                onClick = { onSelect(key) },
                settings = settings,
                color = if (statusColor != null && key == selected) statusColor else null,
            )
        }
    }
}

@Composable
private fun FilterFormatsButton(active: Boolean, settings: Settings, onClick: () -> Unit) {
    val accent = settings.palette.accent.toColor()
    Box(
        Modifier
            .size(44.dp)
            .background(
                if (active) accent.copy(alpha = 0.14f) else ThemeColors.card(settings.theme),
                androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            )
            .clickableLite(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Search, contentDescription = "Фильтр по формату", tint = if (active) accent else ThemeColors.grey)
    }
}

@Composable
private fun FormatFilterSheet(
    settings: Settings,
    current: List<Format>,
    onApply: (List<Format>) -> Unit,
    onDismiss: () -> Unit,
) {
    val picked = remember { mutableStateListOf<Format>().apply { addAll(current) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThemeColors.card(settings.theme),
        title = { Text("Фильтр по формату", color = ThemeColors.onSurface(settings.theme), fontSize = 16.sp) },
        text = {
            Column {
                Format.entries.forEach { f ->
                    val sel = picked.contains(f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(f.ru, color = if (sel) settings.palette.formatTagColor(f).toColor() else ThemeColors.grey, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        LtrSwitch(sel, { on -> if (on) picked.add(f) else picked.remove(f) }, settings)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(picked.toList()) }) {
                Text("Применить", color = settings.palette.accent.toColor(), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onApply(emptyList()); }) { Text("Сбросить", color = ThemeColors.grey) }
        },
    )
}

private fun Modifier.clickableLite(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick).let { Modifier.then(it) })
