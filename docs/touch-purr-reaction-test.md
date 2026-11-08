# Touch cat-voice reaction test

> Historical T-027 baseline. T-032 supersedes the tap sound sequence: ordinary, ordinary, affectionate. Use [the current three-tap procedure](third-tap-affection-test.md) for acceptance; first/second taps retain the 1.2-second guard, third taps use 1.7 seconds.

## Goal

Verify that tapping the existing pet face keeps the T-019 visual delight reaction and plays one short bundled recorded cat meow. Playback remains entirely on-device, rapid taps do not stack sounds, and playback-contaminated microphone windows do not enter recognition, VAD state, audio logs, or structured logs.

## Automated checks

Run:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Expected:

- the build succeeds and guardrails pass;
- the first accepted face tap requests one cat meow;
- taps inside the two-second sound cooldown do not replay or extend the tap sound;
- the visual touch reaction remains independent and restarts on every tap;
- a successful tap sound excludes captured audio for 1.2 seconds;
- unavailable or not-yet-loaded audio does not start the cooldown or block visuals;
- the bundled sound is local and no `INTERNET` permission, TTS, or new dependency is added.

## Xiaomi Pad check

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
& $adb shell am force-stop com.taki.petrobot
& $adb shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 5
```

1. Set media volume to a comfortable level.
2. Tap the face once. Confirm the eyes move, enlarge, show pink cheeks, and one short cat meow is audible.
3. Tap repeatedly for two seconds. Confirm every tap restarts the visual delight reaction while the tap sound does not overlap or rapidly repeat.
4. Wait more than two seconds and tap again. Confirm one new cat meow is audible.
5. Wait more than 1.2 seconds after tapping, then say “ミーニャ”. Confirm the existing attention reaction and meow still work.
6. Leave the app and reopen it. Confirm the next face tap can play the tap sound.

Capture evidence:

```powershell
& $adb logcat -d -s PetRobotFace:I PetRobotPurr:I PetRobotMeow:I PetRobotAudio:I "*:S"
```

Expected evidence:

- the first tap logs `touchPurr=played`;
- rapid taps log `touchPurr=cooldown` without another `played`;
- a later tap logs a new `touchPurr=played`;
- no `PetRobotAudio` window is logged within 1.2 seconds after `touchPurr=played`;
- the keyword check logs one `keywordReaction=triggered keyword=mi` and one `keywordMeow=played`;
- no recognized text is logged or stored.

The tap sound is Mafon2's **Cat Meow** (Freesound 436541, CC0), approximately 0.572 seconds. See [audio sources](audio-sources.md) for provenance and conversion details. Confirm that its timbre matches the supplied recording and is audible without distortion on the tablet. The resource and log names retain `purr` for compatibility; this is a recorded meow, not a synthesized purr. The keyword meow asset is unchanged in this task.

## Stop condition

Stop if rapid taps overlap sounds, visual taps stop responding during cooldown, playback causes a keyword self-trigger, an audio/VAD sample appears inside the 1.2-second playback guard, the existing meow stops working, recognized text appears, or the APK requests network access.
