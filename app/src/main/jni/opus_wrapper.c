#include <jni.h>
#include <opus.h>
#include <string.h>

JNIEXPORT jlong JNICALL
Java_com_yamispeaker_client_audio_OpusDecoderJNI_create(JNIEnv* env, jobject thiz) {
    int error;
    OpusDecoder* dec = opus_decoder_create(48000, 2, &error);
    if (error != OPUS_OK || !dec) return 0;
    return (jlong)(intptr_t)dec;
}

JNIEXPORT jint JNICALL
Java_com_yamispeaker_client_audio_OpusDecoderJNI_decode(
    JNIEnv* env, jobject thiz,
    jlong nativePtr,
    jbyteArray packet, jint packetLen,
    jshortArray pcmOut
) {
    OpusDecoder* dec = (OpusDecoder*)(intptr_t)nativePtr;
    if (!dec) return -1;

    jbyte* packetData = (*env)->GetByteArrayElements(env, packet, NULL);
    jshort* pcmData = (*env)->GetShortArrayElements(env, pcmOut, NULL);

    int samples = opus_decode(
        dec,
        (unsigned char*)packetData, packetLen,
        pcmData, 960, 0
    );

    (*env)->ReleaseShortArrayElements(env, pcmOut, pcmData, 0);
    (*env)->ReleaseByteArrayElements(env, packet, packetData, JNI_ABORT);

    return samples;
}

JNIEXPORT void JNICALL
Java_com_yamispeaker_client_audio_OpusDecoderJNI_destroy(JNIEnv* env, jobject thiz, jlong nativePtr) {
    OpusDecoder* dec = (OpusDecoder*)(intptr_t)nativePtr;
    if (dec) opus_decoder_destroy(dec);
}
