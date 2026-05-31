package com.yamispeaker.client.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.sin

class AudioPlayer {

    private val sampleRate = 48000

    fun playTestTone() {

        thread {

            val durationSeconds = 2

            val totalSamples =
                sampleRate * durationSeconds

            val pcm =
                ShortArray(totalSamples)

            val frequency = 440.0

            for (i in pcm.indices) {

                val sample =
                    sin(
                        2.0 *
                        Math.PI *
                        frequency *
                        i /
                        sampleRate
                    )

                pcm[i] =
                    (sample * Short.MAX_VALUE).toInt()
                        .toShort()
            }

            val audioTrack =
                AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(
                            AudioAttributes.USAGE_MEDIA
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_MUSIC
                        )
                        .build(),

                    AudioFormat.Builder()
                        .setEncoding(
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                        .setSampleRate(sampleRate)
                        .setChannelMask(
                            AudioFormat.CHANNEL_OUT_MONO
                        )
                        .build(),

                    pcm.size * 2,
                    AudioTrack.MODE_STATIC,
                    0
                )

            audioTrack.write(
                pcm,
                0,
                pcm.size
            )

            audioTrack.play()
        }
    }
}