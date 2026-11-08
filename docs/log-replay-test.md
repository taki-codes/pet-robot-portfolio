# Structured log replay test

## Purpose

Verify that saved scalar evidence can be replayed with alternative quiet-period thresholds without recollecting camera or microphone data.

The comparison profiles are:

- current: 1.5 seconds
- alternative: 2.0 seconds
- alternative: 2.5 seconds
- alternative: 3.0 seconds

A gap longer than 10 seconds starts a new replay session. This prevents an app stop or device restart from being interpreted as a quiet-period candidate.

## Automated checks

Run:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Expected:

- The build is successful.
- JVM tests show that the same scalar samples can produce different candidate counts under different thresholds.
- The guardrail script reports no `INTERNET` permission and no forbidden sensing persistence.
- The worktree is clean.

## Xiaomi Pad check

Install and launch the debug app. Tap **7日間ログを再評価**, wait several seconds, then inspect the replay log:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

After tapping the button:

```powershell
Start-Sleep -Seconds 10
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotReplay:I "*:S"
```

Expected:

- One `started` line appears.
- One result line appears for each of `CURRENT_1_5S`, `QUIET_2_0S`, `QUIET_2_5S`, and `QUIET_3_0S`.
- All profiles report the same positive `samples` and `sessions` counts.
- Candidate counts are non-negative. Equal counts are valid when the saved evidence does not cross a threshold boundary.
- A final `completed` line reports the stored candidate count.
- No image, audio, transcript, or identity payload appears.
