# Visible face-count test

## Purpose

Verify that PetRobot uses on-device ML Kit face detection to report only the number of faces visible in each sampled front-camera frame.

The bundled detector runs without landmarks, contours, classifications, or tracking. PetRobot does not expose or persist camera pixels, face features, biometric templates, or identifiers.

## Preparation

1. Build and install the T-006 debug APK.
2. Open PetRobot and keep the activity visible.
3. Prepare scenes with no face, one face, and two visible faces. A second person or a clear face photo may be used only for this functional check.
4. Keep each scene steady long enough for a sparse log entry.

## Evidence

From PowerShell, clear the existing log, open PetRobot, wait for each scene, and inspect the camera tag:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 30
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotCamera:I "*:S"
```

Each sparse camera entry includes `faces=N`, where `N` is a non-negative count for that frame.

## Pass criteria

- an empty scene reports `faces=0`;
- one clearly visible face reports `faces=1`;
- two clearly visible faces can report `faces=2`;
- covering or removing a face makes the count return to zero;
- frame acquisition and Y-plane difference logging continue;
- leaving PetRobot stops frame analysis;
- no tracking ID, face landmark, contour, classification, image, or biometric data is emitted or persisted;
- the APK has no `INTERNET` permission.

Lighting, pose, occlusion, and face size can affect detection. This task validates a per-frame visible-face count only; personal recognition, identity continuity, presence-state decisions, persistence, and threshold tuning belong to later tasks.
