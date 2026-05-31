package com.yamispeaker.client.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class AudioPlayer {

    private val sampleRate = 48000

    private val minBufferSize =
            AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
            )

    val audioTrack =
            AudioTrack(
                    AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build(),
                    minBufferSize * 12,
                    AudioTrack.MODE_STREAM,
                    0
            )

    private val queue = ArrayBlockingQueue<ByteArray>(100)

    @Volatile private var running = false

    init {
        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            throw RuntimeException("AudioTrack initialization failed")
        }
    }

    // mulai AudioTrack + thread konsumen queue
    fun start() {
        running = true

        audioTrack.play()

        // 🔥 IMPORTANT: isi buffer awal 100–200ms
        val warmup = ByteArray(48000 * 2 * 2 / 10)
        audioTrack.write(warmup, 0, warmup.size)

        thread(name = "AudioPlayer") {
            val localBuffer = ByteArray(4096)

            while (running) {

                val data = queue.poll()

                if (data != null) {

                    var offset = 0

                    while (offset < data.size) {

                        val size = minOf(localBuffer.size, data.size - offset)

                        System.arraycopy(data, offset, localBuffer, 0, size)

                        audioTrack.write(localBuffer, 0, size, AudioTrack.WRITE_BLOCKING)

                        offset += size
                    }
                } else {
                    // jangan spin terlalu keras
                    Thread.sleep(2)
                }
            }
        }
    }

    // masukkan data ke queue; drop oldest jika penuh
    fun write(data: ByteArray) {
        if (!queue.offer(data)) {
            queue.poll()
            queue.offer(data)
        }
    }

    // hentikan thread + AudioTrack
    fun stop() {
        running = false
        if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.stop()
        }
        audioTrack.flush()
    }

    // bebaskan resource AudioTrack
    fun release() {
        audioTrack.release()
    }
}
