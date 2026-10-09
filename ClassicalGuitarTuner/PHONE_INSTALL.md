# Build and install on Samsung Galaxy S25 (no computer)

This ZIP is source code, not an APK. GitHub Actions builds an APK online.

1. On your phone, create a private repository at https://github.com/new named ClassicalGuitarTuner. Do not initialize it with a README.
2. Open https://github.dev in your browser (or open the new repository and press the `.` shortcut with a keyboard). Upload the *contents* of this extracted ZIP, preserving the folders `app/` and `.github/workflows/`. On mobile, enable **Desktop site** if needed. Commit and push to `main`. If GitHub's web editor does not allow folder uploads from your phone, you will need a mobile Git client or an alternate upload method.
3. On GitHub, open your repository > Actions > Build Android APK > Run workflow (or wait for the push-triggered run).
4. When the green build completes, open the run and download the `ClassicalGuitarTuner-APK` artifact. This download is another ZIP; extract `app-debug.apk` from it.
5. Tap `app-debug.apk` in My Files. If prompted, allow **Install unknown apps** for the app opening the APK. On Samsung devices, Auto Blocker may block sideloading; adjust that setting only if you understand the security implications and trust the APK you built yourself.
6. Install, open the tuner, and allow microphone permission.

If the build fails, open the failed Actions run, expand `Build debug APK`, and share the error log.

Note: the project has not been compiled or tested on a Galaxy S25 in this environment.
