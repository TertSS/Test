package com.readtracker.lite

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.readtracker.lite.data.Format
import com.readtracker.lite.data.Settings
import com.readtracker.lite.data.Status
import com.readtracker.lite.data.Store
import com.readtracker.lite.data.Title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Единое состояние приложения: библиотека + настройки, сохранение при каждом изменении (II-12). */
class AppViewModel(private val app: Application) : ViewModel() {

    private val _titles = MutableStateFlow<List<Title>>(emptyList())
    val titles: StateFlow<List<Title>> = _titles

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _titles.value = Store.loadLibrary(app)
            _settings.value = Store.loadSettings(app)
        }
    }

    private fun persistLibrary() {
        val snapshot = _titles.value
        viewModelScope.launch(Dispatchers.IO) { Store.saveLibrary(app, snapshot) }
    }

    // ----- Библиотека -----

    /** Новые тайтлы вставляются в начало списка (II-1). */
    fun addTitle(t: Title) {
        _titles.value = listOf(t) + _titles.value
        onTitlesChanged()
    }

    fun updateTitle(t: Title) {
        _titles.value = _titles.value.map { if (it.id == t.id) t else it }
        onTitlesChanged()
    }

    fun deleteTitle(id: String) {
        _titles.value = _titles.value.filterNot { it.id == id }
        persistLibrary()
    }

    /** Словарь жанров пополняется при сохранении любого тайтла, не уменьшается при удалении (II-10). */
    private fun onTitlesChanged() {
        mergeGenresFromLibrary()
        persistLibrary()
    }

    private fun mergeGenresFromLibrary() {
        val used = LinkedHashSet<String>()
        _settings.value.knownGenres.forEach { used.add(it) }
        _titles.value.flatMap { it.genres }.map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { g ->
                if (used.none { it.equals(g, ignoreCase = true) }) used.add(g)
            }
        updateSettings(_settings.value.copy(knownGenres = used.toList()))
    }

    fun importAppend(incoming: List<Title>) {
        val (merged, _) = Store.mergeById(_titles.value, incoming)
        _titles.value = merged
        onTitlesChanged()
    }

    fun importReplace(incoming: List<Title>) {
        _titles.value = incoming
        onTitlesChanged()
    }

    fun clearData() {
        _titles.value = emptyList()
        updateSettings(Settings())
        Store.clearAll(app)
    }

    // ----- Настройки -----

    fun updateSettings(s: Settings) {
        _settings.value = s
        viewModelScope.launch(Dispatchers.IO) { Store.saveSettings(app, s) }
    }

    fun setLastStatusTab(name: String) = updateSettings(_settings.value.copy(lastStatusTab = name))

    fun resetSettings() = updateSettings(Settings())

    /** Пустое название при сохранении превращается в «Без названия» (II-1). */
    fun normalizeAndSave(t: Title, isNew: Boolean) {
        val fixed = t.copy(name = t.name.trim().ifEmpty { "Без названия" })
        if (isNew) addTitle(fixed) else updateTitle(fixed)
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(app) as T
            }
    }
}

/** Вкладки статусов библиотеки (II-5): Все · Читаю · В планах · Завершено · На паузе · Брошено. */
object LibraryTabs {
    const val ALL = "ALL"
    val TABS: List<Pair<String, String>> = listOf(
        ALL to "Все",
        Status.READING.name to Status.READING.ru,
        Status.PLANNED.name to Status.PLANNED.ru,
        Status.COMPLETED.name to Status.COMPLETED.ru,
        Status.PAUSED.name to Status.PAUSED.ru,
        Status.DROPPED.name to Status.DROPPED.ru,
    )
}

/** Разделы аналитики (II-8). */
object AnalyticsSections {
    val ALL = listOf(
        "top_words" to "Топ-5 по словам",
        "words_by_status" to "Слова по статусам",
        "genres" to "Жанровые предпочтения",
        "goals" to "Цели",
    )
    fun title(key: String) = ALL.firstOrNull { it.first == key }?.second ?: key
}

fun newTitleTemplate(): Title = Title(id = System.currentTimeMillis().toString(), format = Format.SERIES)
