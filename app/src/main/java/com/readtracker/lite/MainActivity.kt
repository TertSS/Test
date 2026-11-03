package com.readtracker.lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.readtracker.lite.ui.AppTheme
import com.readtracker.lite.ui.EditorScreen
import com.readtracker.lite.ui.LibraryScreen
import com.readtracker.lite.ui.TitleCard
import com.readtracker.lite.data.Title

/**
 * Точка входа: библиотека + редактор/карточка тайтла.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel(factory = AppViewModel.factory(application))
            val titles by vm.titles.collectAsStateWithLifecycle()
            val settings by vm.settings.collectAsStateWithLifecycle()

            AppTheme(settings.theme) {
                var screen by remember { mutableStateOf<Pair<String, Title?>?>(null) }
                val open = screen
                if (open == null) {
                    LibraryScreen(
                        titles = titles,
                        settings = settings,
                        onOpenTitle = { t -> screen = "card" to t },
                        onEdit = { t -> screen = "edit" to t },
                        onDelete = { t -> vm.deleteTitle(t.id) },
                        onAdd = { screen = "edit" to newTitleTemplate() },
                        onSelectTab = { vm.setLastStatusTab(it) },
                    )
                } else if (open.first == "card") {
                    TitleCard(
                        title = open.second!!,
                        settings = settings,
                        onClick = {},
                        onLongClick = { screen = "edit" to open.second },
                    )
                } else {
                    EditorScreen(
                        initial = open.second!!,
                        isNew = !titles.any { it.id == open.second!!.id },
                        settings = settings,
                        onSave = { vm.normalizeAndSave(it, isNew = !titles.any { t -> t.id == it.id }) },
                        onClose = { screen = null },
                    )
                }
            }
        }
    }
}
