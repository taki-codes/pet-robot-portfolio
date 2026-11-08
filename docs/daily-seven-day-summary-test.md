# Daily and seven-day summary test

## Purpose

Verify that PetRobot derives bounded daily and seven-day summaries from the existing scalar Room log without adding raw camera or microphone data.

## Automated checks

Run:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Expected:

- The build is successful.
- Summary window and aggregation JVM tests pass.
- The guardrail script reports no `INTERNET` permission and no forbidden sensing persistence.
- The worktree is clean.

## Xiaomi Pad check

Install and launch the debug app:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 5
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotSummary:I "*:S"
```

Expected:

- Exactly one `period=DAILY` line and one `period=SEVEN_DAYS` line appear.
- Both lines have non-negative scalar counts.
- The seven-day total is greater than or equal to the daily total.
- The three timing rating counts do not exceed the timing evaluation count.
- No image, audio, transcript, or identity payload appears.

The windows use the device local timezone. Daily starts at local midnight; seven days starts at local midnight six calendar days earlier. Both end at the summary request time.
