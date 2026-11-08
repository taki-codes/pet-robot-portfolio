package com.taki.petrobot

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun PetFace(
    mood: PetFaceMood,
    voiceReactionSequence: Long,
    onTapped: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onStroked: () -> Unit = {},
    onInteracted: () -> Unit = {},
    gestureSession: Long = 0L,
) {
    val currentOnTapped by rememberUpdatedState(onTapped)
    val currentOnStroked by rememberUpdatedState(onStroked)
    val currentOnInteracted by rememberUpdatedState(onInteracted)
    val gazeHorizontal = remember { Animatable(0f) }
    val gazeVertical = remember { Animatable(0f) }
    val eyelidScale = remember { Animatable(1f) }
    val touchProgress = remember { Animatable(0f) }
    val voiceProgress = remember { Animatable(0f) }
    val arrivalProgress = remember { Animatable(0f) }
    var tappedTarget by remember { mutableStateOf<GazeTarget?>(null) }
    var touchSequence by remember { mutableStateOf(0L) }
    var previousMood by remember { mutableStateOf(mood) }
    var arrivalSequence by remember { mutableStateOf(0L) }

    val eyeColor by animateColorAsState(
        targetValue = mood.eyeColor(),
        animationSpec = tween(durationMillis = 450),
        label = "petFaceEyeColor",
    )
    val moodEyeScale by animateFloatAsState(
        targetValue = mood.eyeScale(),
        animationSpec = tween(durationMillis = 450),
        label = "petFaceEyeScale",
    )

    LaunchedEffect(mood) {
        val blinkPauses = mood.blinkPauses()
        var pauseIndex = 0
        while (true) {
            delay(blinkPauses[pauseIndex % blinkPauses.size])
            eyelidScale.animateTo(0.04f, tween(durationMillis = 85))
            eyelidScale.animateTo(1f, tween(durationMillis = 135))
            pauseIndex += 1
        }
    }

    LaunchedEffect(mood) {
        if (shouldTriggerArrivalReaction(previousMood = previousMood, currentMood = mood)) {
            arrivalSequence += 1L
        }
        previousMood = mood
    }

    LaunchedEffect(touchSequence) {
        if (touchSequence == 0L) return@LaunchedEffect
        touchProgress.snapTo(0f)
        touchProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = PET_FACE_TOUCH_DURATION_MILLIS.toInt()),
        )
        touchProgress.snapTo(0f)
    }

    LaunchedEffect(arrivalSequence) {
        if (arrivalSequence == 0L) return@LaunchedEffect
        arrivalProgress.snapTo(0f)
        arrivalProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = PET_FACE_ARRIVAL_DURATION_MILLIS.toInt()),
        )
        arrivalProgress.snapTo(0f)
    }

    LaunchedEffect(voiceReactionSequence) {
        if (voiceReactionSequence == 0L) return@LaunchedEffect
        voiceProgress.snapTo(0f)
        voiceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = PET_FACE_VOICE_REACTION_DURATION_MILLIS.toInt()),
        )
        voiceProgress.snapTo(0f)
    }

    LaunchedEffect(tappedTarget, touchSequence, mood) {
        val tap = tappedTarget
        if (tap != null) {
            coroutineScope {
                launch {
                    gazeHorizontal.animateTo(tap.horizontal, tween(durationMillis = 180))
                }
                launch {
                    gazeVertical.animateTo(tap.vertical, tween(durationMillis = 180))
                }
            }
            delay(650)
            tappedTarget = null
        } else {
            val wanderingTargets = listOf(
                GazeTarget(-0.45f, -0.12f),
                GazeTarget(0.18f, 0.08f),
                GazeTarget(0.52f, -0.18f),
                GazeTarget(0f, 0f),
            )
            var targetIndex = 0
            while (true) {
                delay(mood.wanderDelayMillis())
                val target = wanderingTargets[targetIndex % wanderingTargets.size]
                coroutineScope {
                    launch {
                        gazeHorizontal.animateTo(
                            target.horizontal,
                            tween(durationMillis = 650),
                        )
                    }
                    launch {
                        gazeVertical.animateTo(
                            target.vertical,
                            tween(durationMillis = 650),
                        )
                    }
                }
                targetIndex += 1
            }
        }
    }

    Canvas(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(gestureSession) {
                detectTapGestures { tap ->
                    currentOnTapped()
                    tappedTarget = normalizedGazeTarget(
                        tapX = tap.x,
                        tapY = tap.y,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                    )
                    touchSequence += 1L
                }
            }
            .pointerInput(gestureSession) {
                var detector: StrokeGestureDetector? = null
                var pointerId: PointerId? = null
                detectDragGestures(
                    onDragStart = {
                        detector = StrokeGestureDetector()
                        pointerId = null
                    },
                    onDragEnd = { detector = null },
                    onDragCancel = { detector = null },
                    onDrag = { change, _ ->
                        change.consume()
                        if (pointerId == null) pointerId = change.id
                        if (pointerId != change.id ||
                            change.position.x !in 0f..size.width.toFloat() ||
                            change.position.y !in 0f..size.height.toFloat()
                        ) {
                            detector = null
                        }
                        if (detector != null) currentOnInteracted()
                        if (detector?.sample(
                                xDp = change.position.x / density,
                                yDp = change.position.y / density,
                                timestampMillis = change.uptimeMillis,
                            ) == true
                        ) {
                            currentOnStroked()
                        }
                    },
                )
            },
    ) {
        drawRect(Color.Black)

        val shortSide = min(size.width, size.height)
        val touchVisual = petFaceTouchVisual(touchProgress.value)
        val voiceVisual = petFaceVoiceReactionVisual(voiceProgress.value)
        val arrivalVisual = petFaceArrivalVisual(arrivalProgress.value)
        val scale = touchVisual.eyeScaleMultiplier *
            voiceVisual.eyeScaleMultiplier *
            arrivalVisual.eyeScaleMultiplier *
            moodEyeScale
        val cheekAlpha = maxOf(touchVisual.cheekAlpha, arrivalVisual.cheekAlpha)
        val eyeLayerAlpha = petFaceEyeLayerAlpha(arrivalVisual.smileAlpha)
        val regularEyeAlpha = eyeLayerAlpha.regularEyeAlpha
        val glowAlpha = (
            0.20f +
                voiceVisual.glowAlphaBoost +
                arrivalVisual.glowAlphaBoost
        ).coerceIn(0f, 1f)
        val eyeWidth = shortSide * 0.13f * scale
        val fullEyeHeight = shortSide * 0.31f * scale
        val eyeHeight = (fullEyeHeight * eyelidScale.value).coerceAtLeast(shortSide * 0.012f)
        val eyeSpacing = shortSide * 0.19f
        val gazeX = gazeHorizontal.value * shortSide * 0.055f
        val gazeY = gazeVertical.value * shortSide * 0.045f
        val centerY = size.height * 0.46f + gazeY
        val glowPadding = shortSide * 0.035f
        val cornerRadius = eyeWidth

        val eyeCenters = listOf(
            size.width / 2f - eyeSpacing + gazeX,
            size.width / 2f + eyeSpacing + gazeX,
        )

        if (voiceVisual.attentionMarkAlpha > 0f) {
            val rightEyeCenter = eyeCenters.last()
            val markBaseX = rightEyeCenter + eyeWidth * 0.82f
            val markBaseY = centerY - fullEyeHeight * 0.48f
            val markStrokeWidth = shortSide * 0.018f
            val markColor = Color(0xFFFFD400).copy(
                alpha = voiceVisual.attentionMarkAlpha,
            )
            val rays = listOf(
                Offset(
                    x = markBaseX,
                    y = markBaseY - shortSide * 0.025f,
                ) to Offset(
                    x = markBaseX + shortSide * 0.018f,
                    y = markBaseY - shortSide * 0.115f,
                ),
                Offset(
                    x = markBaseX + shortSide * 0.042f,
                    y = markBaseY,
                ) to Offset(
                    x = markBaseX + shortSide * 0.105f,
                    y = markBaseY - shortSide * 0.070f,
                ),
                Offset(
                    x = markBaseX + shortSide * 0.060f,
                    y = markBaseY + shortSide * 0.045f,
                ) to Offset(
                    x = markBaseX + shortSide * 0.150f,
                    y = markBaseY + shortSide * 0.020f,
                ),
            )
            rays.forEach { (start, end) ->
                drawLine(
                    color = markColor,
                    start = start,
                    end = end,
                    strokeWidth = markStrokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }

        eyeCenters.forEach { centerX ->
            val topLeft = Offset(
                x = centerX - eyeWidth / 2f,
                y = centerY - eyeHeight / 2f,
            )
            drawRoundRect(
                color = eyeColor.copy(alpha = glowAlpha * regularEyeAlpha),
                topLeft = Offset(
                    x = topLeft.x - glowPadding,
                    y = topLeft.y - glowPadding,
                ),
                size = Size(
                    width = eyeWidth + glowPadding * 2f,
                    height = eyeHeight + glowPadding * 2f,
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    x = cornerRadius,
                    y = cornerRadius,
                ),
            )
            drawRoundRect(
                color = eyeColor.copy(alpha = regularEyeAlpha),
                topLeft = topLeft,
                size = Size(width = eyeWidth, height = eyeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    x = cornerRadius,
                    y = cornerRadius,
                ),
            )
            if (eyelidScale.value > 0.35f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.72f * regularEyeAlpha),
                    radius = shortSide * 0.014f * scale,
                    center = Offset(
                        x = centerX - eyeWidth * 0.16f,
                        y = centerY - eyeHeight * 0.27f,
                    ),
                )
            }
        }

        if (eyeLayerAlpha.smileEyeAlpha > 0f) {
            val smileWidth = shortSide * 0.16f * scale
            val smileHeight = shortSide * 0.105f * scale
            val smileStrokeWidth = shortSide * 0.018f * scale
            val smileTop = centerY - smileHeight * 0.62f
            eyeCenters.forEach { centerX ->
                val topLeft = Offset(
                    x = centerX - smileWidth / 2f,
                    y = smileTop,
                )
                drawArc(
                    color = eyeColor.copy(alpha = eyeLayerAlpha.smileEyeAlpha * 0.24f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(
                        x = topLeft.x - smileStrokeWidth * 0.55f,
                        y = topLeft.y - smileStrokeWidth * 0.55f,
                    ),
                    size = Size(
                        width = smileWidth + smileStrokeWidth * 1.1f,
                        height = smileHeight + smileStrokeWidth * 1.1f,
                    ),
                    style = Stroke(
                        width = smileStrokeWidth * 1.8f,
                        cap = StrokeCap.Round,
                    ),
                )
                drawArc(
                    color = eyeColor.copy(alpha = eyeLayerAlpha.smileEyeAlpha),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(width = smileWidth, height = smileHeight),
                    style = Stroke(
                        width = smileStrokeWidth,
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }

        if (cheekAlpha > 0f) {
            val cheekY = centerY + fullEyeHeight * 0.62f
            val cheekRadius = shortSide * 0.042f * (0.9f + cheekAlpha * 0.1f)
            eyeCenters.forEach { centerX ->
                drawCircle(
                    color = Color(0xFFFF78B7).copy(alpha = cheekAlpha * 0.72f),
                    radius = cheekRadius,
                    center = Offset(
                        x = centerX,
                        y = cheekY,
                    ),
                )
            }
        }
    }
}

private fun PetFaceMood.eyeColor(): Color = when (this) {
    PetFaceMood.RESTING -> Color(0xFF2B75A8)
    PetFaceMood.CURIOUS -> Color(0xFFA76CFF)
    PetFaceMood.ATTENTIVE -> Color(0xFF55D9FF)
    PetFaceMood.ENGAGED -> Color(0xFF58E69A)
    PetFaceMood.BREAK_READY -> Color(0xFFFFC857)
}

private fun PetFaceMood.eyeScale(): Float = when (this) {
    PetFaceMood.RESTING -> 0.72f
    PetFaceMood.CURIOUS -> 1.02f
    PetFaceMood.ATTENTIVE -> 1f
    PetFaceMood.ENGAGED -> 1.08f
    PetFaceMood.BREAK_READY -> 0.92f
}

private fun PetFaceMood.blinkPauses(): LongArray = when (this) {
    PetFaceMood.RESTING -> longArrayOf(1_800L, 2_200L, 3_000L)
    PetFaceMood.CURIOUS -> longArrayOf(3_500L, 4_300L)
    PetFaceMood.ATTENTIVE -> longArrayOf(2_800L, 4_100L, 3_300L, 5_000L)
    PetFaceMood.ENGAGED -> longArrayOf(4_400L, 5_200L)
    PetFaceMood.BREAK_READY -> longArrayOf(2_100L, 2_600L)
}

private fun PetFaceMood.wanderDelayMillis(): Long = when (this) {
    PetFaceMood.RESTING -> 3_200L
    PetFaceMood.CURIOUS -> 1_500L
    PetFaceMood.ATTENTIVE -> 1_900L
    PetFaceMood.ENGAGED -> 1_300L
    PetFaceMood.BREAK_READY -> 2_500L
}
