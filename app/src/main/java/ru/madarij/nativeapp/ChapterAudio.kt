package ru.madarij.nativeapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.madarij.nativeapp.data.Paragraph
import java.util.Locale

internal const val RUVOICE_PACKAGE = "ru.kost.ruvoice"

internal enum class ChapterAudioVoice(val engineName: String, val displayName: String) {
    AIDAR("aidar-ru", "Айдар"),
    EUGENE("eugene-ru", "Евгений"),
}

internal fun isSelectedRuVoice(names: Collection<String>): Boolean =
    names.any { it == "aidar-ru" || it == "eugene-ru" }

/** Russian reading copy only; the original bilingual paragraphs and IDs remain unchanged. */
internal fun chapterSpeechChunks(
    title: String,
    paragraphs: List<Paragraph>,
    maxChars: Int = 680,
): List<String> {
    require(maxChars >= 20)
    val parts = sequenceOf(title) + paragraphs.asSequence()
        .filter { it.role != "editor_note" && it.role != "edition_note" }
        .map { it.ru }
    val chunks = mutableListOf<String>()
    parts.forEach { raw ->
        var text = raw.replace(Regex("\\s+"), " ").trim()
        while (text.isNotEmpty()) {
            if (text.length <= maxChars) {
                chunks.add(text)
                break
            }
            val wordEnd = text.lastIndexOf(' ', maxChars)
            val cut = if (wordEnd > maxChars / 3) wordEnd else maxChars
            chunks.add(text.substring(0, cut).trim())
            text = text.substring(cut).trimStart()
        }
    }
    return chunks
}

internal enum class ChapterAudioStatus { LOADING, READY, PLAYING, PAUSED, ERROR }

/** Lazy explicit link to user-installed RuVoice: no paid APIs or app-embedded models. */
internal class ChapterAudioController(context: Context, initialVoice: ChapterAudioVoice) {
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var closed = false
    private var generation = 0
    private var chunks = emptyList<String>()
    private var playWhenReady = false

    var selectedVoice by mutableStateOf(initialVoice)
        private set
    var status by mutableStateOf(ChapterAudioStatus.LOADING)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var chunkIndex by mutableIntStateOf(0)
        private set
    var chunkCount by mutableIntStateOf(0)
        private set
    var speed by mutableStateOf(1.0f)
        private set

    init {
        engine = TextToSpeech(context.applicationContext, { result ->
            main.post {
                if (closed) return@post
                val tts = engine
                if (result != TextToSpeech.SUCCESS || tts == null) {
                    fail("RuVoice не запускается. Проверьте установку полного APK RuVoice.")
                    return@post
                }
                val language = tts.setLanguage(Locale.forLanguageTag("ru-RU"))
                if (language == TextToSpeech.LANG_MISSING_DATA ||
                    language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    fail("Русские голоса RuVoice не установлены.")
                    return@post
                }
                val voices = runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
                if (!isSelectedRuVoice(voices.map { it.name })) {
                    fail("RuVoice недоступен: подключился другой голосовой движок.")
                    return@post
                }
                if (!applyVoice(tts)) return@post
                tts.setSpeechRate(speed)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit
                    override fun onDone(id: String?) {
                        main.post {
                            if (closed || status != ChapterAudioStatus.PLAYING ||
                                id != utteranceId()) return@post
                            chunkIndex += 1
                            if (chunkIndex >= chunkCount) {
                                chunkIndex = 0
                                status = ChapterAudioStatus.READY
                            } else speakCurrent()
                        }
                    }
                    override fun onError(id: String?) {
                        main.post {
                            if (!closed && status == ChapterAudioStatus.PLAYING &&
                                id == utteranceId()) fail("RuVoice не смог озвучить фрагмент.")
                        }
                    }
                })
                status = ChapterAudioStatus.READY
                if (playWhenReady) {
                    playWhenReady = false
                    startPlayback()
                }
            }
        }, RUVOICE_PACKAGE)
    }

    private fun applyVoice(tts: TextToSpeech): Boolean {
        val target = runCatching { tts.voices.orEmpty() }
            .getOrDefault(emptySet()).firstOrNull { it.name == selectedVoice.engineName }
        if (target == null || tts.setVoice(target) != TextToSpeech.SUCCESS) {
            fail("Голос «" + selectedVoice.displayName + "» недоступен. Проверьте RuVoice.")
            return false
        }
        return true
    }

    fun chooseVoice(newVoice: ChapterAudioVoice) {
        if (closed || selectedVoice == newVoice) return
        selectedVoice = newVoice
        val tts = engine ?: return
        if (status == ChapterAudioStatus.LOADING || status == ChapterAudioStatus.ERROR) return
        val wasPlaying = status == ChapterAudioStatus.PLAYING
        generation++
        tts.stop()
        if (!applyVoice(tts)) return
        if (wasPlaying) speakCurrent()
    }

    fun play(title: String, paragraphs: List<Paragraph>) {
        if (closed || status == ChapterAudioStatus.ERROR) return
        if (status == ChapterAudioStatus.PAUSED && chunks.isNotEmpty()) {
            status = ChapterAudioStatus.PLAYING
            speakCurrent()
            return
        }
        stop()
        chunks = chapterSpeechChunks(title, paragraphs)
        chunkCount = chunks.size
        if (chunkCount == 0) {
            message = "Текст для чтения ещё не загружен."
            return
        }
        if (status == ChapterAudioStatus.LOADING) playWhenReady = true
        else startPlayback()
    }

    private fun startPlayback() {
        if (chunks.isEmpty()) return
        message = null
        status = ChapterAudioStatus.PLAYING
        speakCurrent()
    }

    fun pause() {
        if (status != ChapterAudioStatus.PLAYING) return
        generation++
        status = ChapterAudioStatus.PAUSED
        engine?.stop()
    }

    fun stop() {
        generation++
        playWhenReady = false
        engine?.stop()
        chunkIndex = 0
        if (status != ChapterAudioStatus.LOADING && status != ChapterAudioStatus.ERROR)
            status = ChapterAudioStatus.READY
    }

    fun changeSpeed() {
        speed = when (speed) {
            0.9f -> 1.0f
            1.0f -> 1.1f
            1.1f -> 1.2f
            else -> 0.9f
        }
        engine?.setSpeechRate(speed)
        if (status == ChapterAudioStatus.PLAYING) {
            generation++
            engine?.stop()
            speakCurrent()
        }
    }

    private fun utteranceId(): String = "madarij:" + generation + ":" + chunkIndex

    private fun speakCurrent() {
        if (closed || status != ChapterAudioStatus.PLAYING ||
            chunkIndex !in chunks.indices) return
        val result = engine?.speak(chunks[chunkIndex], TextToSpeech.QUEUE_FLUSH, null, utteranceId())
        if (result != TextToSpeech.SUCCESS) fail("Не удалось начать чтение через RuVoice.")
    }

    private fun fail(details: String) {
        generation++
        playWhenReady = false
        engine?.stop()
        message = details
        status = ChapterAudioStatus.ERROR
    }

    fun close() {
        if (closed) return
        closed = true
        generation++
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
