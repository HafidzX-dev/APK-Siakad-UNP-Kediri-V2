# 📱 APK SIAKAD UNP Kediri V2

Aplikasi Android sederhana untuk mengakses **SIAKAD Universitas Nusantara PGRI Kediri** tanpa harus berulang kali memasukkan NPM dan password secara manual.

> **Unofficial Android Client for SIAKAD UNP Kediri**

Aplikasi ini bekerja sebagai client Android yang mengakses SIAKAD melalui WebView, dengan fitur penyimpanan kredensial terenkripsi dan autentikasi biometrik untuk mempermudah akses.

---

## 📥 Download APK

### Latest APK

[![Download APK](https://img.shields.io/badge/Download-APK-green?style=for-the-badge\&logo=android)](https://github.com/HafidzX-dev/APK-Siakad-UNP-Kediri-V2/releases/download/untagged-fa7aec7fc4576d26535c/apk_siakad.apk)

**[⬇️ Download `apk_siakad.apk`](https://github.com/HafidzX-dev/APK-Siakad-UNP-Kediri-V2/releases/download/untagged-fa7aec7fc4576d26535c/apk_siakad.apk)**

> ⚠️ Jika link di atas tidak dapat diakses, cek bagian **Releases** repository untuk mendapatkan build terbaru.

---

## ✨ Features

* 🔐 **Biometric Authentication**

  * Mendukung fingerprint / biometric device.
  * Menggunakan Android BiometricPrompt.
  * Jika perangkat tidak mendukung biometric, aplikasi dapat melanjutkan tanpa biometric.

* 💾 **Secure Credential Storage**

  * NPM dan password disimpan menggunakan `EncryptedSharedPreferences`.
  * Menggunakan AES-256 melalui AndroidX Security Crypto.
  * Data login dapat dihapus melalui tombol reset.

* 🚀 **Automatic Login**

  * Setelah kredensial disimpan, aplikasi mencoba mengisi form login SIAKAD secara otomatis.
  * Tidak perlu mengetik NPM dan password setiap membuka aplikasi.

* 🌐 **SIAKAD WebView**

  * Mengakses SIAKAD UNP Kediri secara langsung melalui WebView.
  * Domain `unpkediri.ac.id` tetap berada di dalam aplikasi.
  * Link eksternal dibuka menggunakan browser Android.

* 🔄 **Pull to Refresh**

  * Tarik halaman ke bawah untuk melakukan refresh SIAKAD.

* 📄 **External Document Support**

  * File seperti PDF, DOCX, XLSX, PPTX, ZIP, dan format dokumen lainnya dapat dibuka menggunakan aplikasi eksternal.

* ⚡ **WebView Optimization**

  * Hardware acceleration.
  * DOM storage.
  * WebView cache.
  * Wide viewport.
  * Optimasi scrolling.

* 🧹 **Reset Login**

  * Menghapus kredensial tersimpan.
  * Membersihkan cookie WebView.
  * Memungkinkan pengguna memasukkan akun lain.

---

## 🖥️ Requirements

| Requirement  | Minimum        |
| ------------ | -------------- |
| Android      | Android 7.0+   |
| API Level    | 24+            |
| Internet     | Required       |
| Biometric    | Optional       |
| Architecture | Android Native |
| Language     | Kotlin         |

Aplikasi dikonfigurasi dengan:

* **minSdk:** 24
* **targetSdk:** 37
* **compileSdk:** 37
* **versionCode:** 1
* **versionName:** 1.0
* **Application ID:** `com.unpkediri.apksiakad`

Konfigurasi tersebut berasal dari module Android aplikasi.

---

## 🛠️ Tech Stack

* Kotlin
* Android SDK
* Jetpack Compose
* Material 3
* AndroidX
* WebView
* BiometricPrompt
* AndroidX Security Crypto
* Gradle Kotlin DSL
* SwipeRefreshLayout

Versi dependency utama mengikuti version catalog repository, termasuk Kotlin `2.2.10`, Android Gradle Plugin `9.3.2`, Compose BOM `2026.02.01`, Biometric `1.1.0`, dan Security Crypto `1.1.0`.

---

## 📂 Project Structure

```text
APK-Siakad-UNP-Kediri-V2/
│
├── app/
│   ├── src/
│   │   ├── androidTest/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── unpkediri/
│   │   │   │           └── apksiakad/
│   │   │   │               ├── data/
│   │   │   │               │   └── SecureStorage.kt
│   │   │   │               │
│   │   │   │               ├── ui/
│   │   │   │               │   └── theme/
│   │   │   │               │
│   │   │   │               └── MainActivity.kt
│   │   │   │
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/
│   │
│   └── build.gradle.kts
│
├── gradle/
│   └── libs.versions.toml
│
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

Struktur repository saat ini terdiri dari module `app`, source Android, Gradle wrapper, version catalog, dan konfigurasi Gradle utama.

---

## 🔐 Security

Aplikasi tidak menyimpan NPM dan password menggunakan `SharedPreferences` biasa.

Credential disimpan menggunakan:

```text
EncryptedSharedPreferences
        │
        ├── AES256-SIV
        │      └── Preference Keys
        │
        └── AES256-GCM
               └── Preference Values
```

Master key dibuat menggunakan AndroidX `MasterKey` dengan skema `AES256_GCM`.

Ketika user melakukan reset:

1. Credential lokal dihapus.
2. Credential login dikosongkan.
3. Cookie WebView dihapus.
4. Cookie di-flush.

---

## 🔑 Login Flow

```text
First Launch
     │
     ▼
Input NPM + Password
     │
     ▼
Encrypted Storage
     │
     ▼
Biometric Verification
     │
     ▼
SIAKAD WebView
     │
     ▼
Detect Login Form
     │
     ▼
Inject Saved Credential
     │
     ▼
Submit Login
     │
     ▼
SIAKAD Dashboard
```

Pada startup berikutnya, aplikasi mengambil credential tersimpan secara asynchronous dan menjalankan biometric authentication sebelum menampilkan WebView utama.

---

## 🌐 SIAKAD Endpoint

Aplikasi mengarah ke:

```text
https://siakad2.unpkediri.ac.id/
```

WebView mempertahankan navigasi pada domain `unpkediri.ac.id`, sedangkan link eksternal diarahkan ke browser Android.

---

## 📱 Android Permissions

Aplikasi membutuhkan akses internet:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Tidak ditemukan permission lokasi, kamera, kontak, SMS, atau storage umum pada `AndroidManifest.xml` yang diperiksa.

---

## 🏗️ Build From Source

### 1. Clone repository

```bash
git clone https://github.com/HafidzX-dev/APK-Siakad-UNP-Kediri-V2.git

cd APK-Siakad-UNP-Kediri-V2
```

### 2. Build Debug APK

Linux/macOS:

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

APK biasanya tersedia di:

```text
app/build/outputs/apk/debug/
```

### 3. Build Release

```bash
./gradlew assembleRelease
```

atau Windows:

```powershell
.\gradlew.bat assembleRelease
```

---

## 🧩 Development

Project menggunakan:

```text
Kotlin
    ↓
Jetpack Compose
    ↓
MainActivity
    ↓
WebView
    ↓
SIAKAD UNP Kediri
```

`MainActivity.kt` menangani UI Compose, biometric authentication, credential flow, WebView, automatic login, refresh, navigation, dan error handling.

---

## ⚠️ Disclaimer

Aplikasi ini merupakan **client Android pihak ketiga / unofficial wrapper** untuk mengakses layanan SIAKAD.

Aplikasi ini tidak mengubah database SIAKAD dan tidak menggantikan layanan resmi universitas.

Gunakan akun SIAKAD milik sendiri dan jangan membagikan NPM maupun password kepada pihak lain.

> **Gunakan aplikasi dengan risiko sendiri.**

---

## 👨‍💻 Author

**HafidzX**

GitHub:

[HafidzX-dev](https://github.com/HafidzX-dev)

Repository:

[APK-Siakad-UNP-Kediri-V2](https://github.com/HafidzX-dev/APK-Siakad-UNP-Kediri-V2)

---

## 📄 License

Belum terdapat file license eksplisit pada root repository saat ini.

Jika project memang ingin didistribusikan sebagai open-source, disarankan menambahkan file `LICENSE` sebelum menentukan lisensi resmi project.

---

## ⭐ Support

Jika project ini membantu, kamu bisa memberikan ⭐ pada repository GitHub.

**Enjoy the easier SIAKAD access! 🚀**
