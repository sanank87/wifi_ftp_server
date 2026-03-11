# 📡 WiFi FTP Server — Android Studio Project

Full Kotlin + Jetpack Compose Android app with a **real Apache FTPServer** backend.

---

## 🚀 Open in Android Studio (3 steps)

### Step 1 — Unzip
```
Unzip WifiFTPServer-Android.zip anywhere on your machine.
```

### Step 2 — Open in Android Studio
```
File → Open → select the WifiFTPServer-Android folder → OK
```
Android Studio will auto-sync Gradle. Wait for "Sync finished" in the status bar (~1-2 min first time).

### Step 3 — Run
```
Plug in your Android phone (or start an emulator) → Click ▶ Run
```
That's it. The APK is built and installed automatically.

---

## 📦 Build a release APK

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

Or via terminal:
```bash
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease
# Requires signing config — see "Signing" section below
```

---

## 🔧 Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 (bundled with Android Studio) |
| Android SDK | API 26+ (minSdk) |
| Gradle | 8.7 (downloaded automatically) |
| Kotlin | 2.0.0 |

---

## 📁 Project Structure

```
app/src/main/
├── java/com/wififtp/server/
│   ├── MainActivity.kt              # Entry point, permission handling
│   ├── WifiFtpApp.kt                # Hilt application class
│   ├── AppModule.kt                 # Hilt DI module (Room, DAO)
│   ├── data/
│   │   ├── Models.kt                # Data classes, Room entities, DAO
│   │   └── SettingsRepository.kt    # DataStore settings persistence
│   ├── service/
│   │   ├── FtpEngine.kt             # ⭐ Apache FTPServer integration
│   │   └── FtpForegroundService.kt  # Background foreground service + BootReceiver
│   ├── ui/
│   │   ├── MainViewModel.kt         # Central ViewModel driving all screens
│   │   ├── Navigation.kt            # Bottom nav + NavHost
│   │   ├── components/
│   │   │   └── Components.kt        # Shared Compose components
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt        # Dashboard — toggle, IP, QR, stats
│   │   │   ├── FileManagerScreen.kt # File browser with sort + search
│   │   │   └── OtherScreens.kt      # Devices, Activity, Settings screens
│   │   └── theme/
│   │       ├── Theme.kt             # Material 3 dark theme
│   │       └── Typography.kt        # Type scale
│   └── util/
│       ├── NetworkUtils.kt          # WiFi IP detection
│       └── QrUtils.kt               # ZXing QR generation
├── res/
│   ├── mipmap-*/ic_launcher*.png    # App icons (all densities)
│   ├── values/strings.xml
│   └── values/themes.xml
└── AndroidManifest.xml
```

---

## ⭐ Features

### FTP Server (Apache FTPServer 1.2.0)
- Full RFC-959 FTP protocol
- PASV and PORT (active/passive) modes
- File upload (STOR), download (RETR), delete (DELE)
- Directory listing (LIST/NLST), navigation (CWD/CDUP)
- mkdir (MKD), rename (RNFR/RNTO)
- Configurable port (default 2121)
- Username/password authentication
- Optional anonymous login
- Event hooks via Ftplet API for transfer logging

### UI
- Material Design 3 dark theme
- One-tap animated server start/stop
- Live IP + port display
- QR code (ZXing) for instant connection
- Copy-to-clipboard FTP URL
- File browser (internal + external storage)
- Connected devices monitor with per-client stats
- Transfer history (Room database)
- Settings with DataStore persistence
- Foreground service with notification
- Badge counts on nav tabs

---

## 🔌 Connect from PC

### FileZilla (Recommended — free)
```
Host: ftp://192.168.x.x
Port: 2121
User: admin
Pass: admin123
```

### Windows Explorer
```
Address bar: ftp://admin:admin123@192.168.x.x:2121
```

### macOS Finder
```
Go → Connect to Server → ftp://192.168.x.x:2121
```

### Command line (any OS)
```bash
ftp -p 192.168.x.x 2121
```

---

## 🏗️ Tech Stack

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose | BOM 2024.08 | UI framework |
| Material 3 | BOM 2024.08 | Design system |
| Hilt | 2.51.1 | Dependency injection |
| Room | 2.6.1 | Transfer log database |
| DataStore | 1.1.1 | Settings persistence |
| Navigation Compose | 2.7.7 | Screen navigation |
| Apache FTPServer | 1.2.0 | Real FTP protocol |
| Apache MINA | 2.2.3 | Network I/O (FTPServer dependency) |
| ZXing | 3.5.3 | QR code generation |
| Kotlin Coroutines | 1.8.1 | Async operations |

---

## 🔐 Signing a Release APK

1. In Android Studio: `Build → Generate Signed Bundle/APK`
2. Create a new keystore or use existing
3. Select APK → Release → Finish

Or add to `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("your-keystore.jks")
        storePassword = "your-store-password"
        keyAlias = "your-key-alias"
        keyPassword = "your-key-password"
    }
}
```

---

## 📋 Android Permissions Used

| Permission | Reason |
|-----------|--------|
| `INTERNET` | TCP socket for FTP connections |
| `ACCESS_WIFI_STATE` | Get WiFi IP address |
| `READ_MEDIA_*` | Access files on Android 13+ |
| `READ_EXTERNAL_STORAGE` | Access files on Android ≤12 |
| `FOREGROUND_SERVICE` | Keep server running in background |
| `FOREGROUND_SERVICE_DATA_SYNC` | Android 14+ foreground service type |
| `WAKE_LOCK` | Keep CPU awake during transfers |
| `POST_NOTIFICATIONS` | Show server status notification |

---

## 🐛 Troubleshooting

**Gradle sync fails**
→ File → Invalidate Caches → Restart
→ Ensure JDK 17 is set: File → Project Structure → SDK Location

**"Connection refused" from PC**
→ Phone and PC must be on the same WiFi network
→ Try disabling Windows Firewall temporarily
→ Confirm port 2121 is not in use (change in Settings)

**Files not accessible**
→ Grant storage permissions when prompted
→ On Android 11+: Settings → Apps → WiFi FTP Server → Permissions → Files

**App crashes on start**
→ Clean project: Build → Clean Project → Rebuild
