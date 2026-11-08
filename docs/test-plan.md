# v0 test plan

## Every pull request

1. Run JVM tests.
2. Run Android lint.
3. Assemble the debug APK.
4. Inspect the assembled APK for forbidden network permission.
5. Inspect sensing main source for forbidden direct persistence APIs.
6. Verify on the physical Xiaomi Pad when Android behavior changes.

## Reliability gates

- Baseline UI: one hour while charging.
- Foreground-service heartbeat: three days before adding sensing. Follow [the heartbeat test procedure](heartbeat-test.md).
- Camera frame acquisition: follow [the CameraX frame-acquisition procedure](camera-frame-test.md).
- Y-plane frame difference: follow [the scalar difference procedure](y-plane-difference-test.md).
- Visible face count: follow [the ML Kit face-count procedure](visible-face-count-test.md).
- Audio envelope and VAD-lite: follow [the microphone scalar-sensing procedure](audio-envelope-test.md).
- Presence state machine: follow [the scalar fusion procedure](presence-state-test.md).
- Break-candidate state machine: follow [the interaction-boundary procedure](break-candidate-test.md).
- Thermal load adaptation: follow [the thermal-status procedure](thermal-adaptation-test.md).
- Structured Room logs: follow [the local persistence procedure](structured-room-log-test.md).
- Three-button timing evaluation: follow [the manual timing-label procedure](three-button-timing-test.md).
- Daily and seven-day summaries: follow [the bounded aggregation procedure](daily-seven-day-summary-test.md).
- Alternative-threshold log replay: follow [the structured replay procedure](log-replay-test.md).
- Camera and audio sensing: follow [the 24-hour soak-test procedure](24-hour-sensing-test.md) before the seven-day study.
- Final v0 data collection: seven days at a fixed installation location.

## v1 interface gates

- Animated pet face baseline: follow [the LOOI-style face procedure](pet-face-ui-test.md).
- Sensing-driven face moods: follow [the face mood procedure](sensing-face-mood-test.md).
- Touch-delight reaction: follow [the touch reaction procedure](pet-face-touch-reaction-test.md).
- Immersive pet display: follow [the dedicated display procedure](immersive-pet-display-test.md).
- Pet-first controls: follow [the collapsible controls procedure](pet-first-controls-test.md).
- Arrival greeting reaction: follow [the returning-person reaction procedure](arrival-greeting-reaction-test.md).
- Keyword reaction: follow [the on-device “ミー” reaction procedure](pet-face-voice-reaction-test.md).
- Local meow reaction (T-026 / T-028, recorded keyword voice): follow [the bundled meow playback procedure](local-meow-reaction-test.md).
- Touch cat-voice reaction: follow [the recorded cat playback procedure](touch-purr-reaction-test.md).

- Arrival cat voice (T-029): follow [the three-minute rest greeting procedure](arrival-meow-test.md).

- Cat voice variation (T-030): follow [the subtle playback-rate procedure](cat-voice-variation-test.md).

- T-032 third-tap affection was superseded by T-034: taps now always use the normal voice. Verify third/sixth taps in [the current procedure](idle-cat-voices-test.md).

- Stroke affectionate voice (T-033): follow [the stroke gesture procedure](stroke-purr-test.md).

- Idle cat voices (T-034): follow [the random-purr/one-hour-sad procedure](idle-cat-voices-test.md).

## Opt-in experiments

- Singing candidate feasibility (T-036): follow [the offline diagnostic procedure](singing-candidate-test.md).
  This debug-only, explicitly enabled probe does not change normal pet behavior and is not
  a validated singing classifier. Device comparison results are required before adoption.

## Evidence

Record the command, result, device used, duration, and observed failure. Do not report physical-device validation from emulator or CI results.
