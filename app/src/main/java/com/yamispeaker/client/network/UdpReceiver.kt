package com.yamispeaker.client.network

import android.os.Process
import com.yamispeaker.client.audio.AudioPlayer
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import kotlin.concurrent.thread

class UdpReceiver(private val audioPlayer: AudioPlayer) {

    private var running = false
    private var socket: DatagramSocket? = null
    private var workerThread: Thread? = null

    private val port = 5000

    fun start() {

        if (running) return
        running = true

        workerThread =
                thread(name = "UdpReceiver") {

                    // 🎧 prioritaskan thread audio
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

                    try {
                        socket = DatagramSocket(port)
                        socket?.reuseAddress = true

                        val buffer = ByteArray(4096)

                        while (running) {

                            val packet = DatagramPacket(buffer, buffer.size)

                            try {
                                socket?.receive(packet)
                            } catch (e: SocketException) {
                                // socket ditutup saat stop()
                                break
                            }

                            if (!running) break

                            val size = packet.length

                            if (size > 0) {

                                val audioData = packet.data.copyOf(size)

                                audioPlayer.write(audioData)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            socket?.close()
                        } catch (_: Exception) {}

                        socket = null
                    }
                }
    }

    fun stop() {

        running = false

        try {
            socket?.close()
        } catch (_: Exception) {}

        socket = null

        workerThread?.interrupt()
        workerThread = null
    }
}
