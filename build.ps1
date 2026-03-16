param(
    [string]$GradleTarget = ":app:assembleDebug"
)

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker is not installed or not on PATH. Install Docker Desktop or use Android Studio to build."
    exit 1
}

$pwd = (Get-Location).Path
Write-Host "Building with Docker. This may take a few minutes..."
docker run --rm -v "${pwd}:/home/gradle/project" -w /home/gradle/project gradle:8.3-jdk17 gradle $GradleTarget
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Build finished. APK (if produced) will be at app/build/outputs/apk/debug/app-debug.apk"
