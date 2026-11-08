# Structured Room log physical-device test

This procedure validates that PetRobot persists bounded scalar evidence in a local Room database.
The database contains timestamps, face counts, Y-plane differences, audio envelope values, state
names, and thermal load profiles. It cannot store camera frames, audio samples, face features,
transcripts, or other raw media.

## Dependency rationale

Room is used because v0 requires queryable local evidence for later daily summaries and threshold
replay. Room 2.8.5 is the current stable AndroidX release. KSP is used only to generate the Room DAO
and database implementation for Kotlin source.

## Build and install

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
```

Confirm that the build succeeds, guardrails pass, and exactly one physical device is listed.

## First run

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 15
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotStorage:I "*:S"
& "C:\Android\Sdk\platform-tools\adb.exe" shell run-as com.taki.petrobot ls -l databases
```

Pass when the log contains `databaseReady rows=0` or another finite row count, followed by at least
one `stored=...` line, and the private app directory contains `petrobot-v0.db`. The exact initial
row count may be nonzero if an earlier build already created the database.

## Persistence across restart

Record the `databaseReady rows=...` value, force-stop and start the app again, wait five seconds,
then inspect the storage log:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 5
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotStorage:I "*:S"
```

Pass when the new `databaseReady` count is greater than zero and is not lower than the previously
recorded count. Record the commands, device, row counts, duration, and any observed failure in the
pull request.
