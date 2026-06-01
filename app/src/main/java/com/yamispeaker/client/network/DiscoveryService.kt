package com.yamispeaker.client.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

object DiscoveryService {

    private var running = false
    private var socket: DatagramSocket? = null
    private var localIp: String = ""
    @Volatile
    var ready: Boolean = false
    var currentAudio by mutableStateOf("")
    var laptopHostname by mutableStateOf("")

    fun start(hpIp: String) {
        if (running) return
        running = true
        localIp = hpIp

        thread(name = "Discovery") {
            socket = DatagramSocket(5002)
            socket?.soTimeout = 60000
            val buf = ByteArray(512)

            while (running) {
                try {
                    val pkt = DatagramPacket(buf, buf.size)
                    socket?.receive(pkt)
                    val msg = String(pkt.data, 0, pkt.length, Charsets.UTF_8).trim()
                    when {
                        msg.startsWith("YAMISPEAKER_DISCOVER") -> {
                            val suffix = if (ready) ":ready" else ""
                            val resp = "YAMISPEAKER_HERE:$localIp$suffix"
                            val data = resp.toByteArray(Charsets.UTF_8)
                            val out = DatagramPacket(
                                data, data.size,
                                pkt.address, pkt.port
                            )
                            socket?.send(out)
                        }
                        msg.startsWith("AUDIO_META:") -> {
                            currentAudio = msg.removePrefix("AUDIO_META:")
                        }
                        msg.startsWith("HOSTNAME:") -> {
                            laptopHostname = msg.removePrefix("HOSTNAME:")
                        }
                    }
                } catch (_: Exception) {
                    if (!running) break
                }
            }
            socket?.close()
        }
    }

    fun sendReady(laptopIp: String) {
        thread(name = "SendReady") {
            try {
                val data = "YAMISPEAKER_READY".toByteArray(Charsets.UTF_8)
                val sock = DatagramSocket()
                val pkt = DatagramPacket(
                    data, data.size,
                    InetAddress.getByName(laptopIp), 5002
                )
                sock.send(pkt)
                sock.close()
            } catch (_: Exception) { }
        }
    }

    fun stop() {
        running = false
        socket?.close()
    }
}
