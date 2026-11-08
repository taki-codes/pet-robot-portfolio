# PetRobot agent instructions

## Project scope
PetRobot turns a Xiaomi Pad 7 Pro into a dedicated tabletop pet terminal.
The current milestone is v0: local sensing and reliability measurement only.

## v0 constraints
- Use Kotlin and Jetpack Compose.
- Keep `minSdk = 34`.
- Keep the existing `MainActivity` baseline screen unchanged unless a task explicitly requests a UI change.
- Do not add the Android `INTERNET` permission.
- Do not add general-purpose STT, TTS, LLM, wake-word platforms, face recognition, face embeddings, topic cards, React Native, or server code.
- T-023 may use Android's on-device speech recognizer only to reduce transient Japanese hypotheses to the boolean keyword `ミー`. It must not request `INTERNET`, fall back to a network recognizer, log or persist hypotheses, or expose transcripts outside the matcher.
- Do not save raw camera frames, images, audio, transcripts, or biometric data.
- Do not add dependencies without explaining why they are required.
- Do not expand a ticket into later milestones.

## Module boundaries
- `:app`: Android lifecycle, Compose UI, permissions, and foreground service entry points.
- `:sensing`: sensor adapters and pure sensing/state logic. It must not persist raw media.
- `:storage`: local structured logs and summaries. It must not depend on `:app`.

Pure state and detector logic must accept timestamped values and remain testable with local JVM tests without CameraX, AudioRecord, ML Kit, Room, Context, or wall-clock access.

## Required verification
Before reporting a task complete, run on Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
.\scripts\verify-v0-guardrails.ps1
```

GitHub Actions must pass before merge. Report the commands run and any checks that could not be executed. Never claim an Android or Xiaomi device check unless it was performed on the physical device.
