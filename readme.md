# YamiSpeaker — Android Client

## Arsitektur

```
UdpReceiver (thread)
  │
  ├── DatagramSocket(5000)      ← terima UDP dari laptop
  ├── loop:
  │     ├── socket.receive()
  │     └── audioPlayer.write() → langsung ke AudioTrack
  └── close socket

AudioPlayer
  │
  └── AudioTrack(MODE_STREAM)   → mainkan PCM ke speaker HP
```

## Alur Data

```
Laptop ──UDP──> Android (:5000) ──AudioTrack──> Speaker HP
```

## Cara Pakai

1. Install APK di HP
2. Buka app → lihat IP yang muncul
3. Tap **START** → "Listening :5000"
4. Kirim audio dari laptop (server C++ atau tools lain)

## Perubahan

| File | Perubahan |
|---|---|
| `MainActivity.kt` | Tambah `getLocalIp()` — tampilkan IP HP di UI. Wiring: `AudioPlayer`(langsung) → `UdpReceiver` |
| `AudioPlayer.kt` | **Architecture change**: hapus thread consumer + JitterBuffer. `write()` langsung ke AudioTrack via thread UDP. `start()` hanya panggil `play()`. Buffer dinaikkan ke `minBufferSize * 12` (~500ms) untuk serap jitter. |
| `UdpReceiver.kt` | **Architecture change**: terima `AudioPlayer` langsung (bukan `JitterBuffer`). `receive()` → `write()` dalam thread UDP. Thread priority dinaikkan ke `THREAD_PRIORITY_URGENT_AUDIO`. |
| `JitterBuffer.kt` | **Dihapus** — tidak diperlukan karena AudioTrack internal buffer sudah handle jitter alami. |

## Kenapa Arsitektur Berubah

Sebelum:

```
UDP → JitterBuffer → Thread consumer → AudioTrack
```

Sekarang:

```
UDP → AudioTrack (langsung)
```

Alasan:
- JitterBuffer + thread consumer bikin **underrun** karena ada gap saat buffer kosong
- `AudioTrack.write()` MODE_STREAM otomatis **block** kalau buffer internal penuh — ini jadi natural backpressure
- Lebih sederhana, latency lebih rendah, glitch hilang
