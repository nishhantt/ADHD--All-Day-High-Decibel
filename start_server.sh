#!/bin/bash
# Run this on Oracle (or any Linux) VM to start the extractor and keep it running.
# Usage: chmod +x start_server.sh && ./start_server.sh
cd "$(dirname "$0")"
pip install -r requirements.txt --quiet
export PORT=8080
nohup python3 extractor_server.py > server.log 2>&1 &
echo "Server starting on port 8080. Check: tail -f server.log"
