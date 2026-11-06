# Countdown Lock Screen & Live Wallpaper (Project Decline)

A native Android application, lock screen live wallpaper, and keyguard widget designed for tracking deadlines and milestones directly from your phone's lock screen without needing to unlock the device.

---

## 🌟 Key Features

### 1. Motionless 2D Lock Screen Wallpaper
- **Zero Motion / Fixed Position**: Sits completely anchored at a fixed position—no shaking, tilting, or sensor-based drift.
- **Visual Parity**: Renders the exact UI from the app with high-contrast glassmorphic panels over 2D scenic backdrops (Alpine Sunset and Misty Forest).
- **Spoken Word Clock**: Formats the current time in phrased natural language (e.g., *"IT IS A QUARTER PAST ELEVEN IN THE MORNING"*).
- **Live 2D Countdown**: Prominently displays large monospace numbers for **Days Left** and **Hours Left** with second-precision live updates.
- **T-Minus Milestones**: Shows upcoming critical project deliverables and countdown days.
- **Interactive Gestures**:
  - **Single Tap**: Toggles between scenic wallpaper themes (Alpine Sunset / Misty Forest).
  - **Double Tap**: Instantly launches the full interactive app over the lock screen.

### 2. Lock Screen / Keyguard App Widget
- Native Android `AppWidgetProvider` (`CountdownWidgetProvider`) compatible with home screen and keyguard/lock screen placements.
- Displays live countdown to deadline with auto-updating `TextClock`.

### 3. Fullscreen Interactive App
- Runs full-screen with edge-to-edge immersive view.
- Configured with `showWhenLocked="true"` and `turnScreenOn="true"` for seamless lock screen interaction.

---

## 📱 Pre-built Release APK

The signed, production-ready APK is available in the [`dist/`](./dist) directory:
- **File**: [`dist/CountdownLockScreen-release.apk`](./dist/CountdownLockScreen-release.apk)
- **Signing**: APK Signature Scheme v2 & v3 (RSA-2048)
- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 15 (API 35)

---

## 🛠️ Project Structure

```text
CountdownApp/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── assets/             # Offline HTML5 assets, Tailwind CSS, icons
│   │       ├── java/
│   │       │   └── com/countdown/lockscreen/
│   │       │       ├── MainActivity.java             # Immersive lock-screen activity
│   │       │       ├── CountdownWidgetProvider.java  # Keyguard app widget
│   │       │       └── CountdownWallpaperService.java # 2D motionless live wallpaper
│   │       ├── res/                # XML layouts, drawables, high-res mipmaps
│   │       └── AndroidManifest.xml # Permissions, receivers, service declarations
│   └── build.gradle.kts
├── dist/
│   └── CountdownLockScreen-release.apk # Production signed APK
├── build_release_apk.ps1           # Automated standalone build pipeline
├── release.keystore                # Production release keystore
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Building from Source

### Prerequisites
- **Java Development Kit (JDK 17+)**
- **Android SDK** (Build-Tools 35.0.0+ / 36.0.0, Platform API 34+)

### Standalone Build (PowerShell)
To compile, package, zipalign, and sign the release APK without needing Android Studio Gradle sync:
```powershell
powershell -ExecutionPolicy Bypass -File .\build_release_apk.ps1
```
The output APK will be generated at `dist/CountdownLockScreen-release.apk`.

---

## 🔒 Security & Privacy
- **Cleartext Traffic Disabled**: Strict HTTPS enforcement via `network_security_config.xml`.
- **Zero Third-party Trackers**: All styling assets (`tailwind.js`) and bitmaps are bundled locally offline.
- **Signed with v2/v3**: Prevents Play Protect installation warnings.
