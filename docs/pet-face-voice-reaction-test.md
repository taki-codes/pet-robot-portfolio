# Pet-face “ミー / ミーニャ” keyword reaction test

## Goal

Verify that Miinya reacts when Android's on-device Japanese recognizer detects either the standalone
short call “ミー” or the full name “ミーニャ”. Existing microphone PCM is streamed through an
in-memory pipe; raw audio and recognition hypotheses are never logged or persisted, and the app
requests no INTERNET permission.

## Automated checks

Run:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
git status --short
```

Expected:

- the build succeeds and guardrails pass;
- “ミー”, “みー”, “ミーニャ”, “みーにゃ”, and the recognizer’s exact short aliases “ミ” / “ミイ” match;
- unrelated speech, “ミーティング”, “見て”, “みんな”, “おみくじ”, “みーちゃん”, and “ミーニャちゃん” do not match;
- repeated recognizer callbacks inside the 1.5-second cooldown produce one reaction;
- the eyes and three yellow attention marks return to the current mood after about 0.8 seconds;
- the marks switch without fading, an afterimage, or a duplicate eye layer.

## Xiaomi Pad check

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.taki.petrobot
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.taki.petrobot/.MainActivity
Start-Sleep -Seconds 5
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotFace:I "*:S"
```

Continue only if `keywordRecognition=ready` appears. If `keywordRecognition=unavailable` or an
error code appears, install the Japanese on-device speech model before retesting.

Say unrelated phrases and confirm no reaction. Then say “ミー” once and “ミーニャ” once, leaving
more than 1.5 seconds between them. For each call, confirm:

1. the eyes become larger and brighter once;
2. three yellow attention marks appear above and to the right;
3. the reaction returns to the current mood after about 0.8 seconds;
4. continued speech does not cause rapid flashing;
5. there is no fading, afterimage, or duplicate eye layer.

Capture evidence:

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" logcat -d -s PetRobotFace:I PetRobotAudio:I "*:S"
```

Expected evidence contains `keywordReaction=triggered keyword=mi` for both allowed calls and no
recognized text.

## Stop condition

Stop if unrelated speech triggers, either “ミー” or “ミーニャ” never triggers while recognition is
ready, the reaction repeats rapidly during one utterance, or any speech text appears in logs or
storage.
