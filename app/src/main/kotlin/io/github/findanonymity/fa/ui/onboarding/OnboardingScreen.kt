package io.github.findanonymity.fa.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import io.github.findanonymity.fa.ui.components.CorpoButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.ui.theme.CorpoYellow
import io.github.findanonymity.fa.ui.theme.CorpoSteel

private data class OnboardingStep(val titleRes: Int, val bodyRes: Int)

private val ONBOARDING_STEPS = listOf(
    OnboardingStep(R.string.onboarding_step1_title, R.string.onboarding_step1_body),
    OnboardingStep(R.string.onboarding_step2_title, R.string.onboarding_step2_body),
    OnboardingStep(R.string.onboarding_step3_title, R.string.onboarding_step3_body),
    OnboardingStep(R.string.onboarding_step4_title, R.string.onboarding_step4_body),
    OnboardingStep(R.string.onboarding_step5_title, R.string.onboarding_step5_body),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = ONBOARDING_STEPS[stepIndex]
    val isLast = stepIndex == ONBOARDING_STEPS.lastIndex

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ONBOARDING_STEPS.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(if (index <= stepIndex) CorpoYellow else CorpoSteel),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(stringResource(step.titleRes), style = MaterialTheme.typography.titleLarge, color = CorpoYellow)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(step.bodyRes), style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onDone) {
                    Text(stringResource(R.string.onboarding_skip))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (stepIndex > 0) {
                        TextButton(onClick = { stepIndex-- }) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }
                    CorpoButton(
                        text = stringResource(if (isLast) R.string.onboarding_done else R.string.onboarding_next),
                        onClick = { if (isLast) onDone() else stepIndex++ },
                    )
                }
            }
        }
    }
}
