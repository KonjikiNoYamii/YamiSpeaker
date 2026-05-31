package com.yamispeaker.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.yamispeaker.client.audio.AudioPlayer
import com.yamispeaker.client.audio.OpusDecoderJNI
import com.yamispeaker.client.network.UdpReceiver
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private val audioPlayer = AudioPlayer()
    private val decoder = OpusDecoderJNI()
    private val udpReceiver = UdpReceiver(audioPlayer, decoder)

    private var laptopIp by mutableStateOf("")
    private var started by mutableStateOf(false)
    private var statusText by mutableStateOf("Stopped")

    // kirim sinyal "siap" ke laptop agar laptop mulai streaming
    private fun notifyLaptop(ip: String) {
        try {
            val socket = DatagramSocket()
            val data = "YAMISPEAKER_READY".toByteArray()
            val packet = DatagramPacket(
                    data, data.size,
                    InetAddress.getByName(ip), 5001
            )
            socket.send(packet)
            socket.close()
        } catch (_: Exception) { }
    }

    // cari IP lokal (bukan loopback) untuk ditampilkan di UI
    private fun getLocalIp(): String {
        NetworkInterface.getNetworkInterfaces().toList().forEach { iface ->
            iface.inetAddresses.toList().forEach { addr ->
                if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                    return addr.hostAddress!!
                }
            }
        }
        return "Unknown"
    }

    private val qrScanner = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            laptopIp = result.contents
            statusText = "Laptop: $laptopIp"
            notifyLaptop(laptopIp)
            audioPlayer.start()
            udpReceiver.start()
            started = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("YamiSpeaker")
                        Spacer(Modifier.height(8.dp))
                        Text("HP: ${getLocalIp()}")
                        Spacer(Modifier.height(4.dp))
                        if (laptopIp.isNotEmpty()) {
                            Text("Laptop: $laptopIp")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(statusText)
                        Spacer(Modifier.height(16.dp))
                        Button(
                                onClick = {
                                    val options = ScanOptions().apply {
                                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        setPrompt("Scan QR dari terminal laptop")
                                        setBeepEnabled(false)
                                        setOrientationLocked(false)
                                    }
                                    qrScanner.launch(options)
                                }
                        ) { Text("SCAN QR") }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        udpReceiver.stop()
        audioPlayer.stop()
        audioPlayer.release()
        decoder.release()
        super.onDestroy()
    }
}
