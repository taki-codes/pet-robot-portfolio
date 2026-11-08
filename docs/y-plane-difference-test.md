# Y-plane frame-difference test

## Purpose

Verify that PetRobot converts consecutive front-camera Y planes into a normalized scalar difference without displaying, exposing, or persisting camera pixels.

## Preparation

1. Build and install the T-005 debug APK.
2. Open PetRobot and keep the activity visible.
3. Aim the front camera at a still scene, then move a hand through the camera view.

## Evidence

From PowerShell, clear the existing log, open PetRobot, wait several seconds, and inspect the camera tag:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotCamera:I "*:S"
```

The first acquired frame reports `yDiff=baseline`. Later sparse log entries report a normalized `yDiff` between `0.0000` and `1.0000` and a positive sample count.

## Pass criteria

- the first frame establishes a baseline;
- later frames report a finite normalized Y-plane difference;
- a still scene produces small values;
- visible motion produces a larger value than the still scene;
- frame dimensions and sample counts stay positive;
- leaving PetRobot stops frame analysis;
- no raw image, Y plane, or difference image is persisted;
- the APK has no `INTERNET` permission.

This task validates only the scalar frame-difference signal. Threshold tuning, presence state, face detection, audio, and persistence belong to later tasks.
