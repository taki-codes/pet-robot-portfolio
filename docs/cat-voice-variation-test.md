# T-030 subtle cat voice variation

## Scope and acceptance

Keep the selected Mafon2 recording and its gain unchanged. Each loaded playback request chooses 0.97, 1.00, or 1.03 playback rate; speed and pitch vary together. This is a subtle change, not a new voice or an emotion classifier.

Each player avoids the previous selected rate. Keyword replies and arrivals share the meow player; taps have their own selector. Rates may match across different players or after activity recreation. A failed playback request may consume a selection.

Successful playback logs catVoiceRate=0.97, 1.0, or 1.03 under PetRobotMeow or PetRobotPurr. Existing played/cooldown logs remain.

- No changes to reaction conditions, cooldowns, three-minute arrival threshold, UI, or source WAVs.
- Slowest playback lasts approximately 0.590 seconds, leaving over 300 ms within the 0.9-second keyword/arrival guard; the tap guard remains 1.2 seconds.
- No new dependencies, network permissions, or raw-media/transcript/face persistence.

## Automated checks

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
```

Selector tests verify the allowed rates, lack of consecutive duplicate selections, and reproducibility with injected randomness. The real WAV asset test checks longest playback against both existing guards with a 300 ms tail margin.

## Xiaomi Pad checks

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
& $adb shell am force-stop com.taki.petrobot
& $adb shell am start -n com.taki.petrobot/.MainActivity
```

1. Wait five seconds, then tap six times, with at least three seconds between taps. Confirm the selected cat identity remains, with slight differences in pitch/length and no distortion. Successful tap logs should contain changing catVoiceRate values in the allowed set.
2. Tap rapidly. Confirm the existing cooldown prevents rapid sound repetition while visual taps continue.
3. Say ミーニャ three times, waiting for each reply and at least three seconds before the next call. Confirm the same subtle variation and existing visual response.
4. Stay quiet for ten seconds after playback. Confirm no self-trigger loop.
5. Follow the T-029 arrival procedure: after continuous RESTING for at least three minutes ten seconds, return without tapping/calling. Confirm one greeting using the same voice and an allowed catVoiceRate. Shorter rests remain silent.
6. Listen at your normal media volume. If the variation sounds unnatural, report it before merge; logs prove selected rates, not perceived quality.

```powershell
& $adb logcat -d -s PetRobotFace:I PetRobotMeow:I PetRobotPurr:I PetRobotAudio:I "*:S"
```

Record commit, build/guardrail results, device, audible impression, and logs. Stop for distortion, repeated self-triggering, broken cooldowns/arrival timing, or forbidden network/persistence behavior.
