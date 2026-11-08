# PetRobot v0 scope

## Purpose

Collect reliable local evidence for presence and interaction-boundary timing before adding autonomous speech.

## Included later in v0

- Foreground service and heartbeat
- CameraX frame acquisition
- Y-plane frame difference
- ML Kit visible face count
- AudioRecord volume envelope and VAD-lite
- Pure presence and break-candidate state machines
- Thermal adaptation
- Structured Room logs
- Three-button timing evaluation
- Daily and seven-day summaries
- Log replay with alternative thresholds

## Explicitly excluded

- Network communication and `INTERNET` permission
- STT, TTS, LLM, wake word
- Personal recognition and face embeddings
- Raw image or audio persistence
- React Native and server components

## Initial validation gates

1. Baseline Compose app installs and runs on Xiaomi Pad.
2. Baseline display remains alive for one hour while charging.
3. Foreground-service heartbeat build survives three days.
4. Camera build survives 24 hours before the seven-day study.
