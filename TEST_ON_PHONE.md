# How to test Skibidi on your phone

## 1. Build the APK

**Option A – Android Studio (Recommended)**

1. Open the project folder in Android Studio.
2. Wait for Gradle sync to complete.
3. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)** from the top menu.
4. Once finished, click **Locate** in the bottom-right notification or find it at:
   `app/build/outputs/apk/debug/app-debug.apk`

**Option B – Command Line (gradlew)**

From the project root in PowerShell:
```powershell
./gradlew assembleDebug
```
APK path: `app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Install on your Android Phone

**Option A – USB (ADB)**

1. Enable **Developer Options** and **USB Debugging** on your phone (Settings → About phone → Tap Build Number 7 times).
2. Connect your phone via USB.
3. Run this command:
   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

**Option B – Direct Copy**

1. Copy `app-debug.apk` to your phone via USB, Google Drive, or email.
2. Open **Files** (or My Files) on your phone.
3. Tap the APK and select **Install**.
4. If prompted, allow installations from "Unknown sources".

---

## 3. Configuration (Optional)

**Search Backend:**
The search uses the JioSaavn API. If you want to use a specific backend proxy (like your `music-extractor-backend` on Render):
1. In `local.properties`, add:
   ```properties
   EXTRACTOR_BACKEND_URL=https://your-backend-url.com/
   ```
2. Rebuild the APK.

---

## 4. What to test

1. **Search**: Type a song name. It should suggest as you type (300ms debounce).
2. **Playback**: Select a song. It should play in the Skibidi player.
3. **Controls**:
   - Play/Pause, Next, and Previous buttons.
   - Drag the slider to seek.
4. **Autoplay**: Let a song finish; the next one in the search list should start.
5. **Background**: Minimize the app; the music should keep playing with a notification.
