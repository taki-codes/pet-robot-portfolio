# Arrival greeting reaction test

T-022 gives the pet a short visual greeting when a person returns after the face has entered the resting state.

## Behavior

- A transition from `RESTING` to any non-resting face mood starts one 4-second greeting.
- The normal vertical eyes and cheeks switch on together immediately. Both stay fully opaque for four seconds, then switch off together immediately without fading.
- The normal-eye and smile-eye layers are mutually exclusive, so the vertical eyes never remain visible behind the arches during either transition.
- Initial composition does not count as a return.
- Changes between active moods do not retrigger the greeting.
- Touch reactions remain independent and take visual priority through the stronger cheek value.

## Automated checks

From the repository root, run:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Expected results:

- Gradle reports `BUILD SUCCESSFUL`.
- The v0 guardrail script passes.
- `git status --short` prints nothing.
- `PetFaceArrivalReactionTest` verifies the trigger boundary, neutral endpoints, peak visual, clamping, and mutually exclusive eye layers.

## Xiaomi Pad check

Install and restart the debug app:

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

Verify on the physical Xiaomi Pad:

1. Start with a person visible and confirm the face behaves normally without an immediate greeting loop.
2. Move fully out of view and wait until the face enters the blue `RESTING` mood.
3. Return in front of the camera.
4. Confirm the normal eyes change once into two bright upward arches (`∩ ∩`) like a happy closed-eye smile, and cheeks appear together immediately, remain fully opaque for four seconds, then switch directly to the normal eyes with both effects disappearing together.
5. Watch both the start and end of the smile and confirm no vertical-eye afterimage is visible behind the arches.
6. Remain visible and confirm normal mood changes do not repeatedly trigger the greeting.
7. Tap the face and confirm the existing tap gaze and cheek reaction still work.
8. Confirm the evaluation menu remains collapsed by default.

Use the existing face log to verify the return boundary:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotFace:I PetRobotPresence:I "*:S"
```

Expected evidence includes a `RESTING` face mood followed by a non-resting mood after the person returns.

## Evidence

Record the device, APK commit, commands, result, and observed greeting. Physical-device acceptance requires the Xiaomi Pad result; CI and previews are supporting evidence only.
