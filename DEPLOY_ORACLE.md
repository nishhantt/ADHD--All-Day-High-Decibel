# Deploy extractor backend on Oracle Cloud (runs 24/7, free)

Follow these steps so your backend runs forever on Oracle’s always-free tier.

---

## Step 1: Oracle Cloud account and VM

1. Go to **https://www.oracle.com/cloud/free/** and sign up (free tier).
2. Log in to the Oracle Cloud console.
3. **Create a VM:**
   - Menu (≡) → **Compute** → **Instances** → **Create instance**.
   - **Name:** e.g. `music-extractor`.
   - **Image:** Pick **Ubuntu 22.04** (or 20.04).
   - **Shape:** Leave **Always Free** (e.g. VM.Standard.E2.1.Micro).
   - **Networking:** Create a new VCN if needed; keep default.
   - **Add SSH keys:** Generate a key pair and **download the private key** (you need it to SSH). Save it as e.g. `oracle_key.pem`.
   - Click **Create**.

4. **Open port 8080** so the app can reach the backend:
   - On the instance page, under **Primary VNIC**, click the **Subnet** link.
   - Click the **Security list** (default one).
   - **Add Ingress Rule:**
     - Source: `0.0.0.0/0`
     - IP Protocol: TCP
     - Source port: (blank)
     - Destination port: **8080**
     - Description: `extractor backend`
   - Save.

5. **Note the VM’s public IP** (shown on the instance details page). Example: `129.146.xxx.xxx`.

---

## Step 2: Connect and install

1. **SSH from your PC** (use Git Bash, WSL, or PowerShell with OpenSSH):

   ```bash
   ssh -i path/to/oracle_key.pem ubuntu@YOUR_VM_PUBLIC_IP
   ```

   (Replace path to key and `YOUR_VM_PUBLIC_IP`.)

2. **On the VM**, install Python and pip if needed:

   ```bash
   sudo apt update
   sudo apt install -y python3 python3-pip
   ```

3. **Copy your backend files to the VM** (from your PC, in a new terminal):

   ```bash
   scp -i path/to/oracle_key.pem extractor_server.py requirements.txt start_server.sh ubuntu@YOUR_VM_PUBLIC_IP:~/
   ```

   Or create the files on the VM with `nano` and paste contents from:
   - `extractor_server.py`
   - `requirements.txt`
   - `start_server.sh`

---

## Step 3: Run the backend 24/7

On the VM:

```bash
chmod +x start_server.sh
./start_server.sh
```

To run it in a way that survives logout (optional but recommended), use `screen`:

```bash
sudo apt install -y screen
screen -S extractor
pip install -r requirements.txt
export PORT=8080
python3 extractor_server.py
```

Press **Ctrl+A** then **D** to detach. To reattach later: `screen -r extractor`.

**Check it works:** On your PC browser open: `http://YOUR_VM_PUBLIC_IP:8080/stream?videoId=dQw4w9WgXcQ`  
You should get JSON with a `url` field.

---

## Step 4: Point the app at Oracle

1. On your PC, edit **`local.properties`** in the project root:

   ```properties
   YOUTUBE_API_KEY=AIzaSyDK1Q5Xl0IroijWh0TxBrXyVMGP17bmi_c
   EXTRACTOR_BACKEND_URL=http://YOUR_VM_PUBLIC_IP:8080/
   ```

   Use the **same** public IP from Step 1 (e.g. `http://129.146.xxx.xxx:8080/`). **Trailing slash is required.**

2. **Rebuild the APK** (Android Studio: Build → Build APK(s)).

3. Install the new APK on your phone.

The app will now use the Oracle backend from anywhere (any Wi‑Fi or mobile data). The backend runs 24/7 on the free VM.

---

## Quick reference

| Item | Value |
|------|--------|
| Backend URL in app | `http://YOUR_VM_PUBLIC_IP:8080/` |
| Test in browser | `http://YOUR_VM_PUBLIC_IP:8080/stream?videoId=VIDEO_ID` |
| SSH | `ssh -i oracle_key.pem ubuntu@YOUR_VM_PUBLIC_IP` |

If the app still plays the sample: check firewall (port 8080 open), backend running on the VM (`python3 extractor_server.py` or `start_server.sh`), and `EXTRACTOR_BACKEND_URL` in `local.properties` (with `/` at the end) then rebuild.
