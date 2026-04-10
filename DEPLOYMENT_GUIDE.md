# No-Card Guide: Getting Your Music Engine Online ($0)

Since you don't have a credit card, we will use **Render.com** or **Hugging Face**. Both of these let you host for free without any billing info!

---

## 🚀 Option 1: Render.com (Easiest - No Card Needed)
Render is perfect for this. I have already added the "Pre-Warm" logic to your Android app so it works smoothly even if the server sleeps!

1. Go to [Render.com](https://render.com) and sign up with your GitHub account.
2. Click **New +** $\rightarrow$ **Web Service**.
3. Select your `music-extractor-backend` repository.
4. For the **Runtime**, choose **Docker**.
5. Select the **Free Instance Type**.
6. Click **Create Web Service**.
7. Once it's live, copy the URL (e.g., `https://my-app.onrender.com`).

---

## 🚀 Option 2: Hugging Face Spaces (Always On - No Card Needed)
Hugging Face is a secret "gem" for free hosting. It's stable and has a generous free tier.

1. Create a free account at [HuggingFace.co](https://huggingface.co).
2. Click **New Space**.
3. Name it (e.g., `skibidi-music`).
4. Select **Docker** as the Space SDK.
5. Choose **Blank** or **HTTP Header** template.
6. Public/Private doesn't matter much here. Click **Create Space**.
7. Upload your `extractor_server.py`, `Dockerfile`, and `requirements.txt` to the Files tab.
8. It will build and run. Your URL will be in the "Embed this Space" or "Direct URL" settings.

---

## 🛠️ Step 4: Link your Android App
Open `NetworkModule.kt` in Android Studio and update:
```kotlin
const val BACKEND_URL = "https://your-custom-url.onrender.com"
```

### Step 3: Configure & Deploy
1. Name your service (e.g., `skibidi-engine`).
2. Click **Deploy**.
3. Wait about 2-3 minutes. Once the status turns **Healthy**, you will see a URL like:
   `https://skibidi-engine-yourname.koyeb.app`

### Step 4: Link your Android App
1. Copy that URL.
2. Open your Android project in Android Studio.
3. Open `NetworkModule.kt`.
4. Replace the old IP with your new URL:
   ```kotlin
   const val BACKEND_URL = "https://skibidi-engine-yourname.koyeb.app"
   ```
5. Build the app and install it on your Samsung A21s.

---

## 🛠️ How to fix the VS Code Docker Error (If you still want it)
If you just want the Docker icon in VS Code for coding:
1. Open **Visual Studio Code**.
2. Press `Ctrl + Shift + X` (Extensions).
3. Search for `Docker`.
4. Click the blue **Install** button inside VS Code.
5. **Ignore the .vsix file** you downloaded; VS Code handles it automatically.
