# Building SMSGuard for Android

## Cloud build (no local Android Studio required)

If you can't or don't want to install Android Studio + the Android SDK locally, push the repo to GitHub and let CI build the APK for you. The workflow at `.github/workflows/android-build.yml` runs unit tests and produces `app-debug.apk` as a downloadable artifact.

1. Push to a GitHub repo (`git push -u origin master` or `main`).
2. Open the **Actions** tab → **Android build** → pick the latest run.
3. Under **Artifacts**, download `smsguard-debug-apk`. Unzip to get `app-debug.apk`.
4. Install Android **Platform Tools** locally (~10 MB, no Studio needed): https://developer.android.com/tools/releases/platform-tools
5. Connect your phone with USB debugging on, then:
   ```powershell
   adb install -r path\to\app-debug.apk
   ```

You can also trigger the workflow manually via **Actions → Android build → Run workflow** (the `workflow_dispatch` trigger).

## Prerequisites

- **Android Studio** (Ladybug or newer, 2024.2.1+). https://developer.android.com/studio
- **JDK 17** — bundled with Android Studio. If using CLI Gradle, set `JAVA_HOME` to the bundled JDK or a separate JDK 17.
- **Android SDK** with platform 35 (API 35) and build tools — install via Android Studio's SDK Manager.
- A physical Android device (Android 8 / API 26 or newer) with **USB debugging enabled**.

## First-time setup

1. Open `C:\Users\Lenovo\workspace\smsguard\android` as a project in Android Studio.
2. When prompted, let Android Studio download the Gradle wrapper distribution and the Android SDK components it needs.
3. The first sync will take a few minutes.

## Build a debug APK

From Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

Or from the command line (after the wrapper has been generated):

```powershell
cd C:\Users\Lenovo\workspace\smsguard\android
.\gradlew :app:assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Generate the Gradle wrapper (one-time)

The wrapper script is not committed to keep the repo small. Generate it once:

```powershell
cd C:\Users\Lenovo\workspace\smsguard\android
gradle wrapper --gradle-version 8.10.2
```

(Requires a system-installed Gradle for the bootstrap. After this, `gradlew` works without a system Gradle.)

## Install on your phone

1. Connect via USB. Confirm `adb devices` lists it.
2. Either:
   - In Android Studio: select your device in the run target dropdown and click ▶ Run.
   - Or: `.\gradlew :app:installDebug`
3. The app appears as **SMSGuard**. Open it.

## First-run permissions

On first launch the app asks for:

- **Receive SMS messages** — required. The app reads incoming SMS to run the detector locally on the body. The body never leaves your device.
- **Show notifications** (Android 13+) — required so the app can show "⚠ This SMS may be a scam" alerts.

If you deny either, the app will still open but cannot do its job.

## Run the unit tests

```powershell
cd C:\Users\Lenovo\workspace\smsguard\android
.\gradlew :app:testDebugUnitTest
```

The detector rules are tested as plain JVM tests — no emulator needed.

## What this build is and isn't

This is a **dogfooding build**, Phase 1 of the plan. It:

- Listens for incoming SMS (manifest-registered `BroadcastReceiver`).
- Runs the detection rules from `hk.smsguard.app.rules` against the body.
- Posts a high-priority notification if the verdict is `suspicious` or worse.
- Keeps an in-memory list of recent flags shown on the home screen.

It does **not**:

- Replace your default Messages app. Your stock Messages app continues to display every SMS exactly as before. SMSGuard is a "second opinion" overlay — it does not suppress anything.
- Send any SMS content to a server. There are no network calls in this build.
- Persist flag history across app restarts (in-memory only — coming next session).
- Have the live OFCA SSRS registry. The hardcoded `DefaultRegistry` covers the most common HK orgs (HSBC, Hang Seng, BOC, Immigration, Police, HKMA). Will be replaced with a live-fetched registry in Phase 3.

## Debug-only inbox scan

Debug builds include an extra **"Scan existing inbox (debug)"** button on the home screen. Tapping it opens a screen that asks for `READ_SMS` permission, then runs the detector locally over the most recent 500 messages already on your phone and shows what would have been flagged.

This feature exists to validate the rules against your real SMS history without waiting for a new scam to arrive. It is **excluded from release builds**:

- The `READ_SMS` permission is declared only in `app/src/debug/AndroidManifest.xml`.
- The `ScanActivity` lives only in `app/src/debug/kotlin/`.
- The home-screen button is gated on `BuildConfig.DEBUG`.

Release builds (`assembleRelease`) ship with neither the permission nor the activity. This was a deliberate threat-model choice — Google Play rejects non-messaging apps that request `READ_SMS` — and is documented in `docs/threat-model.md`.
