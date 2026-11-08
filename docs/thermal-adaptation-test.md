# Thermal load-adaptation physical-device test

This procedure validates that PetRobot observes Android's thermal status and applies a bounded
camera-analysis load profile. It does not record or retain camera frames, microphone audio, or
speech content.

## Load profiles

| Android thermal status | Profile | Camera analysis stride |
| --- | --- | ---: |
| None or light | `FULL` | 1 |
| Moderate | `REDUCED` | 2 |
| Severe | `LOW` | 5 |
| Critical, emergency, or shutdown | `MINIMAL` | 15 |

A stride of 5 means that one of every five received CameraX frames reaches Y-plane comparison and
face detection. Skipped frames are closed immediately. When the profile changes, the Y-plane
baseline is reset so frames captured at different intervals are not compared.

## Build and install

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
```

Confirm that the build succeeds, guardrails pass, and exactly one physical device is listed.

## Current status and normal-rate sensing

Start the app, wait ten seconds, and inspect the thermal and camera logs:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 10
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotThermal:I PetRobotCamera:I "*:S"
```

Pass when a `PetRobotThermal` line contains a finite status, profile, and stride, and camera
scalar results continue to appear. Under ordinary cool conditions, the expected line is
`status=NONE profile=FULL cameraStride=1`.

Optionally capture the platform's supporting thermal report:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" shell dumpsys thermalservice
```

Do not deliberately heat the tablet or override OEM thermal controls. Every elevated-status mapping,
profile transition, recovery to `FULL`, stride value, and timestamp-order check is covered
deterministically by local JVM tests. Record the command output, device, duration, current thermal
status, and any observed failure in the pull request.
