# 24-hour camera and audio sensing soak test

## Purpose

Verify that the fixed-installation Xiaomi Pad can keep the PetRobot activity, CameraX scalar analysis,
microphone envelope analysis, Room storage, and foreground heartbeat running for 24 continuous
hours before the seven-day study.

No raw image, audio, transcript, identity data, or network access is used.

## Preconditions

- T-001 through T-014 are merged.
- PetRobot has notification, camera, and microphone permission.
- HyperOS allows PetRobot background startup and applies no battery restriction.
- Battery charging protection remains at the chosen fixed setting for the entire run.
- Use a stable AC charger after collecting each ADB checkpoint.
- Set screen brightness to the lowest practical level.
- Keep the PetRobot screen in front. Do not press Home, open another app, lock the tablet, force-stop
  PetRobot, reinstall it, clear its data, or reboot the tablet during the run.

PetRobot now requests that Android keep the screen on only while its activity is visible. Leaving the
activity still releases the camera and microphone as designed.

## Build verification

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

## Short physical check before the run

Install and open PetRobot:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

Wait longer than the tablet's normal screen timeout. Pass when the PetRobot activity remains visible
and the screen remains on. Opening another app must still stop camera and microphone sensing.

Verify that checkpoint collection works before the real run:

```powershell
.\scripts\capture-24h-soak-checkpoint.ps1 -Phase precheck
```

This precheck file is not the 24-hour start checkpoint.

## Start checkpoint

With USB connected and PetRobot visible, run:

```powershell
.\scripts\capture-24h-soak-checkpoint.ps1 -Phase start
```

Also record a screenshot of the ongoing `PetRobot heartbeat` notification. Record its elapsed time
and heartbeat number.

After the checkpoint, the PC cable may be disconnected. Connect the tablet to the stable AC charger
without closing PetRobot.

## 12-hour checkpoint

Reconnect USB without navigating away from PetRobot and run:

```powershell
.\scripts\capture-24h-soak-checkpoint.ps1 -Phase 12h
```

Record another heartbeat-notification screenshot. Return the tablet to the same AC charger and fixed
location.

## 24-hour checkpoint

At 24 hours, reconnect USB without navigating away and run:

```powershell
.\scripts\capture-24h-soak-checkpoint.ps1 -Phase 24h
```

Record the final heartbeat-notification screenshot. Then inspect current summary and replay output:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 5
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotSummary:I "*:S"
```

Tap **7日間ログを再評価**, wait ten seconds, then run:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotReplay:I "*:S"
```

Checkpoint text files are saved under `Documents\PetRobotEvidence`, outside the Git worktree.

## Pass criteria

Pass only when all of the following are true:

- the run lasts at least 24 continuous hours without reinstall, force-stop, reboot, or manual restart;
- the heartbeat notification remains present, elapsed time does not reset, and the count continues;
- the screen and PetRobot activity remain visible for the whole run;
- recent diagnostics at every checkpoint contain advancing camera and audio observations;
- the Room database files remain readable and continue growing;
- the 24-hour summary contains non-zero camera and audio counts;
- replay completes with a positive, equal sample count for every threshold profile;
- process exit history contains no crash, ANR, low-memory kill, or force-stop during the run;
- thermal output shows no sustained severe, critical, emergency, or shutdown condition;
- memory does not show a continuing unexplained rise across start, 12-hour, and 24-hour checkpoints;
- no raw media or network permission is introduced.

A failure does not erase the evidence. Save the checkpoint, record the observed symptom and time,
then diagnose the cause before restarting the 24-hour run.
