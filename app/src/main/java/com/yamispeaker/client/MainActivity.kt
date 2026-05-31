package com.yamispeaker.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yamispeaker.client.audio.AudioPlayer
import com.yamispeaker.client.network.UdpReceiver
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private val audioPlayer = AudioPlayer()
    private val udpReceiver = UdpReceiver(audioPlayer)

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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {
            var started by remember { mutableStateOf(false) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("YamiSpeaker")

                        Text(getLocalIp())

                        Text(if (started) "Listening :5000" else "Stopped")

                        Button(
                                onClick = {
                                    if (!started) {

                                        audioPlayer.start()

                                        udpReceiver.start()

                                        started = true
                                    }
                                }
                        ) { Text("START") }
                    }
                }
            }
        }
    }

    // cleanup saat activity dihancurkan
    override fun onDestroy() {

        udpReceiver.stop()

        audioPlayer.stop()

        audioPlayer.release()

        super.onDestroy()
    }
}
