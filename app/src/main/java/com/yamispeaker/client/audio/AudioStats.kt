package com.yamispeaker.client.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AudioStats {
    var packetCount by mutableIntStateOf(0)
    var byteCount by mutableLongStateOf(0L)
    var connected by mutableStateOf(false)
    var startTime by mutableLongStateOf(0L)

    fun reset() {
        packetCount = 0
        byteCount = 0L
        connected = false
        startTime = 0L
    }
}
