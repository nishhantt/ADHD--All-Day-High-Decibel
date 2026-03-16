# Point-to-point setup guide

Do these steps in order. Do not skip.

---

## PART A: Oracle Cloud – account and VM

### A1. Sign up
1. Open browser → go to **https://www.oracle.com/cloud/free/**
2. Click **Start for free**
3. Fill form (email, country, name). Click **Verify my email** and check your inbox
4. Complete the rest (password, region – pick one near you). Click **Create account**
5. You may be asked to sign in again. Do it

### A2. Create a compute instance (VM)
1. In Oracle Cloud, click the **≡** (hamburger) menu top-left
2. Click **Compute** → **Instances**
3. Click **Create instance**
4. **Name:** type `musicbackend` (or any name)
5. **Placement:** leave default
6. **Image and shape:**  
   - Click **Edit** next to "Image and shape"  
   - **Image:** pick **Ubuntu 22.04** (or 20.04)  
   - **Shape:** ensure **Always free-eligible** is selected (e.g. **VM.Standard.E2.1.Micro**)  
   - Click **Select image**
7. **Networking:** leave "Create new virtual cloud network" and default subnet
8. **Add SSH keys:**  
   - Select **Generate a key pair for me**  
   - Click **Save private key** – a file (e.g. `key.pem`) will download. **Keep it safe.**  
   - Click **Save public key** (optional, for your records)
9. Click **Create** at the bottom
10. Wait until **State** = **Running** (green). Note the **Public IP address** (e.g. `129.146.xxx.xxx`) – you need it later

### A3. Open port 8080 for the backend
1. On the instance page, under **Primary VNIC**, click the **Subnet** (blue link, e.g. "Default subnet for...")
2. On the subnet page, under **Security lists**, click the **Default Security List** (blue link)
3. Click **Add ingress rules**
4. Fill:
   - **Source CIDR:** `0.0.0.0/0`
   - **Destination port range:** `8080`
   - **Description:** `backend`
5. Click **Add ingress rules**
6. Go back to your instance (breadcrumb or Compute → Instances) and note the **Public IP** again. You will use it as **YOUR_VM_IP** below

---

## PART B: Put backend files on the VM

### B1. Open terminal on your PC
- **Windows:** Open **PowerShell** or **Git Bash** (if you have it)
- Go to your project folder:
  ```powershell
  cd d:\desktopi\musicPlayer
  ```

### B2. Copy files to the VM
Run this **one line** (replace the two placeholders):

- Replace `PATH_TO_KEY` with the full path to your downloaded `.pem` file  
  Example: `C:\Users\Nishant\Downloads\key.pem`
- Replace `YOUR_VM_IP` with the public IP from A2 (e.g. `129.146.xxx.xxx`)

**PowerShell:**
```powershell
scp -i "PATH_TO_KEY" extractor_server.py requirements.txt start_server.sh ubuntu@YOUR_VM_IP:~/
```

**Example:**
```powershell
scp -i "C:\Users\Nishant\Downloads\key.pem" extractor_server.py requirements.txt start_server.sh ubuntu@129.146.xxx.xxx:~/
```

If it asks "Are you sure you want to continue connecting?" type **yes** and press Enter.

### B3. SSH into the VM
Same placeholders as B2:

```powershell
ssh -i "PATH_TO_KEY" ubuntu@YOUR_VM_IP
```

Example:
```powershell
ssh -i "C:\Users\Nishant\Downloads\key.pem" ubuntu@129.146.xxx.xxx
```

You should see a prompt like `ubuntu@instance-name:~$`. You are now on the VM.

---

## PART C: Run the backend on the VM (24/7)

Do these **on the VM** (after SSH in).

### C1. Install Python and pip
```bash
sudo apt update
sudo apt install -y python3 python3-pip
```

### C2. Install screen (so the server keeps running after you disconnect)
```bash
sudo apt install -y screen
```

### C3. Start a screen session
```bash
screen -S extractor
```

### C4. Install Python packages and start the server
```bash
pip install -r requirements.txt
export PORT=8080
python3 extractor_server.py
```

You should see something like: `Running on http://0.0.0.0:8080`

### C5. Detach from screen (server keeps running)
Press: **Ctrl + A**, then press **D**

You will be back at the normal prompt. The server is still running in the background.

(To reattach later: `screen -r extractor`. To stop the server: attach then press **Ctrl + C**.)

### C6. Test from your PC browser
Open in browser (use your real VM IP):

```
http://YOUR_VM_IP:8080/stream?videoId=dQw4w9WgXcQ
```

You should see JSON with a `"url"` field. If you do, the backend is working. You can close the SSH window; the server keeps running.

---

## PART D: Point the Android app to the backend

### D1. Edit local.properties on your PC
1. On your PC, open the file: **`d:\desktopi\musicPlayer\local.properties`**
2. Set these two lines (use your **real VM IP** and keep the slash at the end):

```properties
YOUTUBE_API_KEY=AIzaSyDK1Q5Xl0IroijWh0TxBrXyVMGP17bmi_c
EXTRACTOR_BACKEND_URL=http://YOUR_VM_IP:8080/
```

Example (fake IP):
```properties
YOUTUBE_API_KEY=AIzaSyDK1Q5Xl0IroijWh0TxBrXyVMGP17bmi_c
EXTRACTOR_BACKEND_URL=http://129.146.xxx.xxx:8080/
```

3. Save the file.

### D2. Build the APK
1. Open **Android Studio**
2. **File** → **Open** → select folder **`d:\desktopi\musicPlayer`**
3. Wait for Gradle sync to finish
4. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
5. When done, click **locate** in the notification, or go to:  
   **`d:\desktopi\musicPlayer\app\build\outputs\apk\debug\app-debug.apk`**

### D3. Install on your phone
- Copy **app-debug.apk** to your phone and open it to install, **or**
- Connect phone by USB, enable **USB debugging**, then in PowerShell:
  ```powershell
  cd d:\desktopi\musicPlayer
  adb install -r app\build\outputs\apk\debug\app-debug.apk
  ```

### D4. Use the app
1. Open **Music Player** on the phone
2. Allow **notifications** if asked
3. Search a song (e.g. "baby justin bieber")
4. Tap a result

The **correct song** should play (not the sample). The backend on Oracle runs 24/7, so the app works from any network.

---

## Checklist

- [ ] A1–A3: Oracle account, VM created, port 8080 open, public IP noted
- [ ] B1–B3: Files copied to VM, SSH works
- [ ] C1–C6: Python installed, server running in screen, browser test shows JSON
- [ ] D1–D4: local.properties has EXTRACTOR_BACKEND_URL with your VM IP and trailing slash, APK built and installed, search and play tested

---

## If something fails

- **SSH "Permission denied":** Use the correct path to the `.pem` file and the correct IP. On Windows, sometimes you need: `ssh -i "C:\path\to\key.pem" -o StrictHostKeyChecking=no ubuntu@YOUR_VM_IP`
- **Browser test shows connection refused:** Port 8080 not open – recheck A3 (ingress rule for 8080, source 0.0.0.0/0). Restart the server (C4) and test again.
- **App still plays sample:** Rebuild APK after changing local.properties (D1, D2). Confirm EXTRACTOR_BACKEND_URL has no typo and ends with `/`. Phone must have internet (Wi‑Fi or mobile data).
