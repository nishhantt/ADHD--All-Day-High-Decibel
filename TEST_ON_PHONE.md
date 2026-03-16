# How to test the app on your phone

## 1. Build the APK

**Option A – Android Studio (easiest)**

1. Open the `musicPlayer` folder in Android Studio.
2. Wait for Gradle sync.
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. APK path: `app/build/outputs/apk/debug/app-debug.apk`.

**Option B – Docker (no Android Studio)**

From the project root in PowerShell:

```powershell
.\build.ps1
```

APK path: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 2. Install on your Samsung A21s

**Option A – USB (ADB)**

1. On the phone: **Settings → About phone** → tap **Build number** 7 times to enable Developer options.
2. **Settings → Developer options** → turn on **USB debugging**.
3. Connect the phone with a USB cable.
4. On the phone, accept “Allow USB debugging” if prompted.
5. On the PC (in the project folder or where you have the APK):

   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

   (`-r` replaces an existing install.)

**Option B – Copy APK to phone**

1. Copy `app-debug.apk` to the phone (USB file copy, cloud, email, etc.).
2. On the phone, open **My Files** (or Files), find `app-debug.apk`, and tap it.
3. If asked, allow **Install unknown apps** for that app (e.g. My Files or Chrome).
4. Tap **Install** and then **Open**.

---

## 3. First run

1. Open **MusicPlayer** on the phone.
2. If Android 13+ asks for **Notifications**, allow it (needed for the playback notification).
3. **Search:** Needs a YouTube Data API key. If you don’t have one, search will return no results (no error message). To enable search:
   - Get an API key from [Google Cloud Console](https://console.cloud.google.com/) (YouTube Data API v3).
   - In the **project root** (on your PC), create or edit `local.properties` and add:
     ```properties
     YOUTUBE_API_KEY=your_key_here
     ```
   - Rebuild the APK and reinstall.
4. **Playback:** Even without search, you can test by opening the app and navigating to the player (e.g. if you had a direct “player” route). With the current dev setup, **any** selected “song” plays the **same sample MP3** (see `DevStreamExtractor`).

---

## 4. What to test

- **Play / Pause** – in-app and from the notification.
- **Next / Previous** – in-app and from the notification (add at least 2 “songs” by searching and tapping two results).
- **Seek** – drag the progress bar.
- **Repeat** – tap repeat, then tap Next in-app (should replay same track); Next from notification does **not** respect repeat.
- **Background** – play a song, press Home or lock the phone; playback and notification should continue.
- **Bluetooth / headset** – play/pause and next/prev from headset or car (if available).

---

## 5. Troubleshooting

- **“App not installed”** – Uninstall any existing MusicPlayer, then install again. Or use `adb install -r ...`.
- **No sound** – Check phone volume and that the notification is showing (so the foreground service is running).
- **Search shows nothing** – Add `YOUTUBE_API_KEY` to `local.properties` and rebuild; or ignore search and test playback with the sample stream only.
- **ADB not found** – Install [Android SDK platform-tools](https://developer.android.com/studio/releases/platform-tools) or use Android Studio (it includes `adb`).
