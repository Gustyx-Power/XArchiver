# Catatan Rilis (Changelog)

Semua perubahan penting pada XArchiver akan didokumentasikan dalam berkas ini.

## [2.1.0] - 2026-10-03

### Ditambahkan - Berkas Terkini & Pelacakan Aktivitas
- **Pengelola Berkas Terkini**: Menambahkan `RecentManager` untuk melacak arsip dan berkas yang baru diakses, diubah, dan diekstrak.
- **Tab Berkas Terkini Khusus**: Tab bawaan di Layar Beranda dengan pengelompokan kategori, metadata berkas, dan aksi akses cepat.
- **Animasi Teks Berjalan (Marquee)**: Mengimplementasikan animasi teks berjalan untuk nama berkas panjang dan judul kartu yang melebihi batas.

### Ditambahkan - Antarmuka Estetika MIUIX / HyperOS
- **Integrasi MIUIX KMP**: Mengintegrasikan komponen desain MIUIX Kotlin Multiplatform (`miuix-ui`, `miuix-blur`, `miuix-icons` v0.9.3).
- **Layar Beranda Bergaya HyperOS (`MiuixHomeScreen`)**: Menampilkan kartu squircle, header blur tekstur real-time, dan pil navigasi mengambang modern.
- **Pengaturan Bergaya HyperOS (`MiuixSettingsScreen`)**: Kartu pengaturan terkelompok, sakelar interaktif yang halus, dan ikon bertema.
- **Layar Tentang Bergaya "Tentang Ponsel" HyperOS (`MiuixAboutScreen`)**: Kartu identitas aplikasi mendalam, info cepat penyimpanan, dan navigasi spesifikasi.
- **Layar Spesifikasi Aplikasi Khusus**: Halaman mandiri untuk mode MIUIX (`MiuixAppSpecsScreen`) dan Material 3 (`AppSpecsScreen`) yang menampilkan arsitektur aplikasi, mesin pengarsip, framework UI, dan parameter sistem.
- **Layar Format yang Didukung Khusus**: Halaman katalog mandiri (`MiuixSupportedFormatsScreen` dan `SupportedFormatsScreen`) yang merinci tipe arsip, streaming berkas tunggal, spesifikasi pengemasan, dan lencana kapabilitas.

### Ditambahkan - Layar Catatan Rilis Dalam Aplikasi
- **Penampil Catatan Rilis Native**: Menambahkan `ChangelogScreen` dan `MiuixChangelogScreen` yang dapat diakses langsung dari bilah atas di sebelah ikon pengaturan.
- **Tipografi Proporsional Penuh**: Tata letak lebar penuh yang rapi untuk setiap item rilis, lencana kategori mikro, pil filter versi, aksi salin catatan rilis, dan wadah sorotan pembaruan.

### Ditambahkan - Deteksi Otomatis Perangkat Cerdas
- **Peralihan Otomatis Xiaomi / Redmi / Poco**: Pengaktifan otomatis mode UI MIUIX pada perangkat keluarga Xiaomi yang menjalankan MIUI atau Xiaomi HyperOS.
- **Deteksi Multi-Tahap**: Memeriksa identitas merek/manufaktur perangkat keras, properti sistem MIUI/HyperOS, dan introspeksi framework.
- **Prioritas Preferensi Pengguna**: Sakelar manual di Pengaturan tetap dihormati dan disimpan secara persisten di DataStore.

### Diubah & Dimodernisasi
- **Peningkatan Minimum SDK**: Menaikkan `minSdk` dari Android 8.0 (API 26) ke Android 10 (API 29) di seluruh modul aplikasi dan modul core (`core-storage`, `core-archive`, `core-extract`, `core-root`).
- **Kompatibilitas Manifest Merger**: Menambahkan konfigurasi `tools:overrideLibrary` untuk pustaka MIUIX KMP guna memastikan eksekusi runtime lancar di Android 10+.
- **Lokalisasi Penuh (Bahasa Indonesia & Bahasa Inggris)**: Lokalisasi berbasis resource lengkap (`strings.xml` dan `values-in/strings.xml`) untuk seluruh layar Tentang, Spesifikasi, Format yang Didukung, dan Catatan Rilis.
- **Peningkatan Versi Aplikasi**: Naik ke versi `2.1.0` (versionCode `2`).

---

## [2.0.0] - 2026-08-19

### Ditambahkan - Hak Akses Istimewa & Integrasi Root
- **Integrasi Shizuku**: Menambahkan dukungan untuk Shizuku API (v13+) melalui `dev.rikka.shizuku` untuk operasi berkas berhak istimewa tanpa root.
- **Layanan Shell Root**: Mengintegrasikan `com.github.topjohnwu.libsu:core` v6.0.0 untuk kemampuan superuser (su) penuh.
- **Operasi Berkas Berhak Istimewa**: Mengimplementasikan `ShizukuFileService` dan `RootFileService` dengan mekanisme fallback otomatis di `FileOperationsManager`.
- **Pengeditan Teks Berhak Istimewa**: Memungkinkan membaca dan menulis berkas sistem yang dilindungi melalui `TextEditorScreen`.
- **UI Akses Istimewa**: Menambahkan indikator status Shizuku khusus, panduan konfigurasi, dan sakelar root di Pengaturan.

### Ditambahkan - Manajemen Penyimpanan & Perombakan Penjelajah Berkas
- **Pager Horizontal Multi-Penyimpanan**: Desain ulang bagian penyimpanan beranda yang mendukung Penyimpanan Internal, Kartu SD, USB OTG, dan partisi Root.
- **Deteksi Penyimpanan Andal**: Mengimplementasikan pemindaian volume Android `StorageManager` dengan polling otomatis dan penyegaran untuk drive OTG.
- **Diagnostik Sistem Berkas**: Deteksi jenis sistem berkas real-time (F2FS, ext4, FAT32) dan metrik kesehatan penyimpanan.
- **Peningkatan Penjelajah Berkas**: Mode pemilihan multi-berkas (`SelectionManager`), ekstraksi massal, salin, pindah, hapus, dan ganti nama.
- **Pratinjau Media & Lencana APK**: Integrasi dekode thumbnail gambar dan video menggunakan Coil; mengekstrak ikon aplikasi untuk berkas APK.
- **Resolusi Konflik**: Menambahkan dialog tabrakan berkas untuk penanganan berkas duplikat saat ekstraksi atau penyalinan.

### Ditambahkan - Mesin Payload Native & Kapabilitas Arsip
- **Mesin Payload Rust Native**: Mengintegrasikan modul JNI native Rust `payload-parser` untuk ekstraksi cepat gambar sistem Android OTA.
- **`PayloadViewerScreen`**: Penjelajah partisi interaktif dengan ekstraksi individual/massal dan verifikasi integritas SHA256.
- **`ArchiveReader` Terpadu**: Arsitektur pembaca arsip berbasis stream yang terstandarisasi.
- **Progres Ekstraksi Streaming**: Pembaruan progres ekstraksi real-time tingkat byte.
- **Ekstraksi Selektif**: Ekstraksi dan inspeksi penyimpanan sementara tanpa mendekompresi seluruh arsip.

### Ditambahkan - UI/UX & Notifikasi Dynamic Island
- **Desain Material 3 Ekspresif**: Perombakan palet warna, bentuk squircle kustom, dan mode gelap AMOLED murni.
- **Notifikasi Dynamic Island**: Mengganti snackbar bawaan dengan banner notifikasi pil beranimasi (`DynamicIslandNotificationHost`).
- **Pengaturan & Layar Tentang Modern**: Desain ulang Pengaturan layar penuh dan metadata versi dinamis di layar Tentang.
- **Alur Izin Terarah**: Orientasi awal yang disederhanakan dengan `SetupScreen` berbasis izin.
- **Internasionalisasi String**: Standardisasi resource string untuk dukungan multi-bahasa.

---

## [1.1-Release] - 2026-02-26

### Ditambahkan - Penampil Payload.bin
- **Dukungan Penuh Android OTA Payload.bin**
  - Melihat dan mengekstrak gambar sistem Android OTA.
  - Menjelajahi daftar partisi (system, vendor, boot, product, dll.).
  - Mengekstrak partisi individual menjadi berkas .img.
  - Mengekstrak seluruh partisi sekaligus.
  - Pelacakan progres ekstraksi real-time.
  - Verifikasi hash SHA256 setelah ekstraksi.
  
- **Parsing Manifest Protobuf**
  - Membaca manifest Android OTA menggunakan Protocol Buffers.
  - Menampilkan informasi partisi (nama, ukuran, tipe kompresi).
  - Mendukung berbagai tipe kompresi (XZ, BZIP2, tanpa kompresi).
  - Parsing operasi tingkat blok.
  
- **Dekompresi Tingkat Lanjut**
  - Dukungan dekompresi XZ/LZMA.
  - Dukungan dekompresi BZIP2.
  - Dekompresi langsung saat membaca (tanpa berkas sementara).
  - Dekompresi streaming yang hemat memori.

### Ditambahkan - Manajemen Arsip
- Kelas `ArchiveManager` baru untuk operasi arsip terpusat.
- Pelacakan progres ekstraksi real-time dengan akurasi tingkat byte.
- Dukungan untuk berkas arsip berukuran besar (3GB+) dengan pembaruan progres yang lancar.
- Peningkatan fungsi pratinjau arsip.
- Validasi ukuran berkas sebelum dibuka di editor teks (batas 10MB).
- Deteksi tipe berkas biner untuk mencegah pembukaan berkas non-teks.

### Ditambahkan - Refaktor Kode
- Membuat `ExplorerViewModel` untuk pemisahan tanggung jawab yang lebih baik.
- Membuat utilitas `FileTypeDetector` untuk deteksi tipe berkas.
- Membuat `FileActionHandler` untuk penanganan klik berkas terpusat.
- Modularisasi `ExplorerScreen` (dikurangi dari 1304 baris).
- Peningkatan kemudahan pemeliharaan dan pengujian kode.

### Diperbaiki
- **Kritis**: Memperbaiki crash aplikasi saat mempratinjau berkas terkompresi (ZIP, TAR, GZ) dengan memperbaiki pelanggaran invarian Flow menggunakan `flowOn(Dispatchers.IO)`.
- **Kritis**: Memperbaiki OutOfMemoryError saat membuka berkas biner besar (misal payload.bin) dengan batas ukuran 10MB pada editor teks.
- **Utama**: Memperbaiki bilah progres ekstraksi yang sempat terhenti di 0% melalui ekstraksi satu lintasan (single-pass) dengan pelacakan tingkat byte.
- Memperbaiki antarmuka adaptor arsip untuk mendukung operasi ekstraksi massal.
- Memperbaiki masalah memori saat melihat entri arsip berukuran besar.
- Memperbaiki error kompilasi pada ExplorerViewModel (ketidakcocokan tipe).
- Memperbaiki konflik impor FileItem antara paket core dan storage.

### Diubah
- Peningkatan performa ekstraksi untuk arsip besar.
- Pembaruan progres sekarang dipancarkan setiap 1MB untuk responsivitas UI yang optimal.
- Refaktor ekstraksi arsip menggunakan pelaporan progres berbasis Flow.
- Editor teks memvalidasi ukuran dan tipe berkas sebelum memuat.
- Output ekstraksi bawaan: `Downloads/XArchiver/extracted/`.

### Peningkatan Teknis
- Menambahkan dukungan Protocol Buffers untuk parsing manifest Android OTA.
- Mengintegrasikan `org.tukaani:xz:1.9` untuk dekompresi XZ.
- Mengintegrasikan Apache Commons Compress untuk dekompresi BZIP2.
- Menggunakan operator `flowOn` untuk pergantian konteks coroutine yang tepat.
- Menghitung progres berbasis byte untuk pelacakan yang akurat.
- Peningkatan penanganan error pada operasi arsip.

---

## [1.0] - Rilis Awal

### Ditambahkan
- Tampilan dan ekstraksi arsip dasar.
- Dukungan format ZIP, TAR, GZ, TGZ.
- Penjelajah berkas terintegrasi arsip.
- Antarmuka Material Design 3.
