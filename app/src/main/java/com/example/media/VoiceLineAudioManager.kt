package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.os.Build
import java.io.File

class VoiceLineAudioManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var activeRecordingFile: File? = null
    private var player: MediaPlayer? = null

    private fun voiceLinesDir(): File =
        File(context.filesDir, "voice_lines").apply { mkdirs() }

    fun createRecordingFile(id: String): File =
        File(voiceLinesDir(), "voice_${id}.m4a")

    fun startRecording(file: File): Result<Unit> {
        stopRecording()
        stopPlayback()
        return try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            activeRecordingFile = file
            Result.success(Unit)
        } catch (e: Exception) {
            releaseRecorderSafely()
            if (file.exists()) file.delete()
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Microphone unavailable (try on a device with mic hardware)."
                )
            )
        }
    }

    fun stopRecording(): File? {
        val file = activeRecordingFile
        val rec = recorder
        recorder = null
        activeRecordingFile = null
        if (rec != null) {
            try {
                rec.stop()
            } catch (_: Exception) {
            }
            try {
                rec.reset()
                rec.release()
            } catch (_: Exception) {
            }
        }
        return if (file != null && file.exists() && file.length() > 0L) file else null
    }

    fun cancelRecording() {
        val file = activeRecordingFile
        releaseRecorderSafely()
        if (file != null && file.exists()) {
            file.delete()
        }
    }

    private fun releaseRecorderSafely() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        } finally {
            recorder = null
            activeRecordingFile = null
        }
    }

    fun playVoiceLineOrDefault(
        filePath: String?,
        onFinished: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        stopPlayback()
        val hasCustomFile = !filePath.isNullOrBlank() && File(filePath).let { it.exists() && it.length() > 0L }
        if (hasCustomFile) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(filePath)
                    setOnCompletionListener {
                        stopPlayback()
                        onFinished()
                    }
                    setOnErrorListener { _, _, _ ->
                        stopPlayback()
                        playSystemFallbackSound(onFinished, onError)
                        true
                    }
                    prepare()
                    start()
                }
                player = mp
                return
            } catch (_: Exception) {
                stopPlayback()
            }
        }
        playSystemFallbackSound(onFinished, onError)
    }

    private fun playSystemFallbackSound(
        onFinished: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                ringtone.play()
                onFinished()
            } else {
                onError("No notification sound available.")
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Audio playback unavailable.")
        }
    }

    fun stopPlayback() {
        val mp = player
        player = null
        if (mp != null) {
            try {
                if (mp.isPlaying) mp.stop()
            } catch (_: Exception) {
            }
            try {
                mp.release()
            } catch (_: Exception) {
            }
        }
    }

    fun deleteVoiceFile(filePath: String) {
        try {
            val f = File(filePath)
            if (f.exists()) f.delete()
        } catch (_: Exception) {
        }
    }

    fun release() {
        releaseRecorderSafely()
        stopPlayback()
    }
}
