# Immersive pet display verification

T-020 makes the Xiaomi Pad behave more like a dedicated pet terminal. While PetRobot has
window focus, the Android status bar and navigation bar are hidden and the display remains
awake. A swipe from the top or bottom edge temporarily reveals the system bars so the user
is never trapped in the app. The bars hide again after Android's transient timeout or when
PetRobot regains focus.

This change adds no device-owner, lock-task, accessibility, network, speech, recognition, or
media-storage capability.

## Automated checks

Run from PowerShell at the repository root:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Expected result:

- Gradle reports `BUILD SUCCESSFUL`.
- The v0 guardrails pass.
- `git status --short` prints nothing.
- The display-policy JVM test confirms that immersive bars are recoverable by edge swipe.

## Xiaomi Pad check

Install and launch the debug build:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

Verify on the physical Xiaomi Pad 7 Pro:

1. The clock/status icons at the top are hidden.
2. The navigation gesture bar at the bottom is hidden.
3. The black pet face fills the newly available area without clipping.
4. Swipe inward from the top edge; the status bar appears temporarily.
5. Swipe inward from the bottom edge; navigation controls appear temporarily.
6. Wait or return focus to PetRobot; the system bars hide again.
7. Tap the face and confirm the T-019 eye-and-cheek reaction still works.
8. Confirm the three timing buttons and seven-day replay button still work.

Do not call this a device pass based only on CI or an emulator.
