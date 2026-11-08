# T-036: opt-in offline singing-candidate experiment

## Scope and limitations

This ticket tests whether a lightweight sustained-pitch heuristic is useful on the
Xiaomi Pad 7 Pro. It is NOT a validated singing/speech classifier. There is no automatic
meow, UI change, speech transcription, model download, new dependency, or network permission.
The user explicitly authorized this diagnostic step toward the singing feature; it does
not implement later conversation/singing responses described in the broader roadmap.

Ordinary launches do not run the experiment. Only a debuggable APK launched with the
`singing_probe=true` intent extra enables it. Home/screen OFF pauses and clears evidence;
returning to the same opted-in activity starts with fresh evidence. A new normal launch
after force-stop disables it. Release APKs ignore the extra.

The existing 16 kHz AudioRecord window is reused synchronously, after the existing keyword
recognizer callback. No second microphone is opened. A bounded 1,001-sample frame uses
YIN-style normalized differences to estimate periodic pitch in 80–800 Hz, gated at
approximately -40 dBFS AC RMS. At most 20 timestamped scalar observations are retained.
No PCM, pitch contour, transcript or identity is logged or stored. Diagnostics go only
to logcat, not Room. Scratch differences are reused; incoming PCM is not retained.

Reference: de Cheveigne & Kawahara (2002), [YIN](https://www.ee.columbia.edu/~dpwe/papers/deChevK02-yin.pdf),
DOI 10.1121/1.1458024. This implementation uses fixed-width squared differences,
cumulative mean normalization and the first local minimum below 0.15. It omits the
paper's interpolation/refinement and returns no pitch if the threshold is not met.
The subsequent singing-candidate thresholds are experimental project choices, not claims
from the paper: about 2 seconds of observations, at least 14/20 periodic windows and
9 stable adjacent pairs (within 1.5 semitones). The current window must be periodic.

Long vowels in ordinary speech, instruments, fans and TV/music can all look periodic.
Quiet singing, rapid notes, vocal fry and polyphonic music may be missed. This experiment
does not identify the singer, distinguish live sound from a speaker, or prove singing.
If real speech cannot be distinguished adequately, do not connect this heuristic to
automatic pet behavior; report that finding before proposing another detection approach.

The user requested this relaxation from 16/20 and 12 pairs after the initial device
trial. Observation duration, pitch estimation and self-playback suppression are unchanged.
Recheck ordinary conversation as well as singing: relaxed thresholds can increase false positives.

## Automated checks

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
```

Tests cover tone frequencies/harmonics, silence/DC/quiet tones/noise, valid sample bounds,
the exact 14-window/9-pair boundary and either count just below it,
short voiced bursts, changing notes, discontinuities, insufficient elapsed observation
time, lifecycle resets, playback suppression including the window across its end, and
screen-off/short-window invalidation. Synthetic tests verify logic, not recognition accuracy.

## Device launch

Keep the usual media volume. Use airplane mode for the offline check and leave camera
permissions/settings unchanged. Verify the existing keyword recognizer is available
on-device as in T-023; T-036 does not alter its provisioning or fall back to a server.

```powershell
cd C:\Dev\pet-robot
git fetch origin
git switch codex/t-036-singing-candidate-probe
git pull --ff-only
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
.\scripts\verify-v0-guardrails.ps1
.\gradlew.bat :app:installDebug --no-daemon --console=plain
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
& $adb shell am force-stop com.taki.petrobot
& $adb shell am start -W -n com.taki.petrobot/.MainActivity --ez singing_probe true
& $adb logcat -v time -s PetRobotSinging:I PetRobotFace:I PetRobotMeow:I PetRobotPurr:I PetRobotAudio:E AndroidRuntime:E "*:S"
```

Expected initial message: `probe=active mode=experimental_pitch` followed by
`probe=warming candidate=false ready=false ...`. Status is emitted on state changes and
approximately once per second. `probe=candidate candidate=true` is only a candidate.
`probe=other ... ready=true` means the rolling observation window is full but does not pass.
`reason=local_playback` / `screen_off` means deliberate suppression, not a negative trial.
`reason=short_window` means insufficient samples; repeated occurrences require investigation.

## Short comparison (no audio recording)

For each of the following, do three 10-second trials from the usual seating distance,
with at least 3 seconds quiet between trials. Note the start time and whether a candidate
appeared during the trial. Do not say the pet's name within the comparison trials.

| Input | What to record |
| --- | --- |
| Quiet room | False candidates out of 3 |
| Ordinary conversation/read aloud | False candidates out of 3 |
| Humming with sustained notes | Detected trials out of 3 and approximate delay |
| Singing a short phrase with held notes | Detected trials out of 3 and approximate delay |
| TV/music from another device | Candidate count; this probe cannot identify the source |

Provisional usefulness gate: quiet 0/3, ordinary conversation at most 1/3,
humming and singing each at least 2/3. These are a small feasibility screen, not an
accuracy benchmark. If the gate fails, retain the experiment on the branch and revise or
abandon it; do not describe the singer detector as accepted. Record TV/music ambiguity
even if the gate passes. No singing-triggered playback is expected in any case.

## Regression, self-sound and lifecycle

1. While quiet, tap, stroke and call ミーニャ. Hear the existing correct sounds and verify
   the face responds normally. Check `reason=local_playback` where capture is running.
   No candidate should be produced from the pet's own playback. After suppression ends,
   new observations must warm up; old voiced evidence must not carry over.
2. Make a candidate, go Home for 10 seconds, and return. Expect `probe=paused`, then
   `probe=active` and fresh warming. Repeat with screen OFF. No candidate logs while paused.
3. Sing for 2 minutes; verify responsive face/touch/name recognition and no audio sensing
   errors, crash or perceptible lag. If lag or thermal trouble appears, report it and stop
   the experiment; no sustained-load claim can be made from JVM tests or CI.
4. Ctrl+C stops the log stream. Disable the probe with the normal launch below. Repeat
   one tap/stroke/name check and confirm there are no new PetRobotSinging logs.

```powershell
& $adb shell am force-stop com.taki.petrobot
& $adb logcat -c
& $adb shell am start -W -n com.taki.petrobot/.MainActivity
& $adb logcat -d -s PetRobotSinging:I AndroidRuntime:E "*:S"
```

## Evidence / acceptance

Record tested commit, commands/results, device/OS, offline mode, trial counts,
self-sound/lifecycle/regression observations and limits. Physical results are pending
until the user runs the test; CI cannot establish singing recognition accuracy.
Do not merge until the user approves the actual results and explicitly requests merging.
