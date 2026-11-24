package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AstigmaPreset
import com.example.data.model.PdfPageMode
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerSettingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "tv_reader_settings"
)

class ReaderSettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ASTIGMA_PRESET = stringPreferencesKey("astigma_preset")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val ALT_LAYOUT = booleanPreferencesKey("alternative_layout")
        val FOCUS_MODE = booleanPreferencesKey("focus_mode")
        val PDF_PAGE_MODE = stringPreferencesKey("pdf_page_mode")
        val BRIGHTNESS_PERCENT = intPreferencesKey("brightness_percent")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val settingsFlow: Flow<ReaderSettings> = context.readerSettingsStore.data.map { prefs ->
        ReaderSettings(
            theme = prefs[Keys.THEME]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() }
                ?: ReaderTheme.AMOLED,
            astigmaPreset = prefs[Keys.ASTIGMA_PRESET]?.let {
                runCatching { AstigmaPreset.valueOf(it) }.getOrNull()
            } ?: AstigmaPreset.SOFT_AMBER,
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 22).coerceIn(14, 38),
            fontFamily = prefs[Keys.FONT_FAMILY]?.let {
                runCatching { ReaderFontFamily.valueOf(it) }.getOrNull()
            } ?: ReaderFontFamily.INTER,
            lineHeightMultiplier = (prefs[Keys.LINE_HEIGHT] ?: 1.45f).coerceIn(1.15f, 2.0f),
            readingMode = prefs[Keys.READING_MODE]?.let {
                runCatching { ReadingMode.valueOf(it) }.getOrNull()
            } ?: ReadingMode.PAGINATED,
            alternativeLayout = prefs[Keys.ALT_LAYOUT] ?: false,
            focusMode = prefs[Keys.FOCUS_MODE] ?: false,
            pdfPageMode = prefs[Keys.PDF_PAGE_MODE]?.let {
                runCatching { PdfPageMode.valueOf(it) }.getOrNull()
            } ?: PdfPageMode.ORIGINAL,
            brightnessPercent = (prefs[Keys.BRIGHTNESS_PERCENT] ?: 80).coerceIn(15, 100),
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true
        )
    }

    suspend fun setTheme(theme: ReaderTheme) {
        context.readerSettingsStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setAstigmaPreset(preset: AstigmaPreset) {
        context.readerSettingsStore.edit { it[Keys.ASTIGMA_PRESET] = preset.name }
    }

    suspend fun setFontSize(sizeSp: Int) {
        context.readerSettingsStore.edit { it[Keys.FONT_SIZE_SP] = sizeSp.coerceIn(14, 38) }
    }

    suspend fun setFontFamily(family: ReaderFontFamily) {
        context.readerSettingsStore.edit { it[Keys.FONT_FAMILY] = family.name }
    }

    suspend fun setLineHeight(multiplier: Float) {
        context.readerSettingsStore.edit { it[Keys.LINE_HEIGHT] = multiplier.coerceIn(1.15f, 2.0f) }
    }

    suspend fun setReadingMode(mode: ReadingMode) {
        context.readerSettingsStore.edit { it[Keys.READING_MODE] = mode.name }
    }

    suspend fun setAlternativeLayout(enabled: Boolean) {
        context.readerSettingsStore.edit { it[Keys.ALT_LAYOUT] = enabled }
    }

    suspend fun setFocusMode(enabled: Boolean) {
        context.readerSettingsStore.edit { it[Keys.FOCUS_MODE] = enabled }
    }

    suspend fun setPdfPageMode(mode: PdfPageMode) {
        context.readerSettingsStore.edit { it[Keys.PDF_PAGE_MODE] = mode.name }
    }

    suspend fun setBrightness(percent: Int) {
        context.readerSettingsStore.edit { it[Keys.BRIGHTNESS_PERCENT] = percent.coerceIn(15, 100) }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.readerSettingsStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }
}
