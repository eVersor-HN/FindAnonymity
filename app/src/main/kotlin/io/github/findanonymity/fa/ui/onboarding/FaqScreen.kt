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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.CorpoCyan
import io.github.findanonymity.fa.ui.theme.CorpoYellow

private data class FaqEntry(val questionRes: Int, val answerRes: Int)

private data class TutorialStep(val iconRes: Int, val titleRes: Int, val bodyRes: Int)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(R.drawable.ic_shield, R.string.faq_step1_title, R.string.faq_step1_body),
    TutorialStep(R.drawable.ic_play, R.string.faq_step2_title, R.string.faq_step2_body),
    TutorialStep(R.drawable.ic_link, R.string.faq_step3_title, R.string.faq_step3_body),
    TutorialStep(R.drawable.ic_clock, R.string.faq_step4_title, R.string.faq_step4_body),
    TutorialStep(R.drawable.ic_power, R.string.faq_step5_title, R.string.faq_step5_body),
    TutorialStep(R.drawable.ic_warn, R.string.faq_step6_title, R.string.faq_step6_body),
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
                // Compact getting-started checklist: all six steps fit one portrait screen; the
                // FAQ starts below the fold.
                item {
                    TerminalCard(modifier = Modifier.fillMaxWidth(), accent = CorpoCyan) {
                        Text(
                            stringResource(R.string.faq_tutorial_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = CorpoCyan,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        TUTORIAL_STEPS.forEach { step ->
                            TutorialRow(painterResource(step.iconRes), step.titleRes, step.bodyRes)
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.faq_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = CorpoYellow,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(FAQ_ENTRIES) { entry ->
                    TerminalCard(modifier = Modifier.fillMaxWidth(), strip = false) {
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
private fun TutorialRow(icon: Painter, titleRes: Int, bodyRes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(CorpoCyan.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = CorpoCyan, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.labelLarge, color = CorpoYellow)
            Text(
                stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
