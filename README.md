# Vibe Player - Multi-Agent Android TV Development

This repository hosts the development of **Vibe Player**, a modern Android TV application. We are using a unique "Multi-Agent" workflow, comparing different AI architectures (Gemini vs. OpenAI) to build a pin-point replica of our high-fidelity web prototype.

## 📂 Project Structure

- `justui/`: The original Web-based prototype (HTML/CSS/JS). Serves as the "Source of Truth" for layout, colors, and animations.
- `geminiandroid/`: The native Android implementation built using **Jetpack Compose**, developed on the `gemini` branch.
- `openaiandroid/`: Parallel Android implementation developed on the `openai` branch.

## 🚀 Development Workflow

We employ a **Remote Build & Validate** strategy to overcome local hardware constraints and ensure production-grade builds:

### 1. Branch Management
- **`main`**: Production-ready, stable code.
- **`gemini`**: Active development of the Gemini-architected Compose app. Optimized for TV-safe interactions.
- **`openai`**: Development and experiments using OpenAI's architecture.

### 2. CI/CD & Remote Building
We use **GitHub Actions** to automate the compilation of Android APKs. This ensures that every commit is "build-stable."

- **Workflow:** `.github/workflows/gemini-build.yml`
- **Engine:** Gradle 8.6 with Java 17.
- **Optimizations:** 
    - **Caching:** Uses `gradle/actions/setup-gradle` for high-speed dependency caching.
    - **Future-Proof:** Opted into Node.js 24 for all build actions.
- **Artifacts:** Every push to the `gemini` branch generates a downloadable `gemini-vibe-player-debug.apk`.

### 3. UI Precision (The "Pin-Point" Rule)
The Android application is built to be a 1:1 match with the `justui` demo:
- **Typography:** Locally bundled **Space Grotesk** font.
- **Animations:** Custom Compose animations for focus scaling (1.06x) and content dimming (0.5 alpha).
- **D-Pad Navigation:** Custom focus handling to mimic the web's TV-remote behavior.

## 🛠 Testing the App
1. Push changes to the `gemini` branch.
2. Wait for the **Gemini Android Build** action to complete.
3. Download the APK from the "Actions" tab.
4. Sideload onto an Android TV or Emulator for validation.
