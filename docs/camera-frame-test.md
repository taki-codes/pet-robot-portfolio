# CameraX frame-acquisition test

## Purpose

Verify that the Xiaomi Pad front camera supplies YUV frames to `:sensing` without displaying or persisting raw images.

## Preparation

1. Complete the T-003 heartbeat run before installing this build on the physical tablet.
2. Build and install the T-004 debug APK.
3. Open PetRobot and allow camera access while using the app.
4. Keep the PetRobot activity visible during this acquisition check.

## Evidence

From PowerShell, clear the existing log and watch only the camera tag:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -s PetRobotCamera:I "*:S"
```

Confirm that output includes frame 1 and later frame counts with non-zero dimensions and Y-plane byte counts.

Example shape:

```text
frame=100 640x480 rotation=0 yBytes=307200
```

## Pass criteria

- camera permission is granted only after the user approves it;
- the front camera produces repeated frame observations;
- each observation has a positive width, height, and Y-plane byte count;
- every `ImageProxy` is closed after analysis;
- the app does not save frames or add network access;
- leaving or closing the activity releases camera acquisition.

This task validates acquisition only. Frame-difference calculations, face detection, audio, and persistence belong to later tasks.
