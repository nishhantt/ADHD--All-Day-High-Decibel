# Hackathon demo – get the app working

Two ways to run the full flow (search → tap → **correct song** plays). Pick one.

---

## Option 1: Demo with your laptop (fastest)

Use this when you’re presenting and your laptop is on the same Wi‑Fi as your phone.

1. **Backend on laptop**
   ```powershell
   cd d:\desktopi\musicPlayer
   pip install -r requirements.txt
   python extractor_server.py
   ```
   Leave this running. You should see: `Running on http://0.0.0.0:8080`.

2. **Your laptop’s IP**
   - CMD: `ipconfig` → use **IPv4 Address** of Wi‑Fi (e.g. `192.168.1.10`).

3. **Point the app at your laptop**
   - Edit **`local.properties`** in the project root:
   ```properties
   YOUTUBE_API_KEY=AIzaSyDK1Q5Xl0IroijWh0TxBrXyVMGP17bmi_c
   EXTRACTOR_BACKEND_URL=http://192.168.1.10:8080/
   ```
   (Use your real IP; keep the trailing `/`.)

4. **Build and install the APK**
   - Android Studio: **Build → Build APK(s)**.
   - Install `app/build/outputs/apk/debug/app-debug.apk` on your phone.

5. **Demo**
   - Open app → search e.g. “baby justin bieber” → tap a result → **that song** plays.

---

## Option 2: Backend on the cloud (app works without your laptop)

Use this so judges (or you) can use the app even when your laptop is off.

### Deploy backend on Render (free)

1. **Push your project to GitHub** (if not already).  
   Make sure these are in the repo: `extractor_server.py`, `requirements.txt`.

2. **Render**
   - Go to [render.com](https://render.com) → Sign up (free).
   - **New → Web Service**.
   - Connect your GitHub repo (the one with `musicPlayer`).
   - Settings:
     - **Build Command:** `pip install -r requirements.txt`
     - **Start Command:** `python extractor_server.py`
     - **Instance type:** Free.
   - Create Web Service. Wait until it’s live (green).

3. **Copy the service URL**  
   Example: `https://musicplayer-xxxx.onrender.com`  
   It must end with `/`.

4. **Put that URL in the app**
   - Edit **`local.properties`**:
   ```properties
   YOUTUBE_API_KEY=AIzaSyDK1Q5Xl0IroijWh0TxBrXyVMGP17bmi_c
   EXTRACTOR_BACKEND_URL=https://musicplayer-xxxx.onrender.com/
   ```
   (Your real Render URL; keep the trailing `/`.)

5. **Rebuild the APK** and install on the phone.  
   Now the app uses the cloud backend; no laptop needed.

**Note:** Render free tier may sleep after ~15 min of no use. First request after that can take 30–60 seconds; after that it’s fast. For a live demo, open the Render URL in a browser once a few minutes before presenting to wake it.

---

## Demo script (what to say / show)

1. **“This is a minimal music streaming app: search, tap, play.”**
2. **Search:** Type a song (e.g. “shape of you”) → show YouTube results.
3. **Tap a result** → player opens, **that song** plays (not a sample).
4. **Show controls:** Play/pause, next, previous, seek, repeat.
5. **Optional:** Lock the phone or go to Home → playback continues with notification.

---

## Checklist before presenting

- [ ] `local.properties` has `YOUTUBE_API_KEY` and `EXTRACTOR_BACKEND_URL` (laptop IP **or** Render URL with `/`).
- [ ] Backend is running (laptop **or** Render service is live).
- [ ] APK was **rebuilt** after the last change to `local.properties`.
- [ ] Phone and laptop on same Wi‑Fi (if using Option 1).
- [ ] If using Render: open the backend URL in a browser once shortly before the demo to avoid cold start during the pitch.
