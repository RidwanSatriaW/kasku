# Kasku (Android)

Aplikasi catatan keuangan pribadi. Kotlin + Jetpack Compose + Room (SQLite). Berjalan offline, tanpa server.

## Cara menjalankan
1. Buka folder ini di Android Studio (Koala 2024.1 atau lebih baru).
2. Tunggu Gradle sync selesai (butuh internet untuk mengunduh dependensi). Android Studio akan membuat Gradle wrapper sendiri.
3. Pilih emulator atau HP (USB debugging aktif), lalu tekan Run.
4. Untuk APK: Build > Build Bundle(s) / APK(s) > Build APK(s).

## Fitur
- Dompet (tambah, ubah, hapus) dengan saldo awal; default: Tunai, Bank, E-wallet, Tabungan
- Transaksi masuk, keluar, transfer antar dompet (tambah, ubah, hapus, cari, filter)
- Beranda: total saldo (bisa disembunyikan), ringkasan bulanan, aman dibelanjakan per hari, proyeksi, donut kategori, transaksi akan datang, progres target
- Analisis: skor kesehatan keuangan, aturan 50/30/20, wawasan, grafik 6 bulan
- Rencana: budget per kategori, transaksi berulang (termasuk transfer otomatis), target tabungan (setor, tarik, riwayat, tutup, arsip), daftar belanja
- Scan struk: OCR di HP (gratis, offline, tanpa API key). API key Claude di tab Dompet bersifat opsional untuk hasil lebih akurat
- Ekspor CSV, backup JSON, pulihkan dari backup (format sama dengan versi web)
- Tiga tema warna (Aurora, Zamrud, Senja), masing-masing punya mode terang dan gelap; ganti di tab Dompet > Tema warna dan Mode

## Database
SQLite lewat Room. File: `/data/data/id.kasku.app/databases/kasku.db`
Tabel: `wallets`, `transactions`, `budgets`.
Untuk melihat isinya: Android Studio > View > Tool Windows > App Inspection > Database Inspector.

## Catatan
- Database naik ke versi 2 dengan migrasi otomatis dari tahap 1, data lama tetap aman.
- Data demo dari versi web tidak dibawa.

## Build tanpa Android Studio
- GitHub Actions: unggah folder ini ke repo GitHub, buka tab Actions > Build APK, lalu unduh artifact `Kasku-debug-apk`.
- Command line: pasang JDK 17, Android command-line tools (`sdkmanager "platforms;android-34" "build-tools;34.0.0"`), dan Gradle 8.7, lalu jalankan `gradle assembleDebug`. APK ada di `app/build/outputs/apk/debug/app-debug.apk`.
