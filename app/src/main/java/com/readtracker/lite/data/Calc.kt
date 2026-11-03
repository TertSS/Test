package com.readtracker.lite.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Точная логика подсчёта оригинала (II-2): эффективные слова/тома, подписи, числа, прогресс.
 */
object Calc {

    /** Эффективные слова: сумма строк в детальном режиме, иначе введённое число. */
    fun effWords(t: Title): Int = when {
        t.format == Format.WEB && t.byChaptersMode -> t.chapterRows.sumOf { it.words }
        t.format != Format.WEB && t.byVolumesMode -> t.volumeRows.sumOf { it.words }
        else -> t.words
    }

    /** Эффективные тома: в режиме по томам — число строк (не наибольший номер), иначе введённое число. */
    fun effVolumes(t: Title): Int =
        if (t.format.hasVolumes && t.byVolumesMode) t.volumeRows.size else t.volumes

    /** Подпись томов: пусто у Веба; онгоинг «5/? т.»; задано всего «5/22 т.»; иначе «5 т.». */
    fun volumesLabel(t: Title): String {
        if (!t.format.hasVolumes) return ""
        val v = effVolumes(t)
        return when {
            t.ongoing -> "$v/? т."
            (t.volumesTotal ?: 0) > 0 -> "$v/${t.volumesTotal} т."
            else -> "$v т."
        }
    }

    /** Подпись глав (Веб): «120/340 гл.» или «120 гл.»; 0 в «всего» = не задано. */
    fun chaptersLabel(t: Title): String {
        if (t.format != Format.WEB) return ""
        val total = t.chaptersTotal ?: 0
        return if (total > 0) "${t.chaptersRead}/$total гл." else "${t.chaptersRead} гл."
    }

    /** Форматирование числа: разделитель тысяч — пробел («1 250 000»). */
    fun groupNumber(n: Long): String {
        val s = n.toString()
        val neg = s.startsWith("-")
        val digits = if (neg) s.substring(1) else s
        val sb = StringBuilder()
        val rem = digits.length % 3
        for (i in digits.indices) {
            if (i > 0 && (i - rem) % 3 == 0) sb.append(' ')
            sb.append(digits[i])
        }
        return if (neg) "-" + sb else sb.toString()
    }

    /**
     * Числа с учётом настройки «Сокращать числа»: от 1 000 — «150K»,
     * от 1 000 000 — «1.2M» (один знак).
     */
    fun num(n: Long, abbreviate: Boolean): String {
        if (!abbreviate) return groupNumber(n)
        val a = kotlin.math.abs(n)
        return when {
            a >= 1_000_000 -> {
                val one = (a / 100_000.0 * 10).roundToLong() / 10.0
                trimDot(one.toString()) + "M"
            }
            a >= 1_000 -> (a / 1000).toString() + "K"
            else -> n.toString()
        }
    }

    private fun trimDot(s: String): String = if (s.endsWith(".0")) s.dropLast(2) else s

    /** Дробный номер без «.0», если целое (II-12). */
    fun rowNum(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

    /** Оценка при шкале 10 — «7/10 ★»; при 5 — «round(v/2)/5 ★». */
    fun ratingLabel(rating: Int?, scale: Int): String {
        val r = rating ?: return ""
        if (r <= 0) return ""
        return if (scale == 5) "${((r / 2.0).roundToLong())}/5 ★" else "$r/10 ★"
    }

    data class Progress(val value: Float, val colorKey: Int, val label: String) {
        companion object {
            const val COLOR_ACCENT = 0
            const val COLOR_STATUS = 1
            const val COLOR_WEB_TAG = 2
        }
    }

    /**
     * Прогресс (II-2): первый подходящий вариант —
     * 1) всего слов (настройка включена и >0) → акцент;
     * 2) формат с томами и всего томов >0 → цвет статуса;
     * 3) веб и всего глав >0 → цвет тега «Веб»;
     * 4) иначе null. Ограничение 0–100%.
     */
    fun progress(t: Title, totalWordsEnabled: Boolean): Progress? {
        if (totalWordsEnabled) {
            val tw = t.totalWordsInTitle ?: 0
            if (tw > 0) {
                val w = effWords(t)
                val p = clampPct(w.toFloat() / tw)
                return Progress(
                    p,
                    Progress.COLOR_ACCENT,
                    "Слова: ${groupNumber(w.toLong())} / ${groupNumber(tw.toLong())}",
                )
            }
        }
        if (t.format.hasVolumes) {
            val vt = t.volumesTotal ?: 0
            if (!t.ongoing && vt > 0) {
                val v = effVolumes(t)
                val p = clampPct(v.toFloat() / vt)
                return Progress(p, Progress.COLOR_STATUS, "Тома: $v/$vt т.")
            }
        }
        if (t.format == Format.WEB) {
            val ct = t.chaptersTotal ?: 0
            if (ct > 0) {
                val p = clampPct(t.chaptersRead.toFloat() / ct)
                return Progress(
                    p,
                    Progress.COLOR_WEB_TAG,
                    "Главы: ${t.chaptersRead}/$ct гл.",
                )
            }
        }
        return null
    }

    private fun clampPct(raw: Float): Float = min(1f, max(0f, raw))

    fun pctText(value: Float): String = (value * 100).roundToLong().toString() + "%"

    // ----- Общие итоги (II-2): считаются по ВСЕМ тайтлам независимо от статуса -----

    fun totalWords(titles: List<Title>): Long = titles.sumOf { effWords(it).toLong() }
    fun totalVolumes(titles: List<Title>): Long = titles.sumOf { effVolumes(it).toLong() }
    fun totalChapters(titles: List<Title>): Long =
        titles.sumOf { if (it.format == Format.WEB) (it.chaptersRead).toLong() else 0L }

    fun completedSeries(titles: List<Title>): Int = titles.count {
        it.status == Status.COMPLETED && it.format == Format.SERIES
    }

    fun completedWeb(titles: List<Title>): Int = titles.count {
        it.status == Status.COMPLETED && it.format == Format.WEB
    }

    fun wordsByStatus(titles: List<Title]): Map<Status, Long> =
        Status.entries.associateWith { st ->
            titles.filter { it.status == st }.sumOf { effWords(it).toLong() }
        }

    fun countByStatus(titles: List<Title]): Map<Status, Int> =
        Status.entries.associateWith { st -> titles.count { it.status == st } }

    /** Жанр: обрезается по краям, пустые пропускаются; считается число тайтлов с жанром (II-8). */
    fun genreCounts(titles: List<Title>): Map<String, Int> {
        val map = LinkedHashMap<String, Int>()
        titles.forEach { t ->
            t.genres.map { it.trim() }.filter { it.isNotEmpty() }.forEach { g ->
                map[g] = (map[g] ?: 0) + 1
            }
        }
        return map
    }

    /**
     * Лепестки радарной диаграммы: сначала пользовательский порядок,
     * остальные по убыванию количества; максимум 8 (II-8).
     */
    fun chartGenres(counts: Map<String, Int>, userOrder: List<String>): List<Pair<String, Int>> {
        val rest = counts.entries.sortedByDescending { it.value }
            .filter { e -> userOrder.none { it.equals(e.key, ignoreCase = true) } }
            .map { it.key to it.value }
        val ordered = userOrder.mapNotNull { g -> counts.entries.firstOrNull { it.key.equals(g, true) }?.let { it.key to it.value } }
        return (ordered + rest).take(8)
    }

    /** Порядок во вкладке «Все» при сортировке по статусу (II-5). */
    fun sortedForAllTab(titles: List<Title>, sortEnabled: Boolean): List<Title> {
        if (!sortEnabled) return titles // новые сверху уже обеспечены порядком вставки
        val order = listOf(Status.PLANNED, Status.READING, Status.PAUSED, Status.DROPPED, Status.COMPLETED)
        return order.flatMap { st -> titles.filter { it.status == st } }
    }

    /**
     * Валидация сохранения (II-3). Возвращает список сообщений об ошибках.
     */
    fun validate(t: Title, totalWordsEnabled: Boolean): List<String> {
        val errors = ArrayList<String>()
        val ct = t.chaptersTotal ?: 0
        if (t.format == Format.WEB && ct > 0 && t.chaptersRead > ct) {
            errors.add("Прочитано глав больше, чем указано всего глав")
        }
        if (totalWordsEnabled) {
            val tw = t.totalWordsInTitle ?: 0
            if (tw > 0 && effWords(t) > tw) {
                errors.add("Слов прочитано больше, чем всего слов в тайтле")
            }
        }
        val vt = t.volumesTotal ?: 0
        if (t.format.hasVolumes && !t.ongoing && vt > 0) {
            val cur = if (t.byVolumesMode) t.volumeRows.size else t.volumes
            if (cur > vt) errors.add("Указано томов больше, чем всего томов в серии")
        }
        return errors
    }
}
