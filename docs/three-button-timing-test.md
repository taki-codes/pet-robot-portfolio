# Three-button timing evaluation physical-device test

This procedure verifies that the user can label a break-candidate timing as too early, just right,
or too late and that PetRobot persists only the selected label, the current break state, and the
elapsed time since that state changed.

## Build and install

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
& "C:\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :app:installDebug --no-daemon --console=plain
```

Install over the T-011 build without uninstalling it. This exercises the Room 1-to-2 migration and
must preserve the existing `petrobot-v0.db` rows.

## Button and storage check

Start PetRobot, confirm that the screen displays these three buttons, and press each once:

- `早すぎる`
- `ちょうどよい`
- `遅すぎる`

Then inspect the logs:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Read-Host "3つの評価ボタンを1回ずつ押し、完了したらEnterを押してください"
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotTiming:I PetRobotStorage:I "*:S"
```

Pass when all three ratings appear:

- `rating=TOO_EARLY`
- `rating=JUST_RIGHT`
- `rating=TOO_LATE`

Each line must also contain `breakState=` and `offsetMs=`. Storage logs must show one `type=TIMING_EVALUATION` entry for every button press. An
`offsetMs=unavailable` value is valid only before the first break-state observation.

## Migration and restart check

Force-stop and restart PetRobot again. Pass when `databaseReady rows=...` is not lower than the
count observed before installing T-012 and no Room migration error appears. Do not clear app data or
uninstall between T-011 and this test.
