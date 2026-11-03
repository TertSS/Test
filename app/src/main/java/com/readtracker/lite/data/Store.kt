package com.readtracker.lite.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Локальное хранилище (часть 9 / II-12): библиотека и настройки — отдельные файлы,
 * сохранение при каждом изменении. Битые записи не должны ломать приложение.
 */
object Store {
    private const val LIB_FILE = "library.json"
    private const val SET_FILE = "settings.json"

    private fun libFile(ctx: Context) = File(ctx.filesDir, LIB_FILE)
    private fun setFile(ctx: Context) = File(ctx.filesDir, SET_FILE)

    fun loadLibrary(ctx: Context): List<Title> {
        return try {
            val f = libFile(ctx)
            if (!f.exists()) return emptyList()
            val arr = org.json.JSONArray(f.readText())
            val list = ArrayList<Title>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                runCatching { list.add(Title.from(o)) }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLibrary(ctx: Context, titles: List<Title>) {
        try {
            val arr = org.json.JSONArray()
            titles.forEach { arr.put(it.toJson()) }
            libFile(ctx).writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    fun loadSettings(ctx: Context): Settings {
        return try {
            val f = setFile(ctx)
            if (!f.exists()) return Settings()
            Settings.from(org.json.JSONObject(f.readText()))
        } catch (_: Exception) {
            Settings()
        }
    }

    fun saveSettings(ctx: Context, s: Settings) {
        try {
            setFile(ctx).writeText(s.toJson().toString())
        } catch (_: Exception) {
        }
    }

    /** Экспорт библиотеки в файл для «Поделиться» (II-11). */
    fun exportLibrary(ctx: Context, titles: List<Title>): File {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val f = File(ctx.cacheDir, "readtracker_library_$date.json")
        val arr = org.json.JSONArray()
        titles.forEach { arr.put(it.toJson()) }
        f.writeText(arr.toString(2))
        return f
    }

    fun exportSettings(ctx: Context, s: Settings): File {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val f = File(ctx.cacheDir, "readtracker_settings_$date.json")
        f.writeText(s.toJson().toString(2))
        return f
    }

    data class ImportResult(val titles: List<Title>, val count: Int)

    /** Чтение JSON-файла библиотеки; устойчиво к битым записям. */
    fun readLibraryUri(ctx: Context, uri: Uri): List<Title>? {
        return try {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?.toString(Charsets.UTF_8) ?: return null
            val arr = org.json.JSONArray(text)
            val list = ArrayList<Title>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                runCatching { list.add(Title.from(o)) }
            }
            list
        } catch (_: Exception) {
            null
        }
    }

    fun readSettingsUri(ctx: Context, uri: Uri): Settings? {
        return try {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?.toString(Charsets.UTF_8) ?: return null
            Settings.from(org.json.JSONObject(text))
        } catch (_: Exception) {
            null
        }
    }

    /** Слияние по id: тот же id заменяется данными из файла, остальные добавляются (II-11). */
    fun mergeById(current: List<Title>, incoming: List<Title>): Pair<List<Title>, Int> {
        val map = LinkedHashMap<String, Title>()
        current.forEach { map[it.id] = it }
        var changed = 0
        incoming.forEach { t ->
            map[t.id] = t
            changed++
        }
        // новые тайтлы — в начало (как и при добавлении), обновлённые остаются на месте
        val newOnes = incoming.filterNot { c -> current.any { it.id == c.id } }.reversed()
        val result = ArrayList<Title>()
        result.addAll(newOnes)
        result.addAll(map.values.filterNot { n -> newOnes.any { it.id == n.id } })
        return result to changed
    }

    fun clearAll(ctx: Context) {
        try {
            libFile(ctx).delete()
            setFile(ctx).delete()
        } catch (_: Exception) {
        }
    }
}
