# Break-candidate state-machine physical-device test

This procedure validates a possible interaction boundary using only scalar presence, motion, and
VAD-lite signals. It does not record or retain camera frames, microphone audio, or speech content.

## State rules

- `IDLE`: no confirmed presence or no recent activity.
- `ACTIVE`: confirmed presence plus voice or motion activity.
- `CANDIDATE`: confirmed presence remains after 1.5 seconds without voice or motion activity.
- New activity changes `CANDIDATE` back to `ACTIVE`.
- Loss of confirmed presence resets the state to `IDLE`.

A candidate is measurement evidence only. It does not yet trigger speech or UI behavior.

## Build and install

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
```

Confirm that the build succeeds, guardrails pass, and exactly one physical device is listed.

## Activity to candidate

Keep your face visible to the front camera. Speak or move for three seconds, then remain still and
quiet for at least three seconds:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
Read-Host "顔を映し、話すか動きながら準備できたらEnterを押してください"
Start-Sleep -Seconds 3
Write-Host "動きと発話を止め、顔を映したまま静かにしてください"
Start-Sleep -Seconds 4
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotBreak:I "*:S"
```

Pass when the log contains `state=ACTIVE`, followed by `state=CANDIDATE` while
`presence=PRESENT`.

## Rearm and reset

While still visible, speak again. Pass when `CANDIDATE -> ACTIVE` appears. Then move completely
out of view and remain quiet. Pass when the presence state eventually becomes absent and the break
state reports `state=IDLE`.

The 1.5-second timing and reset paths are covered deterministically by local JVM tests. Record the
command output, device, duration, and any observed failure in the pull request.
