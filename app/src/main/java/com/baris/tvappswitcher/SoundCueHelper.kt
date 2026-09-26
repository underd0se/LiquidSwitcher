package com.baris.tvappswitcher

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.util.Log

object SoundCueHelper {
    private const val TAG = "SoundCueHelper"

    private var soundPool: SoundPool? = null
    private var tickSoundId: Int = 0
    private var isLoaded: Boolean = false
    private var toneGenerator: ToneGenerator? = null

    @Synchronized
    fun init(context: Context) {
        if (soundPool != null) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == tickSoundId) {
                    isLoaded = true
                    Log.d(TAG, "SoundPool tick sound loaded successfully (id=$sampleId)")
                } else {
                    Log.w(TAG, "SoundPool sample $sampleId failed to load, status: $status")
                }
            }

            tickSoundId = pool.load(context.applicationContext, R.raw.dock_tick, 1)
            soundPool = pool
            Log.d(TAG, "SoundPool initialized, loading tickSoundId=$tickSoundId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SoundPool", e)
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val volume = if (audioManager != null) {
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (max > 0) (current * 100 / max) else 70
            } else 70
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize ToneGenerator fallback", e)
        }
    }

    fun playTick(context: Context) {
        if (!ConfigManager.isSoundEffectsEnabled(context)) return

        if (soundPool == null) {
            init(context)
        }

        if (isLoaded && tickSoundId != 0) {
            val streamId = soundPool?.play(tickSoundId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
            if (streamId != 0) {
                Log.d(TAG, "Played tick sound via SoundPool (streamId=$streamId)")
                return
            }
        }

        // Fallback to ToneGenerator if SoundPool is still loading or unavailable
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
            Log.d(TAG, "Played tick sound via ToneGenerator fallback")
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator fallback failed", e)
        }
    }

    @Synchronized
    fun release() {
        try {
            soundPool?.release()
        } catch (_: Exception) {}
        soundPool = null
        tickSoundId = 0
        isLoaded = false

        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}
