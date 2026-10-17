package io.github.nikoapps.clock.ui

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nikoapps.clock.settings.BackgroundSync
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    /** Null until the stored settings have loaded, so the screen never flashes defaults. */
    val settings: StateFlow<Settings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            repository.update(transform)
            BackgroundSync.sync(getApplication(), repository.current())
        }
    }

    /** Speech follows the setting; on Android 13+ the screen text follows it too. */
    fun setLanguage(tag: String?) {
        update { it.copy(language = tag) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        }
    }
}
