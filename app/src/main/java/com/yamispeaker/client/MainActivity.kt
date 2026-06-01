package com.yamispeaker.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.yamispeaker.client.audio.AudioPlayer
import com.yamispeaker.client.audio.AudioStats
import com.yamispeaker.client.audio.OpusDecoderJNI
import com.yamispeaker.client.network.DiscoveryService
import com.yamispeaker.client.network.UdpReceiver
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val audioPlayer = AudioPlayer()
    private val decoder = OpusDecoderJNI()
    private val udpReceiver = UdpReceiver(audioPlayer, decoder)

    private var laptopIp by mutableStateOf("")
    private var started by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)

    private fun notifyLaptopDirect(ip: String) {
        try {
            val s = DatagramSocket()
            s.send(DatagramPacket("YAMISPEAKER_READY".toByteArray(), 17, InetAddress.getByName(ip), 5001))
            s.close()
        } catch (_: Exception) { }
    }

    private fun getLocalIp(): String {
        NetworkInterface.getNetworkInterfaces().toList().forEach { iface ->
            iface.inetAddresses.toList().forEach { addr ->
                if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false)
                    return addr.hostAddress!!
            }
        }
        return "Unknown"
    }

    private val qrScanner = registerForActivityResult(ScanContract()) { result ->
        try {
            if (result.contents != null) {
                laptopIp = result.contents
                notifyLaptopDirect(laptopIp)
                DiscoveryService.ready = true
                DiscoveryService.sendReady(laptopIp)
                audioPlayer.start()
                udpReceiver.start()
                started = true
            }
        } catch (_: Exception) { }
    }

    private fun disconnect() {
        udpReceiver.stop()
        audioPlayer.stop()
        AudioStats.reset()
        started = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPrefs.init(this)
        DiscoveryService.start(getLocalIp())

        setContent {
            val accent = when (AppPrefs.themeColor) {
                ThemeColor.GREEN -> Color(0xFF00E676)
                ThemeColor.BLUE -> Color(0xFF58A6FF)
                ThemeColor.PURPLE -> Color(0xFFBB86FC)
            }
            val accentDim = when (AppPrefs.themeColor) {
                ThemeColor.GREEN -> Color(0xFF1B8A4C)
                ThemeColor.BLUE -> Color(0xFF1F6FEB)
                ThemeColor.PURPLE -> Color(0xFF6A3E9C)
            }
            val bg = Color(0xFF0A0A0A)
            val card = Color(0xFF141414)
            val textPri = Color(0xFFF5F5F5)
            val textSec = Color(0xFF6B6B6B)
            val border = Color(0xFF222222)

            Surface(Modifier.fillMaxSize(), color = bg) {
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // ── Top bar ──
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(started, accent, textSec)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (started) "LISTENING" else "STANDBY",
                                color = if (started) accent else textSec,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(":5000", color = textSec, fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(44.dp).clickable { showSettings = true }, contentAlignment = Alignment.Center) {
                                Text("⚙", fontSize = 28.sp, color = textSec)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        val hpLabel = deviceLabel(getLocalIp(), android.os.Build.MODEL, AppPrefs.displayMode)
                        Text(hpLabel, color = textPri, fontSize = 13.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (laptopIp.isNotEmpty()) {
                            val pcLabel = deviceLabel(laptopIp, DiscoveryService.laptopHostname.ifEmpty { laptopIp }, AppPrefs.displayMode)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("← ", color = textSec, fontSize = 13.sp)
                                Text(pcLabel, color = accent, fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    // ── Center ──
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (started && DiscoveryService.currentAudio.isNotEmpty()) {
                            Text(DiscoveryService.currentAudio, color = textPri,
                                fontSize = 11.sp, maxLines = 2)
                            Spacer(Modifier.height(6.dp))
                        }
                        AudioVisualizer(started, accent, border)
                        Spacer(Modifier.height(24.dp))
                        if (started) StatsGrid(accent, textSec) else
                            Text("Scan QR untuk memulai", color = textSec, fontSize = 12.sp)
                    }

                    // ── Bottom ──
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (started) {
                            Button(onClick = { disconnect() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0000))
                            ) { Text("DISCONNECT", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFF5252)) }
                        } else {
                            Button(onClick = {
                                val opts = ScanOptions().apply {
                                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    setPrompt("Scan QR dari terminal laptop")
                                    setBeepEnabled(false)
                                    setOrientationLocked(true)
                                }
                                qrScanner.launch(opts)
                            }, modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentDim)
                            ) { Text("SCAN QR", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White) }
                        }
                        if (laptopIp.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(laptopIp, color = accent, fontSize = 11.sp)
                        }
                    }
                }
            }
            if (showSettings) SettingsSheet(accent, textPri, textSec, card, border) { showSettings = false }
        }
    }

    override fun onDestroy() {
        DiscoveryService.stop()
        udpReceiver.stop()
        audioPlayer.stop()
        audioPlayer.release()
        decoder.release()
        super.onDestroy()
    }
}

private fun deviceLabel(ip: String, name: String, mode: DisplayMode) = when (mode) {
    DisplayMode.IP -> ip
    DisplayMode.HOSTNAME -> name
    DisplayMode.BOTH -> "$name • $ip"
}

@Composable
fun SettingsSheet(accent: Color, textPri: Color, textSec: Color, card: Color, border: Color, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss))
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = card,
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Pengaturan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPri)
                        Spacer(Modifier.weight(1f))
                        Text("✕", fontSize = 20.sp, color = textSec,
                            modifier = Modifier.size(44.dp).clickable(onClick = onDismiss),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Spacer(Modifier.height(20.dp))

                    Text("Tampilan", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = textSec)
                    Spacer(Modifier.height(4.dp))
                    DisplayMode.entries.forEach { m ->
                        Row(Modifier.fillMaxWidth()
                            .clickable { AppPrefs.displayMode = m }
                            .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = AppPrefs.displayMode == m,
                                onClick = { AppPrefs.displayMode = m },
                                colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = textSec))
                            Spacer(Modifier.width(8.dp))
                            Text(when (m) {
                                DisplayMode.IP -> "Alamat IP"
                                DisplayMode.HOSTNAME -> "Nama perangkat"
                                DisplayMode.BOTH -> "IP + Nama perangkat"
                            }, fontSize = 13.sp, color = textPri)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = border)
                    Spacer(Modifier.height(16.dp))

                    Text("Warna aksen", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = textSec)
                    Spacer(Modifier.height(4.dp))
                    listOf(
                        ThemeColor.GREEN to Color(0xFF00E676),
                        ThemeColor.BLUE to Color(0xFF58A6FF),
                        ThemeColor.PURPLE to Color(0xFFBB86FC),
                    ).forEach { (m, c) ->
                        Row(Modifier.fillMaxWidth()
                            .clickable { AppPrefs.themeColor = m }
                            .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(18.dp)) {
                                Canvas(Modifier.size(18.dp)) {
                                    drawCircle(color = c, radius = size.minDimension / 2)
                                    if (AppPrefs.themeColor == m)
                                        drawCircle(color = Color.White, radius = 4.dp.toPx())
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(when (m) {
                                ThemeColor.GREEN -> "Hijau"
                                ThemeColor.BLUE -> "Biru"
                                ThemeColor.PURPLE -> "Ungu"
                            }, fontSize = 13.sp, color = textPri)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun StatusDot(active: Boolean, accent: Color, textSec: Color) {
    val alpha by animateFloatAsState(targetValue = if (active) 1f else 0.35f, animationSpec = tween(600), label = "dot")
    val c = if (active) accent else textSec
    Box(Modifier.width(8.dp).height(8.dp)) {
        Canvas(Modifier.fillMaxSize()) { drawCircle(color = c, radius = size.minDimension / 2, alpha = alpha) }
    }
}

@Composable
fun AudioVisualizer(active: Boolean, accent: Color, border: Color) {
    val barCount = 32
    var heights by remember { mutableStateOf(FloatArray(barCount) { 0.08f }) }
    LaunchedEffect(active) {
        if (!active) { heights = FloatArray(barCount) { 0.08f }; return@LaunchedEffect }
        while (true) {
            val next = FloatArray(barCount)
            for (i in 0 until barCount) next[i] = 0.15f + 0.45f * abs(sin(i.toFloat() / barCount * 3.14f * 4)) + Random.nextFloat() * 0.35f
            heights = next; delay(120)
        }
    }
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val gap = 3.dp.toPx(); val bw = (size.width - gap * (barCount - 1)) / barCount; val mid = size.height / 2f
        heights.forEachIndexed { i, h ->
            val bh = (size.height * h).coerceAtMost(size.height * 0.85f)
            drawRoundRect(color = if (active) accent else border, topLeft = Offset(i * (bw + gap), mid - bh / 2f), size = Size(bw, bh), cornerRadius = CornerRadius(bw / 2))
        }
    }
}

@Composable
fun StatsGrid(accent: Color, textSec: Color) {
    var pk by remember { mutableStateOf(0) }; var bt by remember { mutableStateOf(0L) }; var st by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { pk = AudioStats.packetCount; bt = AudioStats.byteCount; st = AudioStats.startTime; delay(500) } }
    val el = if (st > 0) (System.currentTimeMillis() - st) / 1000 else 0
    Row(Modifier.fillMaxWidth()) {
        listOf(formatNum(pk) to "PACKETS", "%.1fMB".format(bt / (1024.0 * 1024.0)) to "DATA",
            (if (el > 0) "%.0fk".format(bt * 8.0 / el / 1000.0) else "0k") to "BITRATE",
            "%02d:%02d".format(el / 60, el % 60) to "DURATION").forEach { (v, l) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(v, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(l, color = textSec, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun formatNum(n: Int) = if (n < 1000) n.toString() else "${n / 1000}.${(n % 1000) / 100}k"
