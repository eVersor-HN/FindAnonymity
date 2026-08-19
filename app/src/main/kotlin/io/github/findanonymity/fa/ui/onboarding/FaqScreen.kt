package io.github.findanonymity.fa.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.CorpoCyan
import io.github.findanonymity.fa.ui.theme.CorpoYellow

private data class FaqEntry(val questionRes: Int, val answerRes: Int)

private data class TutorialStep(val icon: ImageVector, val titleRes: Int, val bodyRes: Int)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(Icons.Filled.Security, R.string.faq_step1_title, R.string.faq_step1_body),
    TutorialStep(Icons.Filled.PlayArrow, R.string.faq_step2_title, R.string.faq_step2_body),
    TutorialStep(Icons.Filled.Link, R.string.faq_step3_title, R.string.faq_step3_body),
    TutorialStep(Icons.Filled.Schedule, R.string.faq_step4_title, R.string.faq_step4_body),
    TutorialStep(Icons.Filled.PowerSettingsNew, R.string.faq_step5_title, R.string.faq_step5_body),
    TutorialStep(Icons.Filled.Warning, R.string.faq_step6_title, R.string.faq_step6_body),
)

private val FAQ_ENTRIES = listOf(
    FaqEntry(R.string.faq_q1, R.string.faq_a1),
    FaqEntry(R.string.faq_q2, R.string.faq_a2),
    FaqEntry(R.string.faq_q3, R.string.faq_a3),
    FaqEntry(R.string.faq_q4, R.string.faq_a4),
    FaqEntry(R.string.faq_q5, R.string.faq_a5),
    FaqEntry(R.string.faq_q6, R.string.faq_a6),
    FaqEntry(R.string.faq_q7, R.string.faq_a7),
    FaqEntry(R.string.faq_q8, R.string.faq_a8),
    FaqEntry(R.string.faq_q9, R.string.faq_a9),
    FaqEntry(R.string.faq_q10, R.string.faq_a10),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.faq_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.faq_tutorial_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = CorpoCyan,
                    )
                }
                items(TUTORIAL_STEPS) { step -> TutorialCard(step) }

                item {
                    Text(
                        stringResource(R.string.faq_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = CorpoYellow,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(FAQ_ENTRIES) { entry ->
                    TerminalCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(entry.questionRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = CorpoYellow,
                        )
                        Text(
                            stringResource(entry.answerRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialCard(step: TutorialStep) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CorpoCyan.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(step.icon, contentDescription = null, tint = CorpoCyan)
            }
            Text(
                stringResource(step.titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = CorpoYellow,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Text(
            stringResource(step.bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
