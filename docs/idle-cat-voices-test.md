# T-034: random foreground purr and idle sad voice

## Acceptance
- Each resumed Miina display session draws one random integer delay from 600 through 1800 seconds, inclusive. At that deadline purr plays once regardless of face presence, camera permission, or interaction.
- Taps, stroking and accepted name reactions do NOT reschedule or rearm purr.
- Independently, sad plays once after 3600 seconds without taps, drag movement or an accepted name reaction. Interaction resets/rearms only sad. It retains the PRESENT requirement with observation receipt age <=2 seconds.
- Ordinary conversation, presence changes, greeting and the app's own audio never reset the interaction clock.
- Pause/Home/screen-off removes the timer callback, disarms the session and releases idle audio players (stopping playback). Resume draws a new purr deadline and starts a fresh sad clock. No background time or persistence.
- A main-thread one-second callback checks deadlines independently of camera frames. Playback checks screen interactive state.
- Shared audio guards defer due playback until other audio ends; purr/sad guards are 1.7/8.5 seconds. If both are due, purr is first and sad waits for its guard; no overlap.
- Resource failure consumes that one attempt. No repeated error/retry every second.
- Existing UI, normal tap voice, stroke affection, keyword and arrival remain.
- No runtime network, dependencies, permissions or raw captured media persistence.

## Automated checks
```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
```
Tests cover random delay endpoints, interaction independence, once-only voices, sad clock/presence, lifecycle, busy/failure, overlap, backward time and bundled audio sizes/durations.

## Xiaomi Pad 7 Pro
1. Restart with logs cleared. Read purrScheduledSeconds (600–1800). This is measured from display resume.
2. Tap, stroke and call ミーニャ during that period: purr should still play once at the originally logged deadline (up to one timer tick late, or deferred by other audio).
3. Repeat a session without a visible face / without camera permission: purr must still play.
4. After a last tap, avoid face interaction and name calls for one hour while visible: hear full sad recording once (~8 seconds). Purr during this hour does not move the sad deadline.
5. Wait another ten minutes: no repeated purr or sad. Interact and verify a new sad interval; purr remains consumed for that display session.
6. Home or screen-off before the purr deadline; remain there across it: no sound. Reopen: new purrScheduledSeconds and fresh countdown; no immediate catch-up.
7. Home/screen-off during either sound: stop it. Return and verify new session.
8. Tap/stroke during idle playback: no voice overlap, but sad timer resets. Normal voices recover after the guard.
9. Verify all taps (including third and sixth, spaced over two seconds) use the normal meow, stroke affection, name response and three-minute arrival greeting.
10. Verify sad stays silent without a current PRESENT observation; return can trigger overdue sad once. Purr ignores presence.

```powershell
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
& $adb shell am force-stop com.taki.petrobot
& $adb shell am start -n com.taki.petrobot/.MainActivity
& $adb logcat -d -s PetRobotIdle:I PetRobotFace:I PetRobotMeow:I PetRobotPurr:I "*:S"
```
Logs: purrScheduledSeconds, idleVoice=purr_played, idleVoice=sad_played, or unavailable.
Device validation is pending. A played log alone does not prove audibility. Use real timings; no production test shortcut.

## T-034 follow-up: normal taps and retained diagnostic status
Third-tap affection has been removed at the owner's request. Stroke affection remains. Tap cooldown stays two seconds.

Every 60 seconds PetRobotIdle logs aggregate in-memory session status: purr/sad pending, played or unavailable, remaining seconds, screen interactive and presence flags. Pause logs session status once. This makes earlier results inspectable even if the original playback line has rotated out of logcat; no media/transcripts are recorded. A played result still requires listening confirmation. A new display session resets these fields.

For a new real-time test, keep PowerShell logcat running (Ctrl+C to stop); do not clear/restart midway:
```powershell
& $adb logcat -v time -s PetRobotIdle:I "*:S"
```
The prior supplied dump contained recent face logs around 19:55–19:59 but not the expected purr time around 19:25. It cannot prove playback failure or success. Device idle-voice testing remains pending.
