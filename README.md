# Vibe Player - Android TV Demo UI

This is a demo user interface for the Android TV application we are currently developing. 

### Key Features (Prototype):
- **Android TV Navigation:** Optimized for D-pad/Remote control usage.
- **Dynamic Views:** Support for both Grid and List layouts (defaults to List).
- **Advanced Player UI:** 
    - Interactive progress bar with seeking support.
    - TV-safe layout with optimized padding and safe areas.
    - High-visibility focus states for remote navigation.
    - Clean, all-white aesthetic.

### Development Note & Workflow:
This UI serves as the foundation for the upcoming native Android application. 

Due to local hardware constraints, the native Android application will be developed using a **Remote Development Workflow**:
1. **Branching:** Native Android code will be written and committed to a separate branch (`feature/android-native`).
2. **Staging Environment (Temp GitHub):** Initial development and CI/CD testing will be pushed to a temporary/secondary GitHub account. This keeps the primary profile clean during the messy prototyping phase.
3. **Remote Building:** GitHub Actions on the temporary repository will automatically build the Android APK upon every push.
4. **Testing:** The resulting APK will be downloaded and tested on an actual Android TV or remote emulator.
5. **Production Release (Main GitHub):** Once the application is stable, verified, and ready for release, the final clean code and its history will be pushed to the main GitHub account and merged into the `main` branch.
