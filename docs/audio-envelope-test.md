# Audio envelope and VAD-lite physical-device test

This procedure validates scalar microphone sensing on the physical Xiaomi Pad. It does not
record, save, transcribe, or identify speech.

## Prerequisites

- Check out the T-007 branch.
- Connect the Xiaomi Pad with USB debugging enabled.
- Keep the tablet in a normal indoor environment.

## Build and install

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
```

Confirm that the build succeeds, the guardrail script passes, and exactly one physical device is
listed.

## Quiet-room baseline

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

Allow microphone access when Android asks. Stay quiet for 15 seconds, then run:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotAudio:I "*:S"
```

Pass when regular lines contain finite `rms`, `dbfs`, `peak`, `vad`, and `samples=1600`.
A quiet room should normally produce lower dBFS values and mostly `vad=false`.

## Voice candidate response

Clear logcat, speak near the tablet for 15 seconds, and inspect the same tag:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
Write-Host "15秒間、タブレットの近くで普通の声量で話してください"
Start-Sleep -Seconds 15
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotAudio:I "*:S"
```

Pass when speech raises RMS/dBFS above the quiet baseline and at least one representative line has
`vad=true`. VAD-lite is a level threshold only, so this test does not claim speech recognition.

## Background stop

Open Android Settings so PetRobot is no longer visible, clear logcat, wait ten seconds, and inspect:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -a android.settings.SETTINGS
Start-Sleep -Seconds 2
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
Start-Sleep -Seconds 10
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotAudio:I "*:S"
```

Pass when no `PetRobotAudio` samples appear while Settings is in front. Returning to PetRobot
should resume samples without reinstalling the app.

Record the command output, device, duration, and any observed failure in the pull request.
