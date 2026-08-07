package io.github.findanonymity.fa.service

import io.github.findanonymity.fa.data.model.RebootRuleConfig
import io.github.findanonymity.fa.data.model.ToggleMode
import io.github.findanonymity.fa.data.model.ToggleRuleConfig

/**
 * Pure, anchor-based phase/countdown math shared by [AutomationService] and the Home UI's live
 * countdown. Never trusts cumulative elapsed delay() time — always recomputes from wall-clock
 * `now` against a fixed anchor, so a single overshoot (Doze slack, process suspension) never
 * compounds across cycles.
 */
object RuleScheduler {

    data class TogglePhase(val shouldBeOn: Boolean, val millisUntilNextTransition: Long)

    fun computeTogglePhase(config: ToggleRuleConfig, now: Long): TogglePhase? {
        if (!config.enabled) return null
        return when (config.mode) {
            ToggleMode.ALWAYS_ON -> TogglePhase(true, Long.MAX_VALUE)
            ToggleMode.ALWAYS_OFF -> TogglePhase(false, Long.MAX_VALUE)
            ToggleMode.UNMANAGED -> null
            ToggleMode.CYCLICAL -> {
                val anchor = config.cycleAnchorEpochMillis ?: now
                val cycleMs = config.cycleInterval.toMillis().coerceAtLeast(1L)
                val activeMs = config.activeDuration.toMillis().coerceIn(0L, cycleMs)
                val elapsed = ((now - anchor) % cycleMs + cycleMs) % cycleMs

                val activePhaseStart = if (config.startOn) 0L else (cycleMs - activeMs)
                val activePhaseEnd = activePhaseStart + activeMs
                val shouldBeOn = elapsed >= activePhaseStart && elapsed < activePhaseEnd

                val nextTransitionAt = when {
                    shouldBeOn -> activePhaseEnd
                    elapsed < activePhaseStart -> activePhaseStart
                    else -> cycleMs + activePhaseStart
                }
                TogglePhase(shouldBeOn, (nextTransitionAt - elapsed).coerceAtLeast(0L))
            }
        }
    }

    fun millisUntilReboot(config: RebootRuleConfig, now: Long): Long {
        val last = config.lastRebootEpochMillis ?: now
        val intervalMs = config.interval.toMillis().coerceAtLeast(1L)
        val elapsed = (now - last).coerceAtLeast(0L)
        return (intervalMs - elapsed).coerceAtLeast(0L)
    }

    fun isRebootDue(config: RebootRuleConfig, now: Long): Boolean =
        config.enabled && millisUntilReboot(config, now) <= 0L
}
