package com.taki.petrobot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.taki.petrobot.ui.theme.PetRobotTheme

private val FaceAccent = Color(0xFF55D9FF)
private val FacePanel = Color(0xD907131B)

@Composable
fun TimingEvaluationScreen(
    mood: PetFaceMood,
    voiceReactionSequence: Long,
    onFaceTapped: () -> Unit,
    onRatingSelected: (TimingRating) -> Unit,
    onReplayRequested: () -> Unit,
    modifier: Modifier = Modifier,
    onFaceStroked: () -> Unit = {},
    onFaceInteracted: () -> Unit = {},
    gestureSession: Long = 0L,
) {
    var controlsVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PetFace(
            mood = mood,
            voiceReactionSequence = voiceReactionSequence,
            onTapped = onFaceTapped,
            onStroked = onFaceStroked,
            onInteracted = onFaceInteracted,
            gestureSession = gestureSession,
            contentDescription = stringResource(R.string.pet_face_content_description),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.pet_name),
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.pet_face_tap_hint),
                color = FaceAccent.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (controlsVisible) {
            TimingEvaluationPanel(
                onRatingSelected = { rating ->
                    onRatingSelected(rating)
                    controlsVisible = reducePetControlPanelVisibility(
                        isVisible = controlsVisible,
                        event = PetControlPanelEvent.ACTION_COMPLETED,
                    )
                },
                onReplayRequested = {
                    onReplayRequested()
                    controlsVisible = reducePetControlPanelVisibility(
                        isVisible = controlsVisible,
                        event = PetControlPanelEvent.ACTION_COMPLETED,
                    )
                },
                onClose = {
                    controlsVisible = reducePetControlPanelVisibility(
                        isVisible = controlsVisible,
                        event = PetControlPanelEvent.CLOSE,
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            )
        } else {
            OutlinedButton(
                onClick = {
                    controlsVisible = reducePetControlPanelVisibility(
                        isVisible = controlsVisible,
                        event = PetControlPanelEvent.OPEN,
                    )
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FaceAccent),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
            ) {
                Text(text = stringResource(R.string.timing_evaluation_open))
            }
        }
    }
}

@Composable
private fun TimingEvaluationPanel(
    onRatingSelected: (TimingRating) -> Unit,
    onReplayRequested: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = FacePanel,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.timing_evaluation_title),
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.timing_evaluation_description),
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.Center) {
                TimingButton(
                    text = stringResource(R.string.timing_too_early),
                    onClick = { onRatingSelected(TimingRating.TOO_EARLY) },
                )
                Spacer(modifier = Modifier.width(12.dp))
                TimingButton(
                    text = stringResource(R.string.timing_just_right),
                    onClick = { onRatingSelected(TimingRating.JUST_RIGHT) },
                )
                Spacer(modifier = Modifier.width(12.dp))
                TimingButton(
                    text = stringResource(R.string.timing_too_late),
                    onClick = { onRatingSelected(TimingRating.TOO_LATE) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp),
            ) {
                OutlinedButton(
                    onClick = onReplayRequested,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FaceAccent),
                ) {
                    Text(text = stringResource(R.string.replay_seven_days))
                }
                TextButton(
                    onClick = onClose,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.72f)),
                ) {
                    Text(text = stringResource(R.string.timing_evaluation_close))
                }
            }
        }
    }
}

@Composable
private fun TimingButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF12374A),
            contentColor = FaceAccent,
        ),
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 900, heightDp = 600)
@Composable
private fun TimingEvaluationScreenPreview() {
    PetRobotTheme {
        TimingEvaluationScreen(
            mood = PetFaceMood.ATTENTIVE,
            voiceReactionSequence = 0L,
            onFaceTapped = {},
            onRatingSelected = {},
            onReplayRequested = {},
        )
    }
}
