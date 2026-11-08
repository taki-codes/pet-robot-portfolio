## Summary

Describe the requested change and why it belongs in the current milestone.

## Verification

- [ ] `.\gradlew.bat test`
- [ ] `.\gradlew.bat lint`
- [ ] `.\gradlew.bat assembleDebug`
- [ ] `.\scripts\verify-v0-guardrails.ps1`
- [ ] Physical Xiaomi Pad verification completed when Android behavior changed

## v0 guardrails

- [ ] No `INTERNET` permission
- [ ] No raw image, camera frame, audio, transcript, or biometric persistence
- [ ] No STT, TTS, LLM, wake word, personal recognition, React Native, or server scope
- [ ] New dependencies are listed and justified
- [ ] Existing baseline behavior is preserved unless this ticket explicitly changes it
