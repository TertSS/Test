package com.readtracker.lite.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readtracker.lite.data.Calc
import com.readtracker.lite.data.Format
import com.readtracker.lite.data.NumRow
import com.readtracker.lite.data.Settings
import com.readtracker.lite.data.Status
import com.readtracker.lite.data.Title
import java.io.File

/**
 * Форма добавления/редактирования тайтла (II-3).
 * Порядок полей: название → автор → описание → обложка → статус → формат → причина дропа →
 * закладка → оценка → жанры → блок расчёта.
 * Мастер добавления: 3 шага «Тайтл», «Статус», «Данные» (или один экран при «Упрощённое добавление»).
 */
@Composable
fun EditorScreen(
    initial: Title,
    isNew: Boolean,
    settings: Settings,
    onSave: (Title) -> Unit,
    onClose: () -> Unit,
) {
    val ctx = LocalContext.current
    var t by remember { mutableStateOf(initial.copy()) }
    var step by remember { mutableStateOf(if (isNew && !settings.simpleAdd) 0 else -1) }
    var showFormatConfirm by remember { mutableStateOf(false) }
    var pendingFormat by remember { mutableStateOf<Format?>(null) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var showVolumeTable by remember { mutableStateOf(false) }
    var showChapterTable by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Копируем файл в приватную папку — выбор файла стирает URL (II-1)
            val path = try {
                val dir = File(ctx.filesDir, "covers").apply { mkdirs() }
                val dst = File(dir, "cover_${System.currentTimeMillis()}.img")
                ctx.contentResolver.openInputStream(uri)?.use { ins -> dst.outputStream().use { ins.copyTo(it) } }
                dst.absolutePath
            } catch (_: Exception) {
                uri.toString()
            }
            t = t.copy(coverPath = path, coverUrl = "")
        }
    }

    fun trySave() {
        val out = finalize(t, settings)
        val errs = Calc.validate(out, settings.totalWordsEnabled)
        if (errs.isNotEmpty()) {
            errors = errs
        } else {
            onSave(out)
        }
    }

    val oneScreen = !isNew || settings.simpleAdd || step == -1

    Column(Modifier.fillMaxSize().background(ThemeColors.background(settings.theme))) {
        LtrTopBar(
            if (isNew) "Новый тайтл" else "Редактирование",
            settings,
            actions = {
                TextButton(onClick = onClose) { Text("Отмена", color = ThemeColors.grey) }
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                if (!oneScreen) {
                    // Индикатор шагов мастера
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Тайтл", "Статус", "Данные").forEachIndexed { i, name ->
                            FilterChipLite(name, i == step, { }, settings)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (oneScreen || step == 0) {
                item {
                    LtrTextField(t.name, { t = t.copy(name = it) }, settings, placeholder = "Название")
                }
                item {
                    LtrTextField(t.author, { t = t.copy(author = it) }, settings, placeholder = "Автор (необязательно)")
                }
                item {
                    LtrTextField(
                        t.description, { t = t.copy(description = it) }, settings,
                        placeholder = "Описание (необязательно)", singleLine = false, minLines = 3,
                    )
                }
                item {
                    CoverField(t, settings, onPickFile = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                        onPickUrl = { url -> t = t.copy(coverUrl = url, coverPath = "") },
                        onClear = { t = t.copy(coverUrl = "", coverPath = "") },
                        onColor = { c -> t = t.copy(coverColor = c) })
                }
            }

            if (oneScreen || step == 1) {
                item {
                    SectionLabel("Статус", settings)
                    FlowChips(Status.entries.map { it.name to it.ru }, t.status.name, { sel ->
                        t = t.copy(status = Status.from(sel))
                    }, settings, colors = { key -> settings.palette.statusColor(Status.from(key)).toColor() })
                }
                item {
                    SectionLabel("Формат", settings)
                    FlowChips(Format.entries.map { it.name to it.ru }, t.format.name, { sel ->
                        val nf = Format.from(sel)
                        if (nf != t.format) {
                            val hasData = t.words != 0 || t.volumes != 0 || t.chaptersRead != 0 ||
                                t.volumeRows.isNotEmpty() || t.chapterRows.isNotEmpty()
                            if (hasData) {
                                pendingFormat = nf
                                showFormatConfirm = true
                            } else {
                                t = applyFormat(t, nf)
                            }
                        }
                    }, settings, colors = { key -> settings.palette.formatTagColor(Format.from(key)).toColor() })
                }
                if (t.status == Status.DROPPED) {
                    item {
                        LtrTextField(
                            t.dropReason, { t = t.copy(dropReason = it) }, settings,
                            placeholder = "Почему брошено",
                        )
                    }
                }
            }

            if (oneScreen || step == 2) {
                if (settings.bookmarksEnabled) {
                    item {
                        LtrTextField(
                            t.bookmark, { t = t.copy(bookmark = it) }, settings,
                            placeholder = "Закладка. Пример: 1.4 глава, 2х3.1 арка...",
                        )
                    }
                }
                if (settings.ratingEnabled) {
                    item { RatingEditor(t.rating, settings, { r -> t = t.copy(rating = r) }) }
                }
                if (settings.genresEnabled) {
                    item {
                        SectionLabel("Жанры", settings)
                        GenreEditor(
                            selected = t.genres.toList(),
                            known = settings.knownGenres,
                            onChange = { list -> t = t.copy(genres = list.toMutableList()) },
                            settings = settings,
                        )
                    }
                }
                item {
                    CalculationBlock(
                        t = t,
                        settings = settings,
                        onChange = { t = it },
                        openVolumeTable = { showVolumeTable = true },
                        openChapterTable = { showChapterTable = true },
                    )
                }
            }
        }

        // Ошибки сохранения — красная плашка внизу (цвет «Брошено»)
        if (errors.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(settings.palette.dropped.toColor().copy(alpha = 0.14f))
                    .border(1.dp, settings.palette.dropped.toColor().copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                errors.forEach { e ->
                    Text(e, color = settings.palette.dropped.toColor(), fontSize = 13.sp)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!oneScreen && step < 2) {
                OutlinedAccentButton("Далее", { step += 1; errors = emptyList() }, settings, filled = true)
            } else {
                OutlinedAccentButton("Сохранить", ::trySave, settings, filled = true)
            }
        }
    }

    if (showFormatConfirm && pendingFormat != null) {
        AlertDialog(
            onDismissRequest = { showFormatConfirm = false },
            containerColor = ThemeColors.card(settings.theme),
            title = { Text("Сменить формат издания?", color = ThemeColors.onSurface(settings.theme)) },
            text = { Text("Введённые данные могут быть потеряны.", color = ThemeColors.grey, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    t = applyFormat(t, pendingFormat!!)
                    showFormatConfirm = false
                }) { Text("Сменить", color = settings.palette.accent.toColor()) }
            },
            dismissButton = {
                TextButton(onClick = { showFormatConfirm = false }) { Text("Отмена", color = ThemeColors.grey) }
            },
        )
    }

    if (showVolumeTable) {
        RowsTableScreen(
            titleName = t.name,
            rowsIn = t.volumeRows,
            itemWord = "том",
            pluralWord = "Томов",
            sortOnSave = false, // тома остаются в введённом порядке (II-3)
            settings = settings,
            onApply = { rows -> t = t.copy(volumeRows = rows) },
            onClose = { showVolumeTable = false },
        )
    }
    if (showChapterTable) {
        RowsTableScreen(
            titleName = t.name,
            rowsIn = t.chapterRows,
            itemWord = "главу",
            pluralWord = "Глав",
            sortOnSave = true, // строки глав сортируются по номеру
            settings = settings,
            onApply = { rows -> t = t.copy(chapterRows = rows) },
            onClose = { showChapterTable = false },
        )
    }
}

/** Смена формата: Серия/Одиночка включают подсчёт томов, Веб — выключает (II-3). */
private fun applyFormat(t: Title, nf: Format): Title {
    val copy = t.copy(format = nf)
    return when (nf) {
        Format.WEB -> copy.copy(byVolumesMode = false)
        else -> copy.copy(byChaptersMode = false)
    }
}

/** Приведение к правилам сохранения (II-3 «Сохранение»). */
private fun finalize(t: Title, settings: Settings): Title {
    val out = t.copy()
    if (out.format.hasVolumes && out.byVolumesMode) {
        out.words = out.volumeRows.sumOf { it.words }
        out.volumes = out.volumeRows.size
    }
    if (out.format == Format.WEB && out.byChaptersMode) {
        out.words = out.chapterRows.sumOf { it.words }
        out.chapterRows.sortBy { it.num }
    }
    if (!out.byVolumesMode) out.volumeRows = mutableListOf()
    if (!out.byChaptersMode) out.chapterRows = mutableListOf()
    if (out.ongoing) out.volumesTotal = null // при онгоинге «всего томов» не сохраняется
    if (!settings.totalWordsEnabled) out.totalWordsInTitle = null
    if (out.chaptersTotal == 0) out.chaptersTotal = null
    if (out.volumesTotal == 0) out.volumesTotal = null
    if (out.totalWordsInTitle == 0) out.totalWordsInTitle = null
    if (out.rating != null && out.rating!! <= 0) out.rating = null
    return out
}

/** Блок расчёта (II-3): режимы «по томам/главам» и «просто итог». */
@Composable
private fun CalculationBlock(
    t: Title,
    settings: Settings,
    onChange: (Title) -> Unit,
    openVolumeTable: () -> Unit,
    openChapterTable: () -> Unit,
) {
    CardBlock(settings) {
        SectionLabel("Расчёт", settings)

        if (t.format.hasVolumes) {
            SwitchRow(
                "Расчёт по томам",
                t.byVolumesMode,
                { on ->
                    val nt = t.copy()
                    if (on) {
                        if (nt.volumeRows.isEmpty()) nt.volumeRows.add(NumRow(1.0, 0))
                        nt.byVolumesMode = true
                    } else {
                        // перенос в суммарные поля: слова = сумма, тома = число строк
                        nt.words = nt.volumeRows.sumOf { it.words }
                        nt.volumes = nt.volumeRows.size
                        nt.volumeRows = mutableListOf()
                        nt.byVolumesMode = false
                    }
                    onChange(nt)
                },
                settings,
                subtext = if (t.byVolumesMode) "Слова по каждому тому" else "Ввести суммарно",
            )
            if (!t.byVolumesMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField(t.words.toString().ifZeroEmpty(), { v -> onChange(t.copy(words = v.toIntOrNull() ?: 0)) }, settings, Modifier.weight(1f), label = "Слов")
                    IntField(t.volumes.toString().ifZeroEmpty(), { v -> onChange(t.copy(volumes = v.toIntOrNull() ?: 0)) }, settings, Modifier.weight(1f), label = "Томов")
                }
            } else {
                OutlinedAccentButton("Открыть список томов", openVolumeTable, settings, modifier = Modifier.padding(top = 6.dp))
                Text(
                    "Томов: ${t.volumeRows.size} · Слов: ${Calc.groupNumber(t.volumeRows.sumOf { it.words }.toLong())}",
                    color = ThemeColors.grey, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (t.format == Format.WEB) {
            SectionLabel("Прогресс глав", settings)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField(
                    t.chaptersRead.toString().ifZeroEmpty(),
                    { v -> onChange(t.copy(chaptersRead = v.toIntOrNull() ?: 0)) },
                    settings, Modifier.weight(1f), label = "Прочитано",
                )
                IntField(
                    (t.chaptersTotal ?: 0).toString().ifZeroEmpty(),
                    { v -> onChange(t.copy(chaptersTotal = v.toIntOrNull()?.takeIf { it > 0 })) },
                    settings, Modifier.weight(1f), label = "Всего (необз.)",
                )
            }
            SwitchRow(
                "Расчёт по главам",
                t.byChaptersMode,
                { on ->
                    val nt = t.copy()
                    if (on) {
                        nt.byChaptersMode = true
                    } else {
                        nt.words = nt.chapterRows.sumOf { it.words }
                        nt.chapterRows = mutableListOf()
                        nt.byChaptersMode = false
                    }
                    onChange(nt)
                },
                settings,
                subtext = if (t.byChaptersMode) "Слова по каждой главе" else "Ввести суммарно",
            )
            if (!t.byChaptersMode) {
                IntField(
                    t.words.toString().ifZeroEmpty(),
                    { v -> onChange(t.copy(words = v.toIntOrNull() ?: 0)) },
                    settings, label = "Слов",
                )
            } else {
                Text(
                    "Глав: ${t.chapterRows.size} · Слов: ${Calc.groupNumber(t.chapterRows.sumOf { it.words }.toLong())}",
                    color = ThemeColors.grey, fontSize = 12.sp,
                )
                OutlinedAccentButton("Открыть список глав", openChapterTable, settings, modifier = Modifier.padding(top = 6.dp))
            }
        }

        if (settings.totalWordsEnabled) {
            Spacer(Modifier.height(8.dp))
            IntField(
                (t.totalWordsInTitle ?: 0).toString().ifZeroEmpty(),
                { v -> onChange(t.copy(totalWordsInTitle = v.toIntOrNull()?.takeIf { it > 0 })) },
                settings, label = "Всего слов в тайтле", placeholder = "Например: 1 200 000", maxDigits = 9,
            )
        }

        if (t.format.hasVolumes) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Всего томов в серии", settings)
            SwitchRow(
                "Онгоинг",
                t.ongoing,
                { on -> onChange(t.copy(ongoing = on, volumesTotal = if (on) null else t.volumesTotal)) },
                settings,
                subtext = if (t.ongoing) "Отображается как 5/?" else "Кол-во томов известно",
            )
            if (!t.ongoing) {
                IntField(
                    (t.volumesTotal ?: 0).toString().ifZeroEmpty(),
                    { v -> onChange(t.copy(volumesTotal = v.toIntOrNull()?.takeIf { it > 0 })) },
                    settings, label = "Необязательно — напр. 25",
                )
            }
        }
    }
}

private fun String.ifZeroEmpty(): String = if (this == "0") "" else this

/** Оценка: выбор по шкале 5 или 10, кнопка «Сбросить» (II-2). */
@Composable
private fun RatingEditor(current: Int?, settings: Settings, onChange: (Int?) -> Unit) {
    CardBlock(settings) {
        SectionLabel("Оценка", settings)
        val scale = settings.ratingScale
        val steps = if (scale == 5) listOf(2, 4, 6, 8, 10) else (1..10).toList()
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            steps.forEach { s ->
                val selected = current == s
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) settings.palette.accent.toColor().copy(alpha = 0.14f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) settings.palette.accent.toColor().copy(alpha = 0.3f) else ThemeColors.divider(settings.theme),
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { onChange(s) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (scale == 5) "${s / 2}" else "$s",
                        color = if (selected) settings.palette.accent.toColor() else ThemeColors.grey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                Calc.ratingLabel(current, scale).ifEmpty { "Не задана" },
                color = settings.palette.accent.toColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = { onChange(null) }) { Text("Сбросить", color = ThemeColors.grey, fontSize = 12.sp) }
        }
    }
}

/** Обложка: превью + URL / файл / цвет-заглушка. Выбор файла стирает URL, ввод URL стирает файл. */
@Composable
private fun CoverField(
    t: Title,
    settings: Settings,
    onPickFile: () -> Unit,
    onPickUrl: (String) -> Unit,
    onClear: () -> Unit,
    onColor: (Long) -> Unit,
) {
    CardBlock(settings) {
        SectionLabel("Обложка", settings)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverBox(t, w = 44, h = 62)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                LtrTextField(
                    t.coverUrl,
                    { v -> onPickUrl(v) },
                    settings,
                    placeholder = "Ссылка на обложку (URL)",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    OutlinedAccentButton("Файл", onPickFile, settings)
                    OutlinedAccentButton("Очистить", onClear, settings)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(0xFF3F51B5L, 0xFF00897BL, 0xFF6A1B9AL, 0xFFD81B60L, 0xFF455A64L).forEach { c ->
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.toColor())
                        .border(
                            if (t.coverColor == c) 2.dp else 0.dp,
                            settings.palette.accent.toColor(),
                            RoundedCornerShape(6.dp),
                        )
                        .clickable { onColor(c) },
                )
            }
        }
    }
}

/** Редактор жанров (II-10): выбранные чипы с ×, поле + кнопка «+», подсказки из известных. */
@Composable
fun GenreEditor(
    selected: List<String>,
    known: List<String>,
    onChange: (List<String>) -> Unit,
    settings: Settings,
) {
    var input by remember { mutableStateOf("") }

    fun add(raw: String) {
        val g = raw.trim()
        if (g.isEmpty()) return
        if (selected.none { it.equals(g, ignoreCase = true) }) {
            onChange(selected + g)
        }
        input = ""
    }

    CardBlock(settings) {
        if (selected.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selected.forEach { g ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(settings.palette.accent.toColor().copy(alpha = 0.14f))
                            .border(1.dp, settings.palette.accent.toColor().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { onChange(selected - g) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("$g  ✕", color = settings.palette.accent.toColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val genreInputModifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyUp && ev.key == Key.Enter) {
                        add(input); true
                    } else false
                }
            LtrTextField(
                value = input,
                onValueChange = { input = it },
                settings = settings,
                modifier = genreInputModifier,
                placeholder = "Например: Фэнтези, Романтика...",
            )
            Spacer(Modifier.width(8.dp))
            AccentPlusButton({ add(input) }, settings, size = 40)
        }
        val suggestions = known.filter { k -> selected.none { it.equals(k, ignoreCase = true) } }
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.take(24).forEach { g ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeColors.card(settings.theme))
                            .border(1.dp, ThemeColors.divider(settings.theme), RoundedCornerShape(8.dp))
                            .clickable { add(g) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("+ $g", color = ThemeColors.grey, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Полоса чипов-мультивыбора (статусы/форматы). */
@Composable
fun FlowChips(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    settings: Settings,
    colors: (String) -> androidx.compose.ui.graphics.Color,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            val sel = key == selectedKey
            val c = colors(key)
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (sel) c.copy(alpha = 0.14f) else ThemeColors.card(settings.theme))
                    .border(1.dp, if (sel) c.copy(alpha = 0.3f) else ThemeColors.divider(settings.theme), RoundedCornerShape(20.dp))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(label, color = if (sel) c else ThemeColors.grey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
