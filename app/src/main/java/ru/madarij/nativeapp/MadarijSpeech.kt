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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.madarij.nativeapp.data.Paragraph
import java.util.Locale

internal const val RUVOICE_PACKAGE = "ru.kost.ruvoice"
internal const val NATURAL_SPEECH_RATE = 1.0f

internal enum class VoiceChoice(val label: String, val voiceName: String?) {
    DEFAULT("Как в RuVoice", null),
    AIDAR("Aidar · мужской", "aidar-ru"),
    EUGENE("Eugene · мужской", "eugene-ru")
}

internal fun isRuVoiceCatalog(voices: Collection<String>): Boolean =
    voices.any { it in setOf("aidar-ru", "eugene-ru", "baya-ru", "xenia-ru", "kseniya-ru") }

internal data class SpeechChunk(val text: String, val paragraphId: String?)

/** Only visible Russian text is spoken; corpus, translations, notes and Arabic stay unchanged. */
internal fun buildMadarijSpeech(title: String, paragraphs: List<Paragraph>, maxChars: Int = 650): List<SpeechChunk> {
    require(maxChars >= 40)
    val result = ArrayList<SpeechChunk>()
    fun add(raw: String, id: String?) {
        var text = readerDisplayMarkdown(raw).replace(Regex("\\s+"), " ").trim()
        while (text.isNotEmpty()) {
            if (text.length <= maxChars) {
                result.add(SpeechChunk(text, id))
                break
            }
            val space = text.lastIndexOf(' ', maxChars)
            val punct = if (space > 0) text.substring(0, space + 1)
                .indexOfLast { it == '.' || it == '?' || it == '!' || it == '…' } else -1
            val sentence = punct.takeIf { it > maxChars / 3 && text.getOrNull(it + 1) == ' ' }
            val cut = if (sentence != null) sentence + 1 else if (space > maxChars / 3) space else maxChars
            result.add(SpeechChunk(text.take(cut).trim(), id))
            text = text.drop(cut).trimStart()
        }
    }
    add(title, null)
    paragraphs.forEach { p -> if (p.ru.isNotBlank()) add(p.ru, p.id) }
    return result
}

internal enum class SpeechStatus { Preparing, Ready, Playing, Paused, Error }

/** On-demand native RuVoice client; keeps at most two chunks queued to reduce synthesis gaps. */
internal class MadarijSpeechController(context: Context, initialVoice: VoiceChoice, initialRate: Float) {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var engineReady = false
    private var closed = false
    private var generation = 0
    private var preparation: Job? = null
    private var chunks: List<SpeechChunk> = emptyList()
    private var pendingStart = false

    var status by mutableStateOf(SpeechStatus.Preparing)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var selectedVoice by mutableStateOf(initialVoice)
        private set
    var speed by mutableStateOf(initialRate)
        private set
    var playedChunks by mutableIntStateOf(0)
        private set
    var totalChunks by mutableIntStateOf(0)
        private set

    init {
        tts = TextToSpeech(context.applicationContext, { result ->
            handler.post {
                if (closed) return@post
                val engine = tts
                if (result != TextToSpeech.SUCCESS || engine == null) {
                    fail("RuVoice не найден. Установите бесплатный RuVoice TTS.")
                    return@post
                }
                val available = runCatching { engine.voices.orEmpty().toList() }.getOrDefault(emptyList())
                if (!isRuVoiceCatalog(available.map { it.name })) {
                    fail("Запустился другой синтезатор вместо RuVoice. Проверьте установку.")
                    return@post
                }
                val language = engine.setLanguage(Locale.forLanguageTag("ru-RU"))
                if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    fail("В RuVoice не установлена русская модель.")
                    return@post
                }
                if (!applyVoice(available.map { it.name })) return@post
                engine.setSpeechRate(speed)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        handler.post {
                            if (closed || status != SpeechStatus.Playing ||
                                utteranceId != "$generation:$playedChunks") return@post
                            playedChunks++
                            if (playedChunks >= totalChunks) {
                                status = SpeechStatus.Ready
                                chunks = emptyList()
                            } else {
                                queueChunk(playedChunks + 1, TextToSpeech.QUEUE_ADD)
                            }
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        handler.post {
                            if (!closed && status == SpeechStatus.Playing &&
                                utteranceId == "$generation:$playedChunks") {
                                fail("RuVoice не смог прочитать фрагмент. Попробуйте другой голос.")
                            }
                        }
                    }
                })
                engineReady = true
                if (pendingStart) {
                    pendingStart = false
                    begin()
                } else {
                    status = SpeechStatus.Ready
                }
            }
        }, RUVOICE_PACKAGE)
    }

    private fun applyVoice(available: List<String>): Boolean {
        val name = selectedVoice.voiceName ?: return true // use RuVoice's current speaker
        if (name !in available) {
            fail("Голос " + selectedVoice.label + " отсутствует в RuVoice.")
            return false
        }
        val selected = tts?.voices?.firstOrNull { it.name == name }
        if (selected == null || tts?.setVoice(selected) != TextToSpeech.SUCCESS) {
            fail("RuVoice не смог включить голос " + selectedVoice.label)
            return false
        }
        return true
    }

    fun chooseVoice(choice: VoiceChoice) {
        if (choice == selectedVoice) return
        stop()
        error = null
        selectedVoice = choice
        if (engineReady) {
            status = SpeechStatus.Ready
            applyVoice(tts?.voices.orEmpty().map { it.name })
        }
    }

    fun changeSpeed(rate: Float) {
        speed = rate
        tts?.setSpeechRate(rate)
    }

    fun play(title: String, paragraphs: List<Paragraph>) {
        if (status == SpeechStatus.Paused) { resume(); return }
        if (status == SpeechStatus.Error) return
        stop()
        status = SpeechStatus.Preparing
        preparation = scope.launch {
            try {
                val readyChunks = withContext(Dispatchers.Default) { buildMadarijSpeech(title, paragraphs) }
                if (closed) return@launch
                if (readyChunks.isEmpty()) { fail("В разделе нет текста для чтения."); return@launch }
                chunks = readyChunks
                totalChunks = readyChunks.size
                playedChunks = 0
                if (engineReady) begin() else pendingStart = true
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { fail("Не удалось подготовить озвучку раздела.") }
        }
    }

    private fun begin() {
        if (closed || !engineReady || chunks.isEmpty()) return
        generation++
        error = null
        status = SpeechStatus.Playing
        queueChunk(playedChunks, TextToSpeech.QUEUE_FLUSH)
        queueChunk(playedChunks + 1, TextToSpeech.QUEUE_ADD)
    }

    private fun queueChunk(index: Int, mode: Int) {
        if (closed || status != SpeechStatus.Playing || index !in chunks.indices) return
        if (tts?.speak(chunks[index].text, mode, null, "$generation:$index") != TextToSpeech.SUCCESS)
            fail("RuVoice не смог начать чтение.")
    }

    fun pause() {
        if (status != SpeechStatus.Playing) return
        generation++
        status = SpeechStatus.Paused
        tts?.stop()
    }
    fun resume() {
        if (status == SpeechStatus.Paused) begin()
    }
    fun stop() {
        pendingStart = false
        preparation?.cancel()
        preparation = null
        generation++
        tts?.stop()
        chunks = emptyList()
        playedChunks = 0
        totalChunks = 0
        if (status != SpeechStatus.Error) status = if (engineReady) SpeechStatus.Ready else SpeechStatus.Preparing
    }
    private fun fail(message: String) {
        pendingStart = false
        generation++
        tts?.stop()
        error = message
        status = SpeechStatus.Error
    }
    fun close() {
        if (closed) return
        closed = true
        stop()
        scope.cancel()
        tts?.shutdown()
        tts = null
    }
}
