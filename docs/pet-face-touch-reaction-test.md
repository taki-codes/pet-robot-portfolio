# Pet face touch-delight verification

T-019 extends the existing tap-to-look behavior with a short, visible delight reaction.
A tap still moves the eyes toward the touched point. The eyes also grow briefly and pink
cheeks appear, then the face returns to the sensing-driven mood in about 900 ms.

The reaction is UI-only. It does not add network access, speech, identity recognition,
raw-media storage, or persistent touch history.

## Automated checks

Run from PowerShell at the repository root:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Expected result:

- Gradle reports `BUILD SUCCESSFUL`.
- The guardrail script passes.
- `git status --short` prints nothing.
- JVM tests cover the neutral, peak, settling, and clamped touch visuals.

## Xiaomi Pad check

Install and start the debug build:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
```

Verify all of the following on the physical Xiaomi Pad 7 Pro:

1. Tap near the left, center, and right edges of the face.
2. The eyes look toward each tapped point.
3. Each tap briefly enlarges the eyes and shows two pink cheeks.
4. The cheeks disappear and the previous sensing-driven eye color and size return.
5. Tap the same point several times quickly; every tap restarts the reaction without a crash.
6. The timing-evaluation buttons and seven-day replay button still work.

A physical-device pass requires direct visual observation; CI and emulator results do not
replace this check.
