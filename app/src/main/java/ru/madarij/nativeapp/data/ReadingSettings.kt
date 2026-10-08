package ru.madarij.nativeapp.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("reading_settings")
data class ReadingSettings(
    val theme: String = "system", val russianSize: Float = 16f, val arabicSize: Float = 26f,
    val lineHeight: Float = 1.6f, val russianFont: String = "sans", val showArabic: Boolean = false,
    val showNotes: Boolean = true, val textWidth: Float = 1f, val alignment: String = "start", val arabicFont: String = "naskh", val brightness: Float = -1f, val reducedMotion: Boolean = false
) {
    fun bounded() = copy(
        theme = theme.takeIf { it in setOf("system", "light", "dark", "sepia") } ?: "system",
        russianSize = russianSize.finiteOr(16f).coerceIn(14f, 36f),
        arabicSize = arabicSize.finiteOr(26f).coerceIn(20f, 44f),
        lineHeight = lineHeight.finiteOr(1.6f).coerceIn(1.3f, 2.2f),
        russianFont = russianFont.takeIf { it in setOf("serif", "sans", "sans-serif", "mono", "monospace", "book") } ?: "sans",
        textWidth = textWidth.finiteOr(1f).coerceIn(0.65f, 1f),
        arabicFont = arabicFont.takeIf { it in setOf("naskh", "alternate") } ?: "naskh",
        brightness = if (brightness == -1f || !brightness.isFinite()) -1f else brightness.coerceIn(0.05f, 1f),
        alignment = alignment.takeIf { it in setOf("start", "justify", "center") } ?: "start"
    )
}
private fun Float.finiteOr(default: Float) = if (isFinite()) this else default
class SettingsRepository(private val context: Context) {
    private val theme = stringPreferencesKey("theme")
    private val russian = floatPreferencesKey("russianSize")
    private val arabic = floatPreferencesKey("arabicSize")
    private val line = floatPreferencesKey("lineHeight")
    private val font = stringPreferencesKey("russianFont")
    private val showArabic = booleanPreferencesKey("showArabic")
    private val showNotes = booleanPreferencesKey("showNotes")
    private val width = floatPreferencesKey("textWidth")
    private val alignment = stringPreferencesKey("alignment")
    private val arabicFont = stringPreferencesKey("arabicFont")
    private val brightness = floatPreferencesKey("brightness")
    private val motion = booleanPreferencesKey("reducedMotion")
    val settings = context.settingsStore.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }.map {
        ReadingSettings(it[theme] ?: "system", it[russian] ?: 16f, it[arabic] ?: 26f,
            it[line] ?: 1.6f, it[font] ?: "sans", it[showArabic] ?: false,
            it[showNotes] ?: true, it[width] ?: 1f, it[alignment] ?: "start", it[arabicFont] ?: "naskh", it[brightness] ?: -1f, it[motion] ?: false).bounded()
    }
    suspend fun update(value: ReadingSettings) {
        val v = value.bounded()
        context.settingsStore.edit {
            it[theme] = v.theme; it[russian] = v.russianSize; it[arabic] = v.arabicSize
            it[line] = v.lineHeight; it[font] = v.russianFont; it[showArabic] = v.showArabic
            it[arabicFont] = v.arabicFont; it[brightness] = v.brightness; it[motion] = v.reducedMotion
            it[showNotes] = v.showNotes; it[width] = v.textWidth; it[alignment] = v.alignment
        }
    }
}
