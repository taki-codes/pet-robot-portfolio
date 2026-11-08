> T-034 update: third-tap affection was removed. Every tap uses the normal meow; stroking still plays 猫の甘え声1 once. References below to the old tap cycle describe the original T-033 checks; use [current tap checks](idle-cat-voices-test.md).

# T-033: stroke the face for an affectionate voice

## Scope and acceptance

One-finger dragging on the existing face for about two seconds plays Taira Komori's 猫の甘え声1 (miina_affection.wav) once. Original face visuals and controls remain unchanged. T-032 taps still cycle ordinary, ordinary, affectionate.

- Compose drag slop must be crossed first. Meaningful motion is measured in 4 dp steps after drag starts; two seconds of motion are required.
- A gap over 350 ms between meaningful motions resets the two-second progress. Holding still and tiny jitter cannot trigger an affectionate voice.
- No delayed timer: playback only starts on a motion event meeting the threshold.
- One attempt per drag even if playback is busy/unavailable; lift and start a new drag to retry.
- Drag cancellation, leaving the face, pointer replacement and lifecycle session restart discard progress.
- Drag consumes motion so lifting after a stroke does not play a tap or advance the T-032 sequence.
- A shared guard skips affectionate voice if another local sound is active; for 1.7 seconds after affectionate voice, tap/keyword/arrival playback cannot overlap it. A blocked tap logs unavailable and does not advance the sequence. Existing visual reactions can still occur.
- Sound playback uses the existing 0.97/1/1.03 rate variation; no loop.
- No new dependencies, permissions, network requests, captured media or pointer-coordinate persistence.
- Ten-minute/one-hour idle sounds are a later task.

## Automated verification

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
```

JVM tests cover exact two-second threshold, one shot per drag, pauses, stillness/jitter, separate drags, reversals, timestamp boundaries, busy/unavailable playback, guard duration, tap sequence isolation and actual WAV duration at the slowest rate.

## Xiaomi Pad 7 Pro

```powershell
.\gradlew.bat :app:installDebug --no-daemon --console=plain
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
& $adb shell am force-stop com.taki.petrobot
& $adb shell am start -n com.taki.petrobot/.MainActivity
```

Wait five seconds, set a comfortable media volume, then:
1. Slide one finger left/right over the face continuously for about three seconds: hear 猫の甘え声1, the same recording as the third tap. Continue to ten seconds without lifting: no second affectionate voice.
2. Lift, wait two seconds, repeat: one new affectionate voice. Releasing a drag must not play a tap voice.
3. Hold a finger still for three seconds: no affectionate voice. A release may retain the existing tap behavior; this is not a stroke. A short swipe under two seconds must not affectionate voice.
4. Stroke for one second, stop for one second, then resume: require about two more seconds of motion. Separate short swipes must not accumulate.
5. During a stroke, go Home/reopen or turn screen off/on: progress must reset. Confirm controls still open/close and their buttons do not affectionate voice.
6. After restarting, make one normal tap; wait, make a full stroke; wait, tap again: second normal voice; wait, tap: affectionate voice. A stroke does not advance the tap counter.
7. Immediately after affectionate voice, tap: no overlapping voice; after two seconds, normal tap behavior resumes. Say ミーニャ after silence and confirm original voice. Watch for self-trigger while remaining silent.
8. Confirm original greeting after at least three minutes ten seconds continuously RESTING, and suppression after a short absence.

```powershell
& $adb logcat -d -s PetRobotFace:I PetRobotMeow:I PetRobotPurr:I PetRobotAudio:I "*:S"
```

Affection logs strokePurr=played (or busy/unavailable); it must not emit touchPurr=played/affection_played on release. Audio scalar windows should be absent inside its 1.7-second guard; sparse logs alone cannot prove all captured samples were excluded.

Physical test results are pending until the user reports them. Stop on affectionate voice repeated in one drag, affectionate voice from stillness, tap-count regression, overlapping audio, self-trigger, controls interference or background gesture resumption.


## Revised sound choice (2026-09-29)

The owner changed sustained stroking from purr to the existing 猫の甘え声1. The stroke sound uses miina_affection.wav, exactly the same recording as the third tap; no new audio processing. The existing strokePurr log/class names are retained. Keep the once-per-drag rule and 1.7-second guard. Earlier purr audibility feedback does not validate this revised mapping; device verification is pending.

Future separate task: after the last interaction, 10 minutes -> adjusted Cat short purr once; 60 minutes -> owner-supplied 悲しげ.mp3 once; then no more until interaction. The supplied sad voice is from 小森平, https://taira-komori.net/animals01.html (猫（悲しげに）); the page lists 4a and 4b, so the exact variant remains to be identified when integrating the supplied file. Site terms https://taira-komori.net/welcome.html allow application embedding and conversion (checked 2026-09-29). No idle playback is implemented in T-033.
