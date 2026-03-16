# Point-to-point setup with Render (no card needed)

Do these steps in order. Render free tier does **not** require a credit card.

**Note:** Free tier sleeps after ~15 min of no use. First request after that may take 30–60 seconds; then it’s fast. Before a demo, open your backend URL in a browser once to wake it.

---

## PART A: Put backend code on GitHub

### A1. Create a new repo on GitHub
1. Go to **https://github.com** and sign in (or create an account).
2. Click **+** (top right) → **New repository**.
3. **Repository name:** e.g. `music-extractor-backend`.
4. Choose **Public**. Do **not** add README, .gitignore, or license.
5. Click **Create repository**.

### A2. Upload backend files
You only need two files in this repo (no API keys, no app code).

**Option 1 – Upload in the browser**
1. On the new repo page, click **uploading an existing file**.
2. Drag and drop (or choose) these two files from `d:\desktopi\musicPlayer`:
   - **extractor_server.py**
   - **requirements.txt**
3. Click **Commit changes**.

**Option 2 – Using Git on your PC**
```powershell
cd d:\desktopi\musicPlayer
git init
git remote add origin https://github.com/YOUR_USERNAME/music-extractor-backend.git
git add extractor_server.py requirements.txt
git commit -m "Backend for music app"
git branch -M main
git push -u origin main
```
(Replace `YOUR_USERNAME` with your GitHub username.)

---

## PART B: Create the service on Render

### B1. Sign up
1. Go to **https://render.com**
2. Click **Get Started for Free**.
3. Sign up with **GitHub** (recommended) – click **Sign up with GitHub** and authorize Render.  
   No credit card is required for the free tier.

### B2. New Web Service
1. In the Render dashboard, click **New +** → **Web Service**.
2. **Connect a repository:**  
   If your repo is not listed, click **Configure account** and allow Render to access GitHub, then select **music-extractor-backend** (or the repo name you used).  
   Click **Connect** next to that repo.

### B3. Settings
1. **Name:** e.g. `music-extractor` (this becomes part of the URL).
2. **Region:** pick one close to you.
3. **Branch:** `main` (or your default branch).
4. **Runtime:** **Python 3**.
5. **Build Command:**  
   ```bash
   pip install -r requirements.txt
   ```
6. **Start Command:**  
   ```bash
   python extractor_server.py
   ```
7. **Instance Type:** leave **Free**.

Click **Create Web Service**.

### B4. Wait for deploy
Render will build and start the app. Wait until the top of the page shows **Live** (green).  
If it fails, check the **Logs** tab for errors (e.g. missing dependency).

### B5. Copy your backend URL
At the top you’ll see something like: **https://music-extractor-xxxx.onrender.com**  
Copy that URL and **add a slash at the end**. Example:
```text
https://music-extractor-xxxx.onrender.com/
```
This is your **EXTRACTOR_BACKEND_URL**.

---

## PART C: Point the Android app to Render

### C1. Edit local.properties
1. On your PC, open **`d:\desktopi\musicPlayer\local.properties`**.
2. Set (paste your **real** Render URL with trailing slash):

```properties
YOUTUBE_API_KEY=AIzaSyDK1Q5Xl0IroijWh0TxBrXyVMGP17bmi_c
EXTRACTOR_BACKEND_URL=https://music-extractor-xxxx.onrender.com/
```

3. Save.

### C2. Build the APK
1. Open **Android Studio** → **File** → **Open** → select **`d:\desktopi\musicPlayer`**.
2. Wait for Gradle sync.
3. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**.
4. APK path: **`app\build\outputs\apk\debug\app-debug.apk`**.

### C3. Install on your phone
- Copy **app-debug.apk** to the phone and install, **or**
- USB + USB debugging:  
  ```powershell
  adb install -r app\build\outputs\apk\debug\app-debug.apk
  ```

### C4. Test
1. Open **Music Player** on the phone.
2. Allow notifications if asked.
3. Search a song (e.g. "baby justin bieber") and tap a result.

The first request after the service has been sleeping may take 30–60 seconds; then the correct song should play.

**Before a hackathon demo:** Open this URL in your phone’s browser once a couple of minutes before presenting:  
`https://music-extractor-xxxx.onrender.com/stream?videoId=dQw4w9WgXcQ`  
That wakes the service so the first play in the app is fast.

---

## Checklist

- [ ] A1–A2: GitHub repo created, **extractor_server.py** and **requirements.txt** pushed.
- [ ] B1–B5: Render account (no card), Web Service created, Build/Start commands set, **Live** and URL copied with trailing `/`.
- [ ] C1–C4: **local.properties** updated, APK built and installed, search and play tested.

---

## If something fails

- **Build failed on Render:** Check the **Logs** tab. Ensure **requirements.txt** contains `flask` and `yt-dlp`, and **Start Command** is exactly `python extractor_server.py`.
- **App still plays sample:** Rebuild the APK after changing **local.properties**. Confirm **EXTRACTOR_BACKEND_URL** has no typo and **ends with `/`**. Phone needs internet.
- **First play very slow:** Normal on free tier after sleep. Open the backend URL in a browser once before demo to wake it.
