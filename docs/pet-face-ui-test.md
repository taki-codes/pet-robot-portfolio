# Animated pet face UI test

## Purpose

Verify the first v1 LOOI-style face without changing the v0 sensing, storage, evaluation, or privacy
behavior.

## Automated verification

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Pass when the gaze-geometry unit tests pass, lint reports no issue, the debug APK assembles, and the
v0 guardrails still report no network permission or forbidden persistence API.

## Physical-device verification

Install and open PetRobot on the Xiaomi Pad:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

Verify all of the following:

- the screen has a black background and two cyan-blue vertical eyes;
- the eyes blink without user input;
- the eyes slowly look left, center, and right;
- tapping near each corner makes the eyes look toward that tap;
- a tap briefly enlarges the eyes before they return to normal;
- the three timing-rating buttons remain visible and usable;
- each timing button still produces a `PetRobotTiming` log and an immediate Room write;
- **7日間ログを再評価** remains available;
- camera, audio, heartbeat, thermal adaptation, and Room logging continue normally;
- leaving PetRobot still releases foreground camera and microphone sensing as before.

The face baseline does not yet start conversation, move a mouth, or react to sensing state. Those are
separate v1 tasks.
