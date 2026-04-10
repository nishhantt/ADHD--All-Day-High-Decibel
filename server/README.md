# ADHD Music Server

YouTube-powered music streaming backend for your phone.

## Features
- Search any song (iTunes API)
- Stream audio from YouTube (yt-dlp)
- Recommendations based on listening history
- 24/7 server on your phone

## Setup on Termux

### 1. Copy server folder to Termux
```bash
# In Termux
cd ~
cp -r /path/to/server ~/music-server
cd ~/music-server
```

### 2. Install dependencies
```bash
pkg update && pkg upgrade -y
pkg install python git

pip install -r requirements.txt
```

### 3. Run the server
```bash
python app.py
```

Server will run on `http://0.0.0.0:7860`

### 4. Get your IP address
```bash
ifconfig | grep "inet "
# Look for 192.168.x.x
```

### 5. Set up Cloudflare Tunnel (for public access)
```bash
# Download cloudflared
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 > cloudflared
chmod +x cloudflared

# Run tunnel
./cloudflared tunnel --url http://localhost:7860
```

You'll get a URL like `https://xxxx.trycloudflare.com` - use this in your app.

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/mobile/health` | GET | Health check |
| `/api/mobile/search?q=` | GET | Search songs |
| `/api/mobile/chart` | GET | Top charts |
| `/api/mobile/play?id=&artist=&title=` | GET | Get stream URL |
| `/api/mobile/recommend?song_id=` | GET | Get recommendations |
| `/api/mobile/up_next?song_id=` | GET | Up next queue |

## Running 24/7

Use Termux Boot to start server on boot:
```bash
pkg install termux-boot
# Create startup script in ~/.termux/boot/
```
