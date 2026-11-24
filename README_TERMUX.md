# JARVIS AI — Android starter project

This is a functional starter, not a claim that every advanced feature in the specification is already implemented.

## Included
- Jetpack Compose futuristic dashboard and navigation
- Android speech recognition via the system recognizer
- Android TextToSpeech output (voice and language depend on installed TTS engines)
- Chat UI connected to a separate Gemini backend
- Backend health endpoint and secure server-side Gemini key handling
- Basic Studio, Projects and Settings screens

## Requirements
- Android SDK platform 35 and build-tools installed
- JDK 17
- Gradle 8.9+ (wrapper can be generated with `gradle wrapper`)
- Python 3.10+ for backend
- Gemini API key for AI responses

## Build in Termux
Android SDK setup on Termux varies by device. Install a JDK and Gradle first, then configure a valid Android SDK.
Example:
```bash
pkg update
pkg install openjdk-17 gradle unzip
cd JARVIS_AI
gradle wrapper
```
Set `ANDROID_HOME` to your Android SDK directory and ensure platform 35/build-tools are installed. Then:
```bash
./gradlew assembleDebug
```
APK output:
`app/build/outputs/apk/debug/app-debug.apk`

If Termux does not have a usable Android SDK, build this project with Android Studio on a computer or a supported cloud build environment. A normal Termux install alone does not guarantee Android SDK availability.

## Run Gemini backend
```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export GEMINI_API_KEY="YOUR_KEY"
export GEMINI_MODEL="gemini-2.0-flash"
uvicorn main:app --host 0.0.0.0 --port 8000
```
Never put the Gemini key in Android source code, APK resources, Git, or a public repository.

### Backend address
The app currently calls `http://127.0.0.1:8000/chat`. This works only when the backend is reachable from the Android device at that address. For a backend running on another device, change the URL in `MainActivity.kt` to that server's LAN address and use HTTPS for production. Android cleartext HTTP is enabled only for local development in this starter.

## Current limitations
Not yet implemented: wake-word detection, background listening, app-wide accessibility automation, WhatsApp/Gmail integrations, call/SMS actions, persistent memory, YouTube publishing, photo/video processing, authentication, encrypted cloud sync, and autonomous multi-step execution. These require separate modules, provider credentials, Android permissions, and user confirmation flows. Do not enable broad Accessibility access without a clear user-facing purpose and safeguards.

## Before release
- Replace wildcard backend CORS with an allowlist.
- Use HTTPS and authenticated user sessions.
- Add rate limiting, privacy controls, encrypted storage, and audit logs.
- Request runtime microphone permission before speech recognition if needed by the target device.
- Test on a real Android phone and verify every integration before claiming it works.
