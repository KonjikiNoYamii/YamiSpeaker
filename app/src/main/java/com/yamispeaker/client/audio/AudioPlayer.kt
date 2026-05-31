package com.yamispeaker.client.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

class AudioPlayer {

    private val sampleRate = 48000

    val audioTrack =
            AudioTrack(
                    AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build(),
                    AudioTrack.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT
                    ) * 4,
                    AudioTrack.MODE_STREAM,
                    0
            )

    init {
        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            throw RuntimeException("AudioTrack init failed")
        }
    }

    // mulai playback AudioTrack
    fun start() {
        audioTrack.play()
    }

    // tulis PCM langsung ke AudioTrack
    fun write(pcm: ByteArray) {
        audioTrack.write(pcm, 0, pcm.size)
    }

    // hentikan AudioTrack
    fun stop() {
        if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.stop()
        }
        audioTrack.flush()
    }

    // bebaskan resource
    fun release() {
        audioTrack.release()
    }
}
