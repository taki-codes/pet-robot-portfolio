# Foreground-service heartbeat test

## Purpose

Verify that the dedicated Xiaomi Pad can keep the PetRobot foreground service alive for three continuous days before sensing is added.

This test does not use the camera, microphone, network, or raw-media storage.

## Preparation

1. Build and install the T-003 debug APK on the physical Xiaomi Pad.
2. Open PetRobot while the tablet screen is visible.
3. Allow notifications when Android asks.
4. Confirm that the ongoing notification says `PetRobot heartbeat`.
5. Keep the tablet charging at the fixed installation location used for the test.
6. Record the test start time and the notification's elapsed value.
7. Record any HyperOS battery setting changed for PetRobot. Use the same setting for the whole run.

Opening the app starts the service. Reinstalling the APK, force-stopping the app, rebooting the tablet, or clearing app data invalidates the run.

## Evidence checkpoints

At the start and after approximately 24, 48, and 72 hours, record:

- wall-clock date and time;
- notification elapsed value and heartbeat number;
- whether the notification remained present;
- whether the tablet rebooted or PetRobot was force-stopped;
- charging state and any visible thermal warning;
- a screenshot.

## Pass criteria

Pass only when all of the following are true:

- the notification remains present for at least 72 continuous hours;
- its elapsed value never resets;
- its heartbeat number continues increasing;
- the app does not crash or require manual restart;
- the tablet does not reboot during the run.

A missing notification, reset elapsed value, reset heartbeat number, crash, force-stop, reinstall, or reboot is a failed or invalid run. Record the observation before starting again.
