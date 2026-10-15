# TempMail Gratis (Android)

Aplikasi Android native sederhana untuk email sementara, tema gelap.

## Fitur
- Membuat alamat email dari API publik 1secmail.
- Menyalin alamat ke clipboard.
- Inbox dengan pemeriksaan otomatis tiap 15 detik dan tombol refresh.
- Membuka dan membaca isi pesan.
- Penghitung waktu sesi lokal 60 menit (indikator lokal, bukan jaminan masa berlaku provider).

## Build APK
Prasyarat: Android Studio (disarankan) atau JDK + Android SDK + Gradle.
1. Buka folder proyek `TempMailGratis` di Android Studio.
2. Tunggu Gradle sync.
3. Pilih **Build > Build APK(s)**.
4. APK debug biasanya berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Catatan
- Menggunakan API publik 1secmail tanpa jaminan uptime/privasi.
- Inbox email sementara dapat bersifat publik; jangan gunakan untuk akun penting, reset password, atau data pribadi.
- Timer 60 menit di aplikasi hanya penghitung sesi lokal. Masa hidup email sebenarnya mengikuti provider.
