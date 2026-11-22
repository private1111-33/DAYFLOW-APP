# DAYFLOW — Android starter project

A native Android starter for DAYFLOW, using Kotlin, Jetpack Compose and Material 3.

## Current state
Working today (all data is stored locally on the device):
- Planner with a monthly calendar; tasks belong to a date and can have a time
- Alarms with repeat days, edit, 10-minute snooze, looping sound on the alarm stream and vibration
- Vocabulary cards with pronunciation (text-to-speech) and learned tracking
- Music library imported from the device, background playback, seek bar, shuffle and repeat
- Home dashboard, Progress screen, JSON backup and restore
- GitHub Actions debug build and emulator screenshots

Not implemented yet:
- Room database (data currently lives in SharedPreferences)
- Recurring tasks and task reminders
- Spaced-repetition scheduling for vocabulary
- Automated tests

## Project details
- Application ID: `com.dayflow.app`
- Minimum Android: API 26
- Compile/target SDK: 35
- JDK: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21

## Build
Open the folder in Android Studio and build the `app` configuration, or run:

```bash
gradle assembleDebug
```

The debug APK is normally generated at:
`app/build/outputs/apk/debug/app-debug.apk`

## Build from GitHub Actions
1. Upload this project to a GitHub repository.
2. Open the **Actions** tab.
3. Run the workflow named **Build DAYFLOW debug APK**.
4. Download the `DAYFLOW-debug-apk` artifact after the workflow completes.

## Recommended next implementation order
1. Architecture packages, Room entities/DAO/database, repositories and DataStore.
2. Task creation, editing, completion and recurring-task rules.
3. AlarmManager exact alarms and permission-aware scheduling.
4. Vocabulary reviews, history and spaced repetition.
5. Media3 music import and background playback.
6. Progress calculations, backup/restore, accessibility and device QA.

Keep all data local. Do not add login, cloud sync, analytics, ads or tracking unless explicitly requested.
