# Local meow reaction test (T-026 / T-028)

## Goal

Verify that an accepted “ミー / ミーニャ” keyword reaction plays one short bundled meow together with the existing visual reaction. Playback stays entirely on-device, adds no network permission or TTS, and does not store audio or recognized text.

## Automated checks

Run:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Expected:

- the build succeeds and guardrails pass;
- an accepted keyword callback triggers the visual reaction and one meow playback request;
- repeated callbacks inside the existing 1.5-second cooldown do not request another meow;
- captured audio is excluded from keyword recognition, VAD state, audio logs, and structured logs during the short local-playback echo guard;
- unavailable or not-yet-loaded audio does not block the existing visual reaction;
- stopping and returning to the activity resets the reaction gate;
- the APK has no `INTERNET` permission and contains no TTS or remote-audio dependency.

## Xiaomi Pad check

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 5
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotFace:I PetRobotMeow:I "*:S"
```

Continue only if `keywordRecognition=ready` appears in the `PetRobotFace` log.

1. Set media volume to a comfortable level.
2. Say an unrelated phrase and confirm that neither the existing visual reaction nor a meow occurs.
3. Say “ミーニャ” once. Confirm that the eyes and attention marks react once and the selected Mafon2 Cat Meow recording is audible once (the same voice as the tap reaction).
4. Wait more than 1.5 seconds, then say “ミー” once and confirm the same behavior.
5. Say an allowed call repeatedly inside 1.5 seconds and confirm that the meow is not rapidly repeated.
6. Stay quiet after playback and confirm that the bundled meow does not cause another visual or audio reaction. Confirm that the first `PetRobotAudio` window after `keywordMeow=played` is at least 0.9 seconds later; audio windows inside the guard must not be logged or stored.
7. Tap the face, wait more than two seconds, and tap again. Confirm the same selected cat voice and existing touch animation.
8. Background and reopen the app, then confirm one accepted call still produces one reaction.

Capture evidence:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotFace:I PetRobotMeow:I PetRobotAudio:I "*:S"
```

Expected evidence contains one `keywordReaction=triggered keyword=mi` and one `keywordMeow=played` for each accepted call, with no recognized text. It must not contain a `PetRobotAudio` window timestamped within 0.9 seconds after `keywordMeow=played`.

T-028 replaces the synthesized keyword sound with the same CC0 recording used for taps: Mafon2's Cat Meow, approximately 0.572 seconds. See [audio sources](audio-sources.md). The existing 0.9-second keyword playback guard and 1.5-second keyword cooldown remain unchanged.

The JVM asset tests check that keyword and tap WAVs match, are playable PCM16, and leave at least 300 ms within the keyword echo guard. A passing log or CI check does not establish audibility or absence of acoustic feedback; those require the listening checks above.

## Stop condition

Stop if unrelated speech plays the meow, one accepted call produces repeated sounds, playback causes a self-trigger loop, an audio/VAD sample is logged or stored inside the 0.9-second playback guard, the existing visual reaction stops working, recognized text appears in logs or storage, or the APK requests network access.
