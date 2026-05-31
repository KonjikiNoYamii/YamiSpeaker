package com.yamispeaker.client.network

import android.os.Process
import com.yamispeaker.client.audio.AudioPlayer
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

class UdpReceiver(private val audioPlayer: AudioPlayer) {

    private var running = false
    private var socket: DatagramSocket? = null

    // mulai thread UDP, terima packet → kirim ke AudioPlayer
    fun start() {

        if (running) return
        running = true

        thread(name = "UdpReceiver") {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            socket = DatagramSocket(5000)
            val buffer = ByteArray(4096)
            while (running) {

                val packet = DatagramPacket(buffer, buffer.size)

                socket?.receive(packet)

                val audioData = packet.data.copyOf(packet.length)

                audioPlayer.write(audioData)
            }

            socket?.close()
        }
    }

    // hentikan receiver + tutup socket
    fun stop() {
        running = false
        socket?.close()
    }
}
