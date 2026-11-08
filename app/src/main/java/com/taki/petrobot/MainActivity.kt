package com.taki.petrobot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.taki.petrobot.sensing.AudioEnvelopeSample
import com.taki.petrobot.sensing.AudioLevelSource
import com.taki.petrobot.sensing.BreakCandidateObservation
import com.taki.petrobot.sensing.BreakCandidateState
import com.taki.petrobot.sensing.BreakCandidateStateMachine
import com.taki.petrobot.sensing.CameraFrameSample
import com.taki.petrobot.sensing.CameraFrameSource
import com.taki.petrobot.sensing.LogReplayEngine
import com.taki.petrobot.sensing.PresenceObservation
import com.taki.petrobot.sensing.PresenceState
import com.taki.petrobot.sensing.PresenceStateMachine
import com.taki.petrobot.sensing.PresenceUpdate
import com.taki.petrobot.sensing.ThermalLoadController
import com.taki.petrobot.sensing.ThermalObservation
import com.taki.petrobot.sensing.ThermalSeverity
import com.taki.petrobot.sensing.ReplayScalarSample
import com.taki.petrobot.sensing.defaultReplayProfiles
import com.taki.petrobot.storage.StorageModule
import com.taki.petrobot.storage.StructuredEventType
import com.taki.petrobot.storage.StructuredLogEntry
import com.taki.petrobot.storage.StructuredLogStore
import com.taki.petrobot.storage.StructuredLogSummary
import com.taki.petrobot.storage.currentSummaryWindows
import com.taki.petrobot.ui.theme.PetRobotTheme
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class MainActivity : ComponentActivity() {
    private val observedCameraFrames = AtomicLong(0L)
    private val observedAudioWindows = AtomicLong(0L)
    private val observedPresenceUpdates = AtomicLong(0L)
    private val observedBreakUpdates = AtomicLong(0L)
    private val observedThermalUpdates = AtomicLong(0L)
    private val observedStoredLogs = AtomicLong(0L)
    private val replayInProgress = AtomicBoolean(false)
    private val localAudioPlaybackGuard = LocalAudioPlaybackGuard()
    private var singingProbe: SingingProbe? = null
    private val keywordReactionCoordinator = KeywordReactionCoordinator(
        playMeow = { canPlayNonStrokeSound() && localMeowPlayer?.play() == true },
        playbackGuard = localAudioPlaybackGuard,
    )
    private val touchPurrCoordinator = TouchPurrCoordinator(
        playPurr = { canPlayNonStrokeSound() && localPurrPlayer?.play() == true },
        playbackGuard = localAudioPlaybackGuard,
    )
    private val arrivalMeowCoordinator = ArrivalMeowCoordinator(
        playMeow = { canPlayNonStrokeSound() && localMeowPlayer?.play() == true },
        playbackGuard = localAudioPlaybackGuard,
    )
    private val strokePurrCoordinator = StrokePurrCoordinator(
        playPurr = { localStrokePurrPlayer?.play() == true },
        playbackGuard = localAudioPlaybackGuard,
    )
    private val idleVoiceCoordinator = IdleVoiceCoordinator(
        play = { voice ->
            when (voice) {
                IdleVoice.PURR -> localIdlePurrPlayer?.play() == true
                IdleVoice.SAD -> localSadPlayer?.play() == true
            }
        },
        playbackGuard = localAudioPlaybackGuard,
    )
    private var localIdlePurrPlayer: LocalPurrPlayer? = null
    private var localSadPlayer: LocalPurrPlayer? = null
    private val idleHandler = Handler(Looper.getMainLooper())
    private var idleSessionActive = false
    private var nextIdleStatusNanos = 0L
    private var idlePresence = false
    private var idlePresenceObservedAtNanos = Long.MIN_VALUE
    private val idleTick = object : Runnable {
        override fun run() {
            if (!idleSessionActive) return
            val now = SystemClock.elapsedRealtimeNanos()
            if (powerManager.isInteractive) {
                val result = idleVoiceCoordinator.tick(
                    timestampNanos = now,
                    sadEligible = idlePresence &&
                        now - idlePresenceObservedAtNanos in 0L..2_000_000_000L,
                )
                if (result != IdleVoiceResult.NONE) {
                    Log.i("PetRobotIdle", "idleVoice=${result.name.lowercase(Locale.US)}")
                }
            }
            if (now >= nextIdleStatusNanos) {
                Log.i("PetRobotIdle", idleVoiceCoordinator.status(now) +
                    " interactive=${powerManager.isInteractive} present=$idlePresence")
                nextIdleStatusNanos = now + 60_000_000_000L
            }
            idleHandler.postDelayed(this, 1_000L)
        }
    }
    private var gestureSession by mutableStateOf(0L)
    private var localStrokePurrPlayer: LocalPurrPlayer? = null
    private val arrivalSession = AtomicLong(0L)
    private var arrivalMonitoringActive = false
    private val latestAudioSample = AtomicReference<AudioEnvelopeSample?>(null)
    private val latestBreakState = AtomicReference("UNKNOWN")
    private val latestBreakStateChangedAtNanos = AtomicLong(0L)
    private val latestPetFaceMood = AtomicReference<PetFaceMood?>(null)
    private var petFaceMood by mutableStateOf(PetFaceMood.RESTING)
    private var voiceReactionSequence by mutableStateOf(0L)
    private val presenceStateMachine = PresenceStateMachine()
    private val breakCandidateStateMachine = BreakCandidateStateMachine()
    private val thermalLoadController = ThermalLoadController()
    private var cameraFrameSource: CameraFrameSource? = null
    private var audioLevelSource: AudioLevelSource? = null
    private var miKeywordRecognizer: OnDeviceMiKeywordRecognizer? = null
    private var localMeowPlayer: LocalMeowPlayer? = null
    private var localPurrPlayer: LocalPurrPlayer? = null
    private var structuredLogStore: StructuredLogStore? = null
    private lateinit var powerManager: PowerManager
    private var thermalListenerRegistered = false

    private val thermalStatusListener = PowerManager.OnThermalStatusChangedListener { status ->
        handleThermalStatus(status)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startHeartbeatService()
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraFrameAcquisition()
            }
        }

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startAudioLevelCapture()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // T-036 is an explicitly requested debug experiment, disabled on ordinary launches.
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 &&
            intent.getBooleanExtra("singing_probe", false)
        ) {
            singingProbe = SingingProbe(localAudioPlaybackGuard) { message ->
                Log.i("PetRobotSinging", message)
            }
        }
        localMeowPlayer = SoundPoolLocalMeowPlayer(applicationContext)
        localPurrPlayer = SoundPoolLocalPurrPlayer(applicationContext)
        localStrokePurrPlayer = SoundPoolLocalPurrPlayer(applicationContext, R.raw.miina_affection)
        startStructuredLogging()
        enableEdgeToEdge()
        applyPetDisplayPolicy(DEDICATED_PET_DISPLAY_POLICY)
        setContent {
            PetRobotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimingEvaluationScreen(
                        mood = petFaceMood,
                        voiceReactionSequence = voiceReactionSequence,
                        onFaceTapped = ::handleFaceTap,
                        onFaceStroked = ::handleFaceStroke,
                        onFaceInteracted = ::recordFaceInteraction,
                        gestureSession = gestureSession,
                        onRatingSelected = ::recordTimingEvaluation,
                        onReplayRequested = ::runSevenDayReplay,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
        startThermalMonitoring()
        startHeartbeatAfterNotificationPermission()
        startCameraAfterPermission()
        startAudioAfterPermission()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && DEDICATED_PET_DISPLAY_POLICY.reapplyWhenWindowRegainsFocus) {
            applyPetDisplayPolicy(DEDICATED_PET_DISPLAY_POLICY)
        }
    }

    override fun onStart() {
        super.onStart()
        arrivalSession.incrementAndGet()
        arrivalMeowCoordinator.reset()
        arrivalMonitoringActive = true
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startAudioLevelCapture()
        }
    }

    override fun onResume() {
        super.onResume()
        singingProbe?.setActive(true)
        localIdlePurrPlayer = SoundPoolLocalPurrPlayer(applicationContext, R.raw.miina_stroke_purr)
        localSadPlayer = SoundPoolLocalPurrPlayer(applicationContext, R.raw.miina_sad)
        idlePresence = false
        idlePresenceObservedAtNanos = Long.MIN_VALUE
        val delay = idleVoiceCoordinator.startSession(SystemClock.elapsedRealtimeNanos())
        idleSessionActive = true
        nextIdleStatusNanos = 0L
        Log.i("PetRobotIdle", "purrScheduledSeconds=${delay / 1_000_000_000L}")
        idleHandler.post(idleTick)
    }

    override fun onPause() {
        singingProbe?.setActive(false)
        Log.i("PetRobotIdle", "session=paused " + idleVoiceCoordinator.status(SystemClock.elapsedRealtimeNanos()))
        idleSessionActive = false
        idleHandler.removeCallbacks(idleTick)
        idleVoiceCoordinator.stopSession()
        localIdlePurrPlayer?.close()
        localIdlePurrPlayer = null
        localSadPlayer?.close()
        localSadPlayer = null
        super.onPause()
    }

    override fun onStop() {
        gestureSession += 1L
        arrivalMonitoringActive = false
        localIdlePurrPlayer?.close()
        localIdlePurrPlayer = null
        localSadPlayer?.close()
        localSadPlayer = null
        arrivalSession.incrementAndGet()
        arrivalMeowCoordinator.reset()
        audioLevelSource?.close()
        audioLevelSource = null
        miKeywordRecognizer?.close()
        miKeywordRecognizer = null
        latestAudioSample.set(null)
        keywordReactionCoordinator.reset()
        super.onStop()
    }

    override fun onDestroy() {
        if (thermalListenerRegistered) {
            powerManager.removeThermalStatusListener(thermalStatusListener)
            thermalListenerRegistered = false
        }
        cameraFrameSource?.close()
        cameraFrameSource = null
        localMeowPlayer?.close()
        localMeowPlayer = null
        localIdlePurrPlayer?.close()
        localIdlePurrPlayer = null
        localSadPlayer?.close()
        localSadPlayer = null
        localStrokePurrPlayer?.close()
        localStrokePurrPlayer = null
        localPurrPlayer?.close()
        localPurrPlayer = null
        touchPurrCoordinator.reset()
        localAudioPlaybackGuard.reset()
        structuredLogStore?.close()
        structuredLogStore = null
        super.onDestroy()
    }

    private fun applyPetDisplayPolicy(policy: PetDisplayPolicy) {
        if (policy.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        var hiddenTypes = 0
        if (policy.hideStatusBar) {
            hiddenTypes = hiddenTypes or WindowInsetsCompat.Type.statusBars()
        }
        if (policy.hideNavigationBar) {
            hiddenTypes = hiddenTypes or WindowInsetsCompat.Type.navigationBars()
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (hiddenTypes != 0) {
            controller.hide(hiddenTypes)
        }
        if (policy.allowTransientSystemBarsBySwipe) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun startStructuredLogging() {
        val logStore = StorageModule.openStructuredLog(
            context = this,
            onStored = { eventType, rowId ->
                val storedCount = observedStoredLogs.incrementAndGet()
                if (
                    eventType == StructuredEventType.TIMING_EVALUATION ||
                    storedCount == 1L ||
                    storedCount % LOG_EVERY_N_STORED_EVENTS == 0L
                ) {
                    Log.i(
                        STORAGE_LOG_TAG,
                        "stored=$storedCount type=${eventType.name} rowId=$rowId",
                    )
                }
            },
            onError = { error ->
                Log.e(STORAGE_LOG_TAG, "Structured log storage failed", error)
            },
        )
        structuredLogStore = logStore
        logStore.count { rowCount ->
            Log.i(STORAGE_LOG_TAG, "databaseReady rows=$rowCount")
        }
        currentSummaryWindows(
            nowEpochMillis = System.currentTimeMillis(),
            zoneId = ZoneId.systemDefault(),
        ).forEach { window ->
            logStore.readSummary(window, ::logStructuredSummary)
        }
    }

    private fun logStructuredSummary(summary: StructuredLogSummary) {
        Log.i(
            SUMMARY_LOG_TAG,
            "period=${summary.window.period.name} " +
                "start=${summary.window.startEpochMillis} " +
                "end=${summary.window.endInclusiveEpochMillis} " +
                "total=${summary.totalEventCount} " +
                "camera=${summary.cameraSampleCount} " +
                "face=${summary.faceObservedSampleCount} " +
                "audio=${summary.audioSampleCount} " +
                "voice=${summary.voiceCandidateSampleCount} " +
                "present=${summary.presentTransitionCount} " +
                "breakCandidates=${summary.breakCandidateTransitionCount} " +
                "timing=${summary.timingEvaluationCount} " +
                "ratings=${summary.tooEarlyCount}/" +
                "${summary.justRightCount}/${summary.tooLateCount}",
        )
    }

    private fun appendStructuredLog(entry: StructuredLogEntry) {
        structuredLogStore?.append(entry)
    }

    private fun startThermalMonitoring() {
        powerManager = getSystemService(PowerManager::class.java)
        powerManager.addThermalStatusListener(mainExecutor, thermalStatusListener)
        thermalListenerRegistered = true
        handleThermalStatus(powerManager.currentThermalStatus)
    }

    private fun handleThermalStatus(status: Int) {
        val severity = status.toThermalSeverity()
        val update = thermalLoadController.update(
            ThermalObservation(
                timestampNanos = SystemClock.elapsedRealtimeNanos(),
                severity = severity,
            ),
        )
        cameraFrameSource?.setAnalysisStride(update.profile.cameraAnalysisStride)

        val updateCount = observedThermalUpdates.incrementAndGet()
        if (updateCount == 1L || update.profileChanged) {
            Log.i(
                THERMAL_LOG_TAG,
                "status=${severity.name} profile=${update.profile.name} " +
                    "cameraStride=${update.profile.cameraAnalysisStride}",
            )
            appendStructuredLog(
                StructuredLogEntry(
                    elapsedRealtimeNanos = update.timestampNanos,
                    wallTimeEpochMillis = System.currentTimeMillis(),
                    eventType = StructuredEventType.THERMAL_PROFILE,
                    thermalSeverity = severity.name,
                    sensingLoadProfile = update.profile.name,
                    cameraAnalysisStride = update.profile.cameraAnalysisStride,
                ),
            )
        }
    }

    private fun Int.toThermalSeverity(): ThermalSeverity = when (this) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalSeverity.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalSeverity.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalSeverity.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalSeverity.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalSeverity.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalSeverity.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalSeverity.SHUTDOWN
        else -> ThermalSeverity.NONE
    }

    private fun startHeartbeatAfterNotificationPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startHeartbeatService()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startHeartbeatService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, ReliabilityHeartbeatService::class.java)
        )
    }

    private fun startCameraAfterPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCameraFrameAcquisition()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraFrameAcquisition() {
        if (cameraFrameSource != null) return

        val source = CameraFrameSource(
            context = this,
            lifecycleOwner = this,
            onFrame = { frame ->
                logCameraFrame(frame)
                updatePresenceAndBreakCandidate(frame)
            },
            onError = { error ->
                Log.e(CAMERA_LOG_TAG, "Camera frame sensing failed", error)
            },
        )
        source.setAnalysisStride(thermalLoadController.profile.cameraAnalysisStride)
        source.start()
        cameraFrameSource = source
    }

    private fun logCameraFrame(frame: CameraFrameSample) {
        val frameCount = observedCameraFrames.incrementAndGet()
        if (frameCount == 1L || frameCount % LOG_EVERY_N_FRAMES == 0L) {
            val difference = frame.meanAbsoluteYDifference?.let {
                String.format(Locale.US, "%.4f", it)
            } ?: "baseline"
            Log.i(
                CAMERA_LOG_TAG,
                "frame=$frameCount ${frame.width}x${frame.height} " +
                    "rotation=${frame.rotationDegrees} " +
                    "yBytes=${frame.yBufferBytes} " +
                    "yDiff=$difference samples=${frame.sampledYPixelCount} " +
                    "faces=${frame.visibleFaceCount}",
            )
            appendStructuredLog(
                StructuredLogEntry(
                    elapsedRealtimeNanos = frame.timestampNanos,
                    wallTimeEpochMillis = System.currentTimeMillis(),
                    eventType = StructuredEventType.CAMERA_SAMPLE,
                    visibleFaceCount = frame.visibleFaceCount,
                    meanAbsoluteYDifference = frame.meanAbsoluteYDifference,
                ),
            )
        }
    }

    private fun updatePresenceAndBreakCandidate(frame: CameraFrameSample) {
        val audio = latestAudioSample.get()
        val audioAgeNanos = audio?.let { frame.timestampNanos - it.timestampNanos }
        val freshVoiceCandidate = audio != null &&
            audioAgeNanos != null &&
            audioAgeNanos in 0L..AUDIO_EVIDENCE_FRESHNESS_NANOS &&
            audio.voiceCandidate

        val presenceUpdate = presenceStateMachine.update(
            PresenceObservation(
                timestampNanos = frame.timestampNanos,
                visibleFaceCount = frame.visibleFaceCount,
                meanAbsoluteYDifference = frame.meanAbsoluteYDifference,
                voiceCandidate = freshVoiceCandidate,
            ),
        )
        logPresenceUpdate(frame, freshVoiceCandidate, presenceUpdate)

        val breakUpdate = breakCandidateStateMachine.update(
            BreakCandidateObservation(
                timestampNanos = frame.timestampNanos,
                presenceState = presenceUpdate.state,
                meanAbsoluteYDifference = frame.meanAbsoluteYDifference,
                voiceCandidate = freshVoiceCandidate,
            ),
        )
        val breakUpdateCount = observedBreakUpdates.incrementAndGet()
        latestBreakState.set(breakUpdate.state.name)
        updatePetFaceMood(
            observationTimestampNanos = frame.timestampNanos,
            presenceState = presenceUpdate.state,
            breakState = breakUpdate.state,
        )
        if (breakUpdateCount == 1L || breakUpdate.stateChanged) {
            latestBreakStateChangedAtNanos.set(breakUpdate.timestampNanos)
            val difference = frame.meanAbsoluteYDifference?.let {
                String.format(Locale.US, "%.4f", it)
            } ?: "baseline"
            Log.i(
                BREAK_LOG_TAG,
                "state=${breakUpdate.state.name} " +
                    "previous=${breakUpdate.previousState.name} " +
                    "presence=${presenceUpdate.state.name} " +
                    "activity=${breakUpdate.activityDetected} " +
                    "yDiff=$difference voice=$freshVoiceCandidate",
            )
            appendStructuredLog(
                StructuredLogEntry(
                    elapsedRealtimeNanos = breakUpdate.timestampNanos,
                    wallTimeEpochMillis = System.currentTimeMillis(),
                    eventType = StructuredEventType.BREAK_STATE,
                    visibleFaceCount = frame.visibleFaceCount,
                    meanAbsoluteYDifference = frame.meanAbsoluteYDifference,
                    voiceCandidate = freshVoiceCandidate,
                    presenceState = presenceUpdate.state.name,
                    breakState = breakUpdate.state.name,
                    activityDetected = breakUpdate.activityDetected,
                ),
            )
        }
    }

    private fun updatePetFaceMood(
        observationTimestampNanos: Long,
        presenceState: PresenceState,
        breakState: BreakCandidateState,
    ) {
        val mood = resolvePetFaceMood(
            presenceState = presenceState,
            breakState = breakState,
        )
        val session = arrivalSession.get()
        val observedAtNanos = SystemClock.elapsedRealtimeNanos()
        runOnUiThread {
            if (arrivalMonitoringActive && arrivalSession.get() == session) {
                val result = arrivalMeowCoordinator.onMood(
                    mood = mood,
                    observationTimestampNanos = observationTimestampNanos,
                    playbackTimestampNanos = SystemClock.elapsedRealtimeNanos(),
                )
                val status = when (result) {
                    ArrivalMeowResult.NONE -> null
                    ArrivalMeowResult.SHORT_REST -> "short_rest"
                    ArrivalMeowResult.PLAYED -> "played"
                    ArrivalMeowResult.UNAVAILABLE -> "unavailable"
                    ArrivalMeowResult.BUSY -> "busy"
                }
                if (status != null) {
                    Log.i(MEOW_LOG_TAG, "arrivalMeow=$status")
                }
                if (idleSessionActive) {
                    idlePresence = presenceState == PresenceState.PRESENT
                    idlePresenceObservedAtNanos = observedAtNanos
                }
            }
        }
        val previousMood = latestPetFaceMood.getAndSet(mood)
        if (previousMood == mood) return

        Log.i(
            FACE_LOG_TAG,
            "mood=${mood.name} previous=${previousMood?.name ?: "UNINITIALIZED"} " +
                "presence=${presenceState.name} breakState=${breakState.name}",
        )
        runOnUiThread {
            petFaceMood = mood
        }
    }

    private fun logPresenceUpdate(
        frame: CameraFrameSample,
        freshVoiceCandidate: Boolean,
        update: PresenceUpdate,
    ) {
        val updateCount = observedPresenceUpdates.incrementAndGet()
        if (updateCount == 1L || update.stateChanged) {
            val difference = frame.meanAbsoluteYDifference?.let {
                String.format(Locale.US, "%.4f", it)
            } ?: "baseline"
            Log.i(
                PRESENCE_LOG_TAG,
                "state=${update.state.name} previous=${update.previousState.name} " +
                    "evidence=${update.evidence.name} faces=${frame.visibleFaceCount} " +
                    "yDiff=$difference voice=$freshVoiceCandidate",
            )
            appendStructuredLog(
                StructuredLogEntry(
                    elapsedRealtimeNanos = update.timestampNanos,
                    wallTimeEpochMillis = System.currentTimeMillis(),
                    eventType = StructuredEventType.PRESENCE_STATE,
                    visibleFaceCount = frame.visibleFaceCount,
                    meanAbsoluteYDifference = frame.meanAbsoluteYDifference,
                    voiceCandidate = freshVoiceCandidate,
                    presenceState = update.state.name,
                ),
            )
        }
    }

    private fun canPlayNonStrokeSound(): Boolean =
        !strokePurrCoordinator.isBlockingOtherSounds(SystemClock.elapsedRealtimeNanos()) &&
            !idleVoiceCoordinator.isBlockingOtherSounds(SystemClock.elapsedRealtimeNanos())

    private fun recordFaceInteraction() {
        if (idleSessionActive) idleVoiceCoordinator.onInteraction(SystemClock.elapsedRealtimeNanos())
    }

    private fun handleFaceStroke() {
        if (!arrivalMonitoringActive) return
        recordFaceInteraction()
        val result = strokePurrCoordinator.onStroke(SystemClock.elapsedRealtimeNanos())
        val status = when (result) {
            StrokePurrResult.PLAYED -> "played"
            StrokePurrResult.BUSY -> "busy"
            StrokePurrResult.UNAVAILABLE -> "unavailable"
        }
        Log.i(PURR_LOG_TAG, "strokePurr=$status")
    }

    private fun handleFaceTap() {
        if (!arrivalMonitoringActive) return
        recordFaceInteraction()
        val result = touchPurrCoordinator.onTouch(SystemClock.elapsedRealtimeNanos())
        val status = when (result) {
            TouchPurrResult.PLAYED -> "played"
            TouchPurrResult.COOLDOWN -> "cooldown"
            TouchPurrResult.UNAVAILABLE -> "unavailable"
        }
        Log.i(PURR_LOG_TAG, "touchPurr=$status")
    }

    private fun startAudioAfterPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startAudioLevelCapture()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startAudioLevelCapture() {
        if (audioLevelSource != null) return

        val recognizer = OnDeviceMiKeywordRecognizer(
            context = this,
            onKeywordDetected = keyword@{
                val timestampNanos = SystemClock.elapsedRealtimeNanos()
                if (!arrivalMonitoringActive || idleVoiceCoordinator.isBlockingOtherSounds(timestampNanos)) {
                    return@keyword
                }
                val reaction = keywordReactionCoordinator.onKeywordDetected(timestampNanos)
                if (reaction.visualTriggered) {
                    idleVoiceCoordinator.onInteraction(timestampNanos)
                    Log.i(FACE_LOG_TAG, "keywordReaction=triggered keyword=mi")
                    Log.i(
                        MEOW_LOG_TAG,
                        "keywordMeow=${if (reaction.meowPlayed) "played" else "unavailable"}",
                    )
                    voiceReactionSequence += 1L
                }
            },
            onStatus = { status ->
                Log.i(FACE_LOG_TAG, "keywordRecognition=$status")
            },
            onError = { errorCode ->
                Log.w(FACE_LOG_TAG, "keywordRecognitionError=$errorCode")
            },
        ).also(OnDeviceMiKeywordRecognizer::start)
        miKeywordRecognizer = recognizer

        audioLevelSource = AudioLevelSource(
            onSample = onSample@{ sample ->
                if (!keywordReactionCoordinator.shouldProcessCapturedAudio(sample.timestampNanos)) {
                    return@onSample
                }
                latestAudioSample.set(sample)
                val windowCount = observedAudioWindows.incrementAndGet()
                if (windowCount == 1L || windowCount % LOG_EVERY_N_AUDIO_WINDOWS == 0L) {
                    Log.i(
                        AUDIO_LOG_TAG,
                        "window=$windowCount " +
                            "rms=${String.format(Locale.US, "%.4f", sample.rms)} " +
                            "dbfs=${String.format(Locale.US, "%.1f", sample.dbfs)} " +
                            "peak=${String.format(Locale.US, "%.4f", sample.peak)} " +
                            "vad=${sample.voiceCandidate} samples=${sample.sampleCount}",
                    )
                    appendStructuredLog(
                        StructuredLogEntry(
                            elapsedRealtimeNanos = sample.timestampNanos,
                            wallTimeEpochMillis = System.currentTimeMillis(),
                            eventType = StructuredEventType.AUDIO_SAMPLE,
                            audioRms = sample.rms,
                            audioDbfs = sample.dbfs,
                            audioPeak = sample.peak,
                            voiceCandidate = sample.voiceCandidate,
                        ),
                    )
                }
            },
            onPcmWindow = { pcm, sampleCount ->
                val timestampNanos = SystemClock.elapsedRealtimeNanos()
                if (keywordReactionCoordinator.shouldProcessCapturedAudio(timestampNanos)) {
                    recognizer.acceptPcm(pcm, sampleCount)
                }
                singingProbe?.let { probe ->
                    // A diagnostic failure must not terminate the existing microphone loop.
                    runCatching {
                        probe.acceptPcm(pcm, sampleCount, timestampNanos, powerManager.isInteractive)
                    }.onFailure { error ->
                        probe.setActive(false)
                        Log.e("PetRobotSinging", "probe=failed", error)
                    }
                }
            },
            onError = { error ->
                singingProbe?.setActive(false)
                Log.e(AUDIO_LOG_TAG, "Audio level sensing failed", error)
            },
        ).also(AudioLevelSource::start)
    }

    private fun runSevenDayReplay() {
        if (!replayInProgress.compareAndSet(false, true)) {
            Log.i(REPLAY_LOG_TAG, "alreadyRunning=true")
            return
        }

        val store = structuredLogStore
        if (store == null) {
            replayInProgress.set(false)
            Log.i(REPLAY_LOG_TAG, "unavailable=true")
            return
        }

        val nowEpochMillis = System.currentTimeMillis()
        val window = currentSummaryWindows(
            nowEpochMillis = nowEpochMillis,
            zoneId = ZoneId.systemDefault(),
        ).last()
        val engine = LogReplayEngine(defaultReplayProfiles())
        var latestAudioEpochMillis: Long? = null
        var latestVoiceCandidate = false
        var latestCameraEpochMillis: Long? = null
        var latestVisibleFaceCount = 0
        var storedBreakCandidateCount = 0L

        Log.i(
            REPLAY_LOG_TAG,
            "started period=${window.period.name} " +
                "start=${window.startEpochMillis} end=${window.endInclusiveEpochMillis}",
        )
        store.readReplayEntries(
            window = window,
            onBatch = { entries ->
                entries.forEach { entry ->
                    when (entry.eventType) {
                        StructuredEventType.AUDIO_SAMPLE -> {
                            latestAudioEpochMillis = entry.wallTimeEpochMillis
                            latestVoiceCandidate = entry.voiceCandidate == true
                            val cameraAgeMillis = latestCameraEpochMillis?.let {
                                entry.wallTimeEpochMillis - it
                            }
                            if (
                                cameraAgeMillis != null &&
                                cameraAgeMillis in 0L..CAMERA_EVIDENCE_HOLD_MILLIS
                            ) {
                                engine.accept(
                                    ReplayScalarSample(
                                        timestampNanos =
                                            entry.wallTimeEpochMillis * NANOS_PER_MILLISECOND,
                                        visibleFaceCount = latestVisibleFaceCount,
                                        meanAbsoluteYDifference = null,
                                        voiceCandidate = latestVoiceCandidate,
                                    ),
                                )
                            }
                        }
                        StructuredEventType.CAMERA_SAMPLE -> {
                            latestCameraEpochMillis = entry.wallTimeEpochMillis
                            latestVisibleFaceCount = entry.visibleFaceCount ?: 0
                            val audioAgeMillis = latestAudioEpochMillis?.let {
                                entry.wallTimeEpochMillis - it
                            }
                            val freshVoiceCandidate =
                                audioAgeMillis != null &&
                                    audioAgeMillis in 0L..AUDIO_EVIDENCE_FRESHNESS_MILLIS &&
                                    latestVoiceCandidate
                            engine.accept(
                                ReplayScalarSample(
                                    timestampNanos =
                                        entry.wallTimeEpochMillis * NANOS_PER_MILLISECOND,
                                    visibleFaceCount = latestVisibleFaceCount,
                                    meanAbsoluteYDifference =
                                        entry.meanAbsoluteYDifference,
                                    voiceCandidate = freshVoiceCandidate,
                                ),
                            )
                        }
                        StructuredEventType.BREAK_STATE -> {
                            if (entry.breakState == "CANDIDATE") {
                                storedBreakCandidateCount += 1L
                            }
                        }
                        else -> Unit
                    }
                }
            },
            onComplete = {
                engine.results().forEach { result ->
                    Log.i(
                        REPLAY_LOG_TAG,
                        "profile=${result.profile.name} " +
                            "quietNanos=${result.profile.quietPeriodNanos} " +
                            "samples=${result.sampleCount} " +
                            "sessions=${result.sessionCount} " +
                            "present=${result.presentTransitionCount} " +
                            "breakCandidates=${result.breakCandidateCount}",
                    )
                }
                Log.i(
                    REPLAY_LOG_TAG,
                    "completed storedBreakCandidates=$storedBreakCandidateCount",
                )
                replayInProgress.set(false)
            },
        )
    }

    private fun recordTimingEvaluation(rating: TimingRating) {
        val evaluation = createTimingEvaluationRecord(
            rating = rating,
            pressedAtNanos = SystemClock.elapsedRealtimeNanos(),
            referenceBreakState = latestBreakState.get(),
            breakStateChangedAtNanos = latestBreakStateChangedAtNanos.get(),
        )
        val offset = evaluation.millisSinceBreakStateChange?.toString() ?: "unavailable"
        Log.i(
            TIMING_LOG_TAG,
            "rating=${evaluation.rating.name} " +
                "breakState=${evaluation.referenceBreakState} offsetMs=$offset",
        )
        appendStructuredLog(
            StructuredLogEntry(
                elapsedRealtimeNanos = evaluation.pressedAtNanos,
                wallTimeEpochMillis = System.currentTimeMillis(),
                eventType = StructuredEventType.TIMING_EVALUATION,
                breakState = evaluation.referenceBreakState,
                timingRating = evaluation.rating.name,
                timingOffsetMillis = evaluation.millisSinceBreakStateChange,
            ),
        )
    }

    private companion object {
        const val CAMERA_LOG_TAG = "PetRobotCamera"
        const val AUDIO_LOG_TAG = "PetRobotAudio"
        const val PRESENCE_LOG_TAG = "PetRobotPresence"
        const val BREAK_LOG_TAG = "PetRobotBreak"
        const val THERMAL_LOG_TAG = "PetRobotThermal"
        const val STORAGE_LOG_TAG = "PetRobotStorage"
        const val TIMING_LOG_TAG = "PetRobotTiming"
        const val SUMMARY_LOG_TAG = "PetRobotSummary"
        const val REPLAY_LOG_TAG = "PetRobotReplay"
        const val FACE_LOG_TAG = "PetRobotFace"
        const val MEOW_LOG_TAG = "PetRobotMeow"
        const val PURR_LOG_TAG = "PetRobotPurr"
        const val LOG_EVERY_N_FRAMES = 100L
        const val LOG_EVERY_N_AUDIO_WINDOWS = 10L
        const val LOG_EVERY_N_STORED_EVENTS = 25L
        const val AUDIO_EVIDENCE_FRESHNESS_NANOS = 500_000_000L
        const val AUDIO_EVIDENCE_FRESHNESS_MILLIS = 500L
        const val CAMERA_EVIDENCE_HOLD_MILLIS = 5_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
