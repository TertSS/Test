package com.readtracker.lite.data

/**
 * Палитра (часть I) и её настраиваемые цвета (II-11 «Персонализация»).
 * Значения ARGB в Long. Хранятся в Settings отдельно от библиотеки.
 */
data class Palette(
    val accent: Long = 0xFFFF9F0AL,
    val planned: Long = 0xFF60A5FAL,
    val reading: Long = 0xFF34D399L,
    val paused: Long = 0xFFFBBF24L,
    val completed: Long = 0xFFA78BFAL,
    val dropped: Long = 0xFFF87171L,
    val tagSeries: Long = 0xFFA78BFAL,
    val tagWeb: Long = 0xFFFBBF24L,
    val tagSingle: Long = 0xFFFF9F0AL,
    val tagOngoing: Long = 0xFF34D399L,
) {
    fun statusColor(s: Status): Long = when (s) {
        Status.PLANNED -> planned
        Status.READING -> reading
        Status.PAUSED -> paused
        Status.COMPLETED -> completed
        Status.DROPPED -> dropped
    }

    fun formatTagColor(f: Format): Long = when (f) {
        Format.SERIES -> tagSeries
        Format.WEB -> tagWeb
        Format.SINGLE -> tagSingle
    }

    /** Цвет по ключу настройки (для колор-пикера). */
    fun get(key: String): Long = when (key) {
        "accent" -> accent
        "planned" -> planned
        "reading" -> reading
        "paused" -> paused
        "completed" -> completed
        "dropped" -> dropped
        "tagSeries" -> tagSeries
        "tagWeb" -> tagWeb
        "tagSingle" -> tagSingle
        "tagOngoing" -> tagOngoing
        else -> accent
    }

    fun with(key: String, value: Long): Palette = when (key) {
        "accent" -> copy(accent = value)
        "planned" -> copy(planned = value)
        "reading" -> copy(reading = value)
        "paused" -> copy(paused = value)
        "completed" -> copy(completed = value)
        "dropped" -> copy(dropped = value)
        "tagSeries" -> copy(tagSeries = value)
        "tagWeb" -> copy(tagWeb = value)
        "tagSingle" -> copy(tagSingle = value)
        "tagOngoing" -> copy(tagOngoing = value)
        else -> this
    }

    companion object {
        val DEFAULT = Palette()
        val COLOR_KEYS = listOf(
            "accent" to "Акцент",
            "planned" to "В планах",
            "reading" to "Читаю",
            "paused" to "На паузе",
            "completed" to "Завершено",
            "dropped" to "Брошено",
            "tagSeries" to "Тег: Серия",
            "tagWeb" to "Тег: Веб",
            "tagSingle" to "Тег: Сингл",
            "tagOngoing" to "Тег: Онгоинг",
        )
    }
}

/** Три темы (часть I). */
enum class AppThemeMode(val ru: String) { DARK("Тёмная"), AMOLED("AMOLED"), LIGHT("Светлая") }

/** Варианты вида карточек (II-6). */
enum class CardStyle(val ru: String, val desc: String) {
    NUMBERS("Цифры", "Крупное число справа без подложки"),
    TILE("Цифры · плитка", "Число и подписи в плитке акцентного цвета"),
    BAR("Цифры · полоса", "Как «Цифры» плюс полоса прогресса снизу"),
}

/** Расположение закладки в карточке. */
enum class BookmarkPos { INLINE, BOTTOM }

/**
 * Настройки приложения (II-11). Значения по умолчанию — как в оригинале.
 * Хранятся отдельно от библиотеки, сохраняются при каждом изменении.
 */
data class Settings(
    val theme: AppThemeMode = AppThemeMode.DARK,
    val palette: Palette = Palette.DEFAULT,

    // Вид карточек
    val cardStyle: CardStyle = CardStyle.NUMBERS,
    val singleLineName: Boolean = false,

    // Интерфейс
    val centeredHeaders: Boolean = false,
    val alignedFilters: Boolean = true,
    val cardChevron: Boolean = true,
    val statusTextInList: Boolean = true,
    val dropReasonInCards: Boolean = true,
    val hideOngoingTag: Boolean = false,
    val abbreviateNumbers: Boolean = false,
    val disableAnimations: Boolean = false,
    val fabGlow: Boolean = false,
    val hideBottomBar: Boolean = true,

    // Функции
    val titleScreenEnabled: Boolean = false,
    val titleHeaderOnScreen: Boolean = true,
    val progressBlockOnTitleScreen: Boolean = false,
    val simpleAdd: Boolean = false,
    val goalsEnabled: Boolean = false,
    val bookmarksEnabled: Boolean = true,
    val bookmarkPosition: BookmarkPos = BookmarkPos.INLINE,
    val totalWordsEnabled: Boolean = false,
    val ratingEnabled: Boolean = true,
    val ratingScale: Int = 10,
    val searchEnabled: Boolean = false,
    val genresEnabled: Boolean = false,
    val onlyChartGenres: Boolean = false,

    // Аналитика
    val wideMetricCards: Boolean = false,
    val webChaptersOnCards: Boolean = true,
    val sortAllByStatus: Boolean = true,
    /** Включённые разделы аналитики в порядке показа (II-8). */
    val analyticsSections: List<String> = listOf("top_words", "words_by_status"),

    // Цели (пустые/0 — цели нет)
    val goalWords: Long = 0,
    val goalVolumes: Long = 0,
    val goalWebCompleted: Long = 0,
    val goalSeriesCompleted: Long = 0,

    // Пользовательский словарь жанров (не уменьшается при удалении тайтлов, II-10)
    val knownGenres: List<String> = emptyList(),

    // Порядок лепестков радарной диаграммы жанров (II-8)
    val genreChartOrder: List<String> = emptyList(),

    // Сворачиваемые разделы настроек (по умолчанию выкл)
    val collapsibleSections: Boolean = false,

    // Запомненная вкладка статусов библиотеки (II-5)
    val lastStatusTab: String = Status.PLANNED.name,
) {
    fun toJson(): org.json.JSONObject = org.json.JSONObject().apply {
        put("theme", theme.name)
        put("palette", org.json.JSONObject().apply {
            put("accent", palette.accent.toString())
            put("planned", palette.planned.toString())
            put("reading", palette.reading.toString())
            put("paused", palette.paused.toString())
            put("completed", palette.completed.toString())
            put("dropped", palette.dropped.toString())
            put("tagSeries", palette.tagSeries.toString())
            put("tagWeb", palette.tagWeb.toString())
            put("tagSingle", palette.tagSingle.toString())
            put("tagOngoing", palette.tagOngoing.toString())
        })
        put("cardStyle", cardStyle.name)
        put("singleLineName", singleLineName)
        put("centeredHeaders", centeredHeaders)
        put("alignedFilters", alignedFilters)
        put("cardChevron", cardChevron)
        put("statusTextInList", statusTextInList)
        put("dropReasonInCards", dropReasonInCards)
        put("hideOngoingTag", hideOngoingTag)
        put("abbreviateNumbers", abbreviateNumbers)
        put("disableAnimations", disableAnimations)
        put("fabGlow", fabGlow)
        put("hideBottomBar", hideBottomBar)
        put("titleScreenEnabled", titleScreenEnabled)
        put("titleHeaderOnScreen", titleHeaderOnScreen)
        put("progressBlockOnTitleScreen", progressBlockOnTitleScreen)
        put("simpleAdd", simpleAdd)
        put("goalsEnabled", goalsEnabled)
        put("bookmarksEnabled", bookmarksEnabled)
        put("bookmarkPosition", bookmarkPosition.name)
        put("totalWordsEnabled", totalWordsEnabled)
        put("ratingEnabled", ratingEnabled)
        put("ratingScale", ratingScale)
        put("searchEnabled", searchEnabled)
        put("genresEnabled", genresEnabled)
        put("onlyChartGenres", onlyChartGenres)
        put("wideMetricCards", wideMetricCards)
        put("webChaptersOnCards", webChaptersOnCards)
        put("sortAllByStatus", sortAllByStatus)
        put("analyticsSections", org.json.JSONArray().apply { analyticsSections.forEach { put(it) } })
        put("goalWords", goalWords)
        put("goalVolumes", goalVolumes)
        put("goalWebCompleted", goalWebCompleted)
        put("goalSeriesCompleted", goalSeriesCompleted)
        put("knownGenres", org.json.JSONArray().apply { knownGenres.forEach { put(it) } })
        put("genreChartOrder", org.json.JSONArray().apply { genreChartOrder.forEach { put(it) } })
        put("collapsibleSections", collapsibleSections)
        put("lastStatusTab", lastStatusTab)
    }

    companion object {
        fun from(o: org.json.JSONObject): Settings {
            val p = o.optJSONObject("palette")
            fun color(key: String, def: Long): Long =
                p?.optString(key)?.toLongOrNull() ?: def
            return Settings(
                theme = runCatching { AppThemeMode.valueOf(o.optString("theme", "DARK")) }.getOrDefault(AppThemeMode.DARK),
                palette = Palette(
                    accent = color("accent", 0xFFFF9F0AL),
                    planned = color("planned", 0xFF60A5FAL),
                    reading = color("reading", 0xFF34D399L),
                    paused = color("paused", 0xFFFBBF24L),
                    completed = color("completed", 0xFFA78BFAL),
                    dropped = color("dropped", 0xFFF87171L),
                    tagSeries = color("tagSeries", 0xFFA78BFAL),
                    tagWeb = color("tagWeb", 0xFFFBBF24L),
                    tagSingle = color("tagSingle", 0xFFFF9F0AL),
                    tagOngoing = color("tagOngoing", 0xFF34D399L),
                ),
                cardStyle = runCatching { CardStyle.valueOf(o.optString("cardStyle", "NUMBERS")) }.getOrDefault(CardStyle.NUMBERS),
                singleLineName = o.optBoolean("singleLineName", false),
                centeredHeaders = o.optBoolean("centeredHeaders", false),
                alignedFilters = o.optBoolean("alignedFilters", true),
                cardChevron = o.optBoolean("cardChevron", true),
                statusTextInList = o.optBoolean("statusTextInList", true),
                dropReasonInCards = o.optBoolean("dropReasonInCards", true),
                hideOngoingTag = o.optBoolean("hideOngoingTag", false),
                abbreviateNumbers = o.optBoolean("abbreviateNumbers", false),
                disableAnimations = o.optBoolean("disableAnimations", false),
                fabGlow = o.optBoolean("fabGlow", false),
                hideBottomBar = o.optBoolean("hideBottomBar", true),
                titleScreenEnabled = o.optBoolean("titleScreenEnabled", false),
                titleHeaderOnScreen = o.optBoolean("titleHeaderOnScreen", true),
                progressBlockOnTitleScreen = o.optBoolean("progressBlockOnTitleScreen", false),
                simpleAdd = o.optBoolean("simpleAdd", false),
                goalsEnabled = o.optBoolean("goalsEnabled", false),
                bookmarksEnabled = o.optBoolean("bookmarksEnabled", true),
                bookmarkPosition = runCatching { BookmarkPos.valueOf(o.optString("bookmarkPosition", "INLINE")) }.getOrDefault(BookmarkPos.INLINE),
                totalWordsEnabled = o.optBoolean("totalWordsEnabled", false),
                ratingEnabled = o.optBoolean("ratingEnabled", true),
                ratingScale = if (o.optInt("ratingScale", 10) == 5) 5 else 10,
                searchEnabled = o.optBoolean("searchEnabled", false),
                genresEnabled = o.optBoolean("genresEnabled", false),
                onlyChartGenres = o.optBoolean("onlyChartGenres", false),
                wideMetricCards = o.optBoolean("wideMetricCards", false),
                webChaptersOnCards = o.optBoolean("webChaptersOnCards", true),
                sortAllByStatus = o.optBoolean("sortAllByStatus", true),
                analyticsSections = mutableListOf<String>().apply {
                    o.optJSONArray("analyticsSections")?.let { arr ->
                        for (i in 0 until arr.length()) add(arr.optString(i))
                    }
                }.ifEmpty { Settings().analyticsSections },
                goalWords = o.optLong("goalWords", 0),
                goalVolumes = o.optLong("goalVolumes", 0),
                goalWebCompleted = o.optLong("goalWebCompleted", 0),
                goalSeriesCompleted = o.optLong("goalSeriesCompleted", 0),
                knownGenres = mutableListOf<String>().apply {
                    o.optJSONArray("knownGenres")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val g = arr.optString(i, "").trim()
                            if (g.isNotEmpty()) add(g)
                        }
                    }
                },
                genreChartOrder = mutableListOf<String>().apply {
                    o.optJSONArray("genreChartOrder")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val g = arr.optString(i, "").trim()
                            if (g.isNotEmpty()) add(g)
                        }
                    }
                },
                collapsibleSections = o.optBoolean("collapsibleSections", false),
                lastStatusTab = o.optString("lastStatusTab", Status.PLANNED.name),
            )
        }
    }
}
