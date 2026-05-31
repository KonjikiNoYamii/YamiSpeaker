package com.yamispeaker.client.audio

class OpusDecoderJNI {

    private var nativePtr: Long = 0

    init {
        nativePtr = create()
        if (nativePtr == 0L) {
            throw RuntimeException("OpusDecoder init failed")
        }
    }

    // decode opus packet → PCM short array, return sample count
    fun decode(packet: ByteArray, packetLen: Int, pcmOut: ShortArray): Int {
        return decode(nativePtr, packet, packetLen, pcmOut)
    }

    fun release() {
        if (nativePtr != 0L) {
            destroy(nativePtr)
            nativePtr = 0
        }
    }

    private external fun create(): Long
    private external fun decode(nativePtr: Long, packet: ByteArray, packetLen: Int, pcmOut: ShortArray): Int
    private external fun destroy(nativePtr: Long)

    companion object {
        init {
            System.loadLibrary("opus_wrapper")
        }
    }
}
