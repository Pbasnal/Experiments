# Screen Time Guardian (Kotlin Multiplatform + Android)

This folder contains a **new Kotlin Multiplatform project** with an Android application target focused on app screen-time control.

## What this project does

- Defines per-app daily limits (for example, Instagram 30 minutes/day).
- Reads daily usage using Android `UsageStatsManager`.
- Evaluates lock rules in shared KMP logic (`commonMain`) via `ScreenTimePolicyEngine`.
- Updates lock state for apps that exceed configured limits.
- Uses an `AccessibilityService` to detect foreground app changes and opens a lock screen activity for blocked apps.

## Project structure

- `app/src/commonMain`: shared models and policy engine (KMP shared logic).
- `app/src/androidMain`: Android app UI, usage reader, lock service, and lock screen.

## Important Android permissions/setup

To function on device, users must manually enable:

1. **Usage Access** (Settings > Security/Privacy > Usage Access)
2. **Accessibility Service** for Screen Time Guardian

## Build

From this folder:

```bash
./gradlew :app:assembleDebug
```

(You may need to generate or use a local Gradle wrapper in your environment.)
