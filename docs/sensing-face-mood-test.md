# Sensing-driven pet face mood test

## Purpose

Verify that the v1 face reflects existing scalar sensing states without adding speech, network
access, identity recognition, or raw-media persistence.

## Mood mapping

- no confirmed presence: dim blue resting eyes;
- possible presence: violet curious eyes;
- confirmed presence with no activity: cyan attentive eyes;
- confirmed activity: green engaged eyes;
- break candidate: amber break-ready eyes.

## Automated verification

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Pass when the mood-mapping unit tests, existing tests, lint, APK assembly, and v0 guardrails all
succeed.

## Physical-device verification

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

Verify the following on the Xiaomi Pad:

1. With nobody visible, the eyes settle into dim blue resting mode.
2. Moving into view briefly shows violet curious mode.
3. A visible face produces cyan attentive mode.
4. Moving or speaking while visible produces green engaged mode.
5. Becoming quiet after activity produces amber break-ready mode.
6. Tap gaze, blink animation, timing buttons, and seven-day replay remain usable.
7. Leaving PetRobot releases foreground camera and microphone sensing as before.

Capture mood transitions with:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotFace:I PetRobotPresence:I PetRobotBreak:I "*:S"
```

Pass when the visual mood changes match the transition logs and no regression is observed.
