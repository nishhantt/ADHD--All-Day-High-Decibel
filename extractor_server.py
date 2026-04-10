# Backend no longer needed!
# The app now calls JioSaavn API directly from the phone.
# This file is kept for reference only.

from fastapi import FastAPI

app = FastAPI()

@app.get("/")
async def root():
    return {
        "status": "deprecated",
        "message": "Backend no longer needed. App calls JioSaavn API directly."
    }
