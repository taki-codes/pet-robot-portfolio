# Pet-first collapsible controls test

T-021 keeps the animated pet face as the primary surface. Timing evaluation controls stay collapsed until the user explicitly opens them.

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
- `PetControlPanelStateTest` verifies open, close, action-completed, and repeated-open transitions.

## Xiaomi Pad check

Install and restart the debug app:

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

Verify on the physical Xiaomi Pad:

1. The app initially shows the pet face without the large timing-evaluation panel.
2. A small `評価メニュー` button appears at the bottom and does not cover the face.
3. Tapping `評価メニュー` opens the existing three timing choices and seven-day replay action.
4. Tapping `閉じる` returns to the face-first display.
5. Reopen the panel and tap one timing choice. Confirm the panel closes and the timing event is logged.
6. Reopen the panel and request seven-day replay. Confirm the panel closes and replay starts.
7. Confirm the face tap reaction and immersive system-bar behavior still work.

For the timing-log check:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotTiming:I PetRobotReplay:I "*:S"
```

Expected result: the selected timing rating and requested replay appear in logcat while the face remains the default screen.

## Evidence

Record the device, APK commit, commands, results, and screenshots or observations. Physical-device acceptance requires the Xiaomi Pad result; CI and previews are supporting evidence only.
