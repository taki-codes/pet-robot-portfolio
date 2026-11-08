# Presence state-machine physical-device test

This procedure validates only scalar sensor fusion and state transitions on the physical Xiaomi
Pad. It does not retain raw camera frames or microphone audio and does not identify people.

## State rules

- `FACE` evidence changes the state directly to `PRESENT`.
- Motion at or above `0.02`, or fresh VAD-lite activity, starts `POSSIBLE`.
- Continuous activity for 750 ms changes `POSSIBLE` to `PRESENT`.
- A `POSSIBLE` candidate expires after a 500 ms evidence gap.
- `PRESENT` changes to `ABSENT` after a three-second evidence gap.

These are v0 measurement defaults and can be replayed with alternative thresholds later.

## Build and install

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
```

Confirm that the build succeeds, guardrails pass, and exactly one physical device is listed.

## Face arrival and departure

Start with nobody visible to the front camera:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 5
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotPresence:I "*:S"
```

Pass when the first line reports `state=ABSENT` if no activity evidence is present.

Clear logcat, face the camera for five seconds, then move completely out of view and stay quiet for
five seconds:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
Write-Host "5秒間カメラに顔を映し、その後カメラ外へ移動して5秒間静かにしてください"
Start-Sleep -Seconds 12
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotPresence:I "*:S"
```

Pass when a transition to `state=PRESENT ... evidence=FACE` appears, followed after departure by a
transition to `state=ABSENT`. Brief face-detector misses must not immediately clear `PRESENT`.

## Activity-only candidate

With no face visible, sustained movement or speech near the tablet may produce
`ABSENT -> POSSIBLE -> PRESENT`. A single short noise or motion spike should produce at most a
temporary `POSSIBLE` state that returns to `ABSENT`.

This check is observational because the camera field of view and room acoustics affect the scalar
signals. The deterministic thresholds and timing are covered by local JVM tests.

Record the command output, device, duration, and observed failure in the pull request.
