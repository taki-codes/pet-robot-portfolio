# T-029 arrival meow test

## Behavior and acceptance

The existing visual arrival greeting is unchanged. A transition from RESTING to any active face mood requests one bundled cat meow only after at least **180 continuous seconds in RESTING** while the activity is started. Count from the RESTING mood, not from the moment a person leaves the room.

- Initial active state is silent. Initial RESTING starts a new observation period.
- Rest duration uses sensing observation timestamps, so callback delivery delays cannot turn a return before 180 seconds into an eligible greeting. Playback exclusion starts at actual playback time.
- Repeated RESTING updates do not restart the timer.
- A return before 180 seconds is silent and consumes that rest period.
- Remaining active or changing active moods does not replay the greeting.
- A subsequent greeting requires a new full 180-second rest period.
- Backgrounding/restarting the activity clears arrival timing; background time is not counted.
- If another local sound's capture guard is active, skip the greeting. Failed/not-loaded playback is also consumed without delayed retries.
- Successful playback uses the existing keyword cat asset and shared 0.9-second capture guard.
- Resetting arrival timing does not clear a guard for audio still playing.
- No new permissions, dependencies, networking, raw media, transcripts, or face-data storage.

## Automated checks

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
```

ArrivalMeowCoordinatorTest covers the 180-second boundary, continuous/fragmented rest, one-shot playback, repeat visits, unavailable/busy playback, and lifecycle resets. Existing asset tests check the WAV fits the echo guard. CI cannot establish physical audibility or absence of acoustic feedback.

## Xiaomi Pad check

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
& $adb shell am force-stop com.taki.petrobot
& $adb shell am start -n com.taki.petrobot/.MainActivity
```

1. Start with a person visible. Confirm no immediate arrival sound.
2. Leave the view until the log shows mood=RESTING. Return after about 30 seconds. Confirm the existing visual greeting but no arrival sound; expect arrivalMeow=short_rest.
3. Leave again, confirm mood=RESTING, and remain away for at least 3 minutes 10 seconds **after that log**. Keep the app in the foreground. If another mood appears, start the wait again from the next RESTING.
4. Return without touching or calling the name. Confirm one selected cat meow and the existing greeting expression. Expect arrivalMeow=played.
5. Remain visible and quiet for at least 10 seconds. Confirm no second meow or keyword self-trigger. Continue remaining visible for a minute and confirm no repeated arrival sounds.
6. Confirm taps and the name call ミーニャ still play the selected voice.
7. While RESTING, background the app for over 3 minutes, then reopen with a person visible. Confirm no arrival sound based on background time.
8. If playback fails or logs unavailable, record the failure. busy means another local sound was protected at that instant; retry with a fresh full rest period.

```powershell
& $adb logcat -d -s PetRobotFace:I PetRobotMeow:I PetRobotPurr:I PetRobotAudio:I "*:S"
```

Record the APK commit, elapsed rest time, audible result, and logs. No PetRobotAudio window should be logged during the 0.9-second playback guard; sparse logs alone cannot prove all internal capture handling. Stop for repeated greetings while active, a greeting before 180 seconds of continuous rest, background-time greetings, self-trigger loops, broken existing reactions, or forbidden persistence/network access.
