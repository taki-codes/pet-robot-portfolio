> Superseded by T-034 at the owner's request: all taps now play the normal meow. The following is historical T-032 documentation, not the current acceptance criteria. See [current procedure](idle-cat-voices-test.md).

# T-032: third-tap affectionate voice

## Purpose and acceptance

Keep the existing visual tap response. Successful audible taps cycle ordinary Cat Meow, ordinary Cat Meow, Taira Komori's 猫の甘え声1 (cat_sweet_voice1.mp3), then repeat. No simultaneous ordinary sound on the third tap.

- A two-second cooldown follows every successful playback. Taps during cooldown still keep existing visual feedback but do not play or advance the sequence.
- Count successful audible taps regardless of spacing during one foreground session. This is not a rapid triple-tap gesture.
- Failed playback leaves the count unchanged and starts no cooldown/guard.
- onStop resets the sequence to the first ordinary tap, preserving any active cooldown and shared microphone guard.
- Ordinary tap guard stays 1.2 seconds; affectionate guard is 1.7 seconds, covering the slowest playback rate plus 300 ms.
- Keyword and three-minute arrival behavior/assets remain unchanged.
- No new permission, dependency, captured media persistence or runtime network access.
- Stroke detection, idle 30/60-minute calls and expression synchronization are separate tasks.

## Automated checks

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
```

Unit tests exercise two cycles, ignored cooldown taps, failed third playback/retry, lifecycle sequence reset with cooldown/guard preserved, and actual WAV duration at the slowest rate.

## Xiaomi Pad 7 Pro acceptance

Install the branch APK; restarting the app resets the sequence.

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
& $adb shell am force-stop com.taki.petrobot
& $adb shell am start -n com.taki.petrobot/.MainActivity
```

1. Wait five seconds. Tap six times, leaving at least three seconds between taps. Hear ordinary, ordinary, affectionate, ordinary, ordinary, affectionate. Compare the third tap against the supplied cat_sweet_voice1.mp3. Check the affectionate clip finishes cleanly at a comfortable volume.
2. Restart. Tap once, then rapidly several times within one second. Wait three seconds and tap: this must be the second ordinary voice. Wait three seconds and tap: affectionate voice. Visual tap feedback should continue during cooldown.
3. Stay silent during playback; verify no keyword self-trigger. Logs should have no captured audio scalar windows within the respective playback guard. Sparse logs are supporting evidence, not proof for every microphone sample.
4. After at least two seconds of silence, call ミーニャ: existing keyword response and original meow.
5. After one or two accepted taps, go Home and return, wait three seconds, then tap: ordinary voice starts the sequence again.
6. Leave detection until RESTING continuously for at least three minutes ten seconds, then return. Hear the original greeting once; brief detection interruptions must not repeat it.

```powershell
& $adb logcat -d -s PetRobotFace:I PetRobotMeow:I PetRobotPurr:I PetRobotAudio:I "*:S"
```

Expected tap log cycle: touchPurr=played, played, affection_played. Ignored taps: cooldown. Failed playback: unavailable. catVoiceRate remains 0.97, 1.0 or 1.03.

Stop for wrong sequence, clipped/overlapping tap audio, lost visual feedback, keyword self-trigger, regression of original keyword/arrival voice, or network permission. Report device evidence separately from CI. See [audio sources](audio-sources.md) for credits.
