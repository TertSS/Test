package com.readtracker.lite.data

import org.json.JSONArray
import org.json.JSONObject

/** Формат издания: ровно один из трёх (II-1). */
enum class Format(val ru: String, val short: String) {
    SERIES("Серия", "Серия"),
    WEB("Веб", "Веб"),
    SINGLE("Одиночка", "Сингл");

    /** «Флаг считать тома»: включён у Серии и Одиночки, выключен у Веба. */
    val hasVolumes: Boolean get() = this != WEB

    companion object {
        fun from(s: String?): Format = entries.firstOrNull { it.name == s } ?: SERIES
    }
}

/** Статусы чтения (5 значений, II-1). */
enum class Status(val ru: String) {
    PLANNED("В планах"),
    READING("Читаю"),
    PAUSED("На паузе"),
    COMPLETED("Завершено"),
    DROPPED("Брошено");

    companion object {
        /** Неизвестный статус при загрузке → «В планах» (II-12). */
        fun from(s: String?): Status = entries.firstOrNull { it.name == s } ?: PLANNED
    }
}

/** Строка детальной таблицы: номер (дробный, 1.5) → слов (целое). */
data class NumRow(var num: Double, var words: Int) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("num", num)
        put("words", words)
    }

    companion object {
        fun from(o: JSONObject): NumRow = NumRow(
            num = o.optDouble("num", 0.0),
            words = o.optInt("words", 0),
        )
    }
}

/**
 * Модель тайтла (II-1). id — строка, время создания в миллисекундах.
 * Пустые/битые значения при загрузке не ломают приложение: числа → 0, неизвестный статус → «В планах».
 */
data class Title(
    val id: String,
    var name: String = "",
    var author: String = "",
    var description: String = "",
    /** Обложка: URL или путь к локальному файлу; пусто — заглушка. */
    var coverUrl: String = "",
    var coverPath: String = "",
    /** Цвет-заглушка (ARGB), по умолчанию тёмно-индиго. */
    var coverColor: Long = 0xFF3F51B5L,
    var status: Status = Status.PLANNED,
    var format: Format = Format.SERIES,
    var ongoing: Boolean = false,
    var words: Int = 0,
    var volumes: Int = 0,
    var chaptersRead: Int = 0,
    /** «Всего глав»; null или 0 = не задано (в lite поле стартует пустым). */
    var chaptersTotal: Int? = null,
    /** «Всего томов в серии»; null = не задано. При онгоинге не сохраняется. */
    var volumesTotal: Int? = null,
    /** «Всего слов в тайтле»; null = не задано. */
    var totalWordsInTitle: Int? = null,
    var byVolumesMode: Boolean = false,
    var volumeRows: MutableList<NumRow> = mutableListOf(),
    var byChaptersMode: Boolean = false,
    var chapterRows: MutableList<NumRow> = mutableListOf(),
    var bookmark: String = "",
    /** Оценка целым 0–10; null = не задана. */
    var rating: Int? = null,
    var genres: MutableList<String> = mutableListOf(),
    var dropReason: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("author", author)
        put("description", description)
        put("coverUrl", coverUrl)
        put("coverPath", coverPath)
        put("coverColor", coverColor.toString())
        put("status", status.name)
        put("format", format.name)
        put("ongoing", ongoing)
        put("words", words)
        put("volumes", volumes)
        put("chaptersRead", chaptersRead)
        put("chaptersTotal", chaptersTotal ?: JSONObject.NULL)
        put("volumesTotal", volumesTotal ?: JSONObject.NULL)
        put("totalWordsInTitle", totalWordsInTitle ?: JSONObject.NULL)
        put("byVolumesMode", byVolumesMode)
        put("volumeRows", JSONArray().apply { volumeRows.forEach { put(it.toJson()) } })
        put("byChaptersMode", byChaptersMode)
        put("chapterRows", JSONArray().apply { chapterRows.forEach { put(it.toJson()) } })
        put("bookmark", bookmark)
        put("rating", rating ?: JSONObject.NULL)
        put("genres", JSONArray().apply { genres.forEach { put(it) } })
        put("dropReason", dropReason)
    }

    companion object {
        fun from(o: JSONObject): Title {
            val vRows = mutableListOf<NumRow>()
            o.optJSONArray("volumeRows")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { vRows.add(NumRow.from(it)) }
                }
            }
            val cRows = mutableListOf<NumRow>()
            o.optJSONArray("chapterRows")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { cRows.add(NumRow.from(it)) }
                }
            }
            return Title(
                id = o.optString("id", System.currentTimeMillis().toString()),
                name = o.optString("name", ""),
                author = o.optString("author", ""),
                description = o.optString("description", ""),
                coverUrl = o.optString("coverUrl", ""),
                coverPath = o.optString("coverPath", ""),
                coverColor = o.optString("coverColor").toLongOrNull() ?: 0xFF3F51B5L,
                status = Status.from(o.optString("status").ifEmpty { null }),
                format = Format.from(o.optString("format").ifEmpty { null }),
                ongoing = o.optBoolean("ongoing", false),
                words = o.optInt("words", 0),
                volumes = o.optInt("volumes", 0),
                chaptersRead = o.optInt("chaptersRead", 0),
                chaptersTotal = if (o.isNull("chaptersTotal")) null else o.optInt("chaptersTotal", 0),
                volumesTotal = if (o.isNull("volumesTotal")) null else o.optInt("volumesTotal", 0),
                totalWordsInTitle = if (o.isNull("totalWordsInTitle")) null else o.optInt("totalWordsInTitle", 0),
                byVolumesMode = o.optBoolean("byVolumesMode", false),
                volumeRows = vRows,
                byChaptersMode = o.optBoolean("byChaptersMode", false),
                chapterRows = cRows,
                bookmark = o.optString("bookmark", ""),
                rating = if (o.isNull("rating")) null else o.optInt("rating", 0),
                genres = mutableListOf<String>().apply {
                    o.optJSONArray("genres")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val g = arr.optString(i, "").trim()
                            if (g.isNotEmpty()) add(g)
                        }
                    }
                },
                dropReason = o.optString("dropReason", ""),
            )
        }
    }
}
