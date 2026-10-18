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
import io.github.nikoapps.clock.speech.SpeechLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    /** Null until the stored settings have loaded, so the screen never flashes defaults. */
    val settings: StateFlow<Settings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** When a chime was last spoken, so a helper can check they still work. */
    val lastChimeAt: StateFlow<Instant?> = SpeechLog(application).lastChimeAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            repository.update(transform)
            BackgroundSync.sync(getApplication(), repository.current())
        }
    }

    /** Screen text can only follow its own language on Android 13+. */
    val canChooseScreenLanguage: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** The screen language as a language code ("en", "ne"), or null when it follows the phone. */
    fun screenLanguage(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val locales = getApplication<Application>().getSystemService(LocaleManager::class.java).applicationLocales
        return if (locales.isEmpty) null else locales[0].language
    }

    /** Changes only the screen text. The activity restarts in the new language. */
    fun setScreenLanguage(tag: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        getApplication<Application>().getSystemService(LocaleManager::class.java).applicationLocales =
            if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }
}
