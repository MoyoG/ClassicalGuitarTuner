# Classical Guitar Tuner for Android

A standalone Java Android app for tuning a six-string classical (nylon-string) guitar.

## Build
1. Install Android Studio with Android SDK Platform 35 and JDK 17.
2. Open the `ClassicalGuitarTuner` folder as an Android Studio project.
3. Allow Gradle sync to download Android Gradle Plugin 8.7.3.
4. Select **Build > Build APK(s)** or run on an Android device.
5. Grant microphone permission when prompted.

The APK will be at `app/build/outputs/apk/debug/app-debug.apk` after a successful debug build.

## How to use
- Tap AUTO DETECT to recognize the nearest standard open string.
- Or tap a string E, A, D, G, B, e to lock the tuning target.
- Tune until the needle centers and the display reads IN TUNE (within 5 cents).
- Strings from low to high: E2 82.41 Hz, A2 110 Hz, D3 146.83 Hz, G3 196 Hz, B3 246.94 Hz, E4 329.63 Hz.
- Works offline. Microphone samples are processed on-device and are not stored or uploaded.

## Notes
- Use in a quiet room, pluck one open string at a time, and let it ring.
- Auto detection is designed for strings near standard pitch; for very detuned strings, lock a string manually.
- Pitch detection is YIN-inspired and has not been validated on a physical Android device in this environment.
- This archive includes source files, not a precompiled APK or Gradle wrapper. Android Studio can sync using an installed Gradle environment.
