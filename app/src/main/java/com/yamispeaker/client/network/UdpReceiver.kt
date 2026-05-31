package com.yamispeaker.client.network

import com.yamispeaker.client.audio.AudioPlayer
import com.yamispeaker.client.audio.OpusDecoderJNI
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.Arrays
import kotlin.concurrent.thread

class UdpReceiver(
        private val audioPlayer: AudioPlayer,
        private val decoder: OpusDecoderJNI
) {

    private var running = false
    private var socket: DatagramSocket? = null

    // mulai thread UDP: terima packet → decode Opus → tulis PCM ke AudioTrack
    fun start() {
        if (running) return
        running = true

        thread(name = "UdpReceiver") {
            socket = DatagramSocket(5000)
            val buf = ByteArray(1500)
            val pcmOut = ShortArray(1920)

            while (running) {
                val packet = DatagramPacket(buf, buf.size)
                socket?.receive(packet)

                // skip 4 byte header (seq + timestamp)
                val opusPayload = Arrays.copyOfRange(
                        packet.data, 4, packet.length
                )

                val pcmSamples = decoder.decode(opusPayload, opusPayload.size, pcmOut)

                if (pcmSamples > 0) {
                    val pcmBytes = ByteArray(pcmSamples * 2 * 2) // stereo short→byte
                    var idx = 0
                    for (i in 0 until pcmSamples * 2) {
                        val s = pcmOut[i].toInt()
                        pcmBytes[idx++] = (s and 0xFF).toByte()
                        pcmBytes[idx++] = (s shr 8).toByte()
                    }
                    audioPlayer.write(pcmBytes)
                }
            }

            socket?.close()
        }
    }

    // hentikan receiver
    fun stop() {
        running = false
        socket?.close()
    }
}
