package io.github.findanonymity.fa.service

import io.github.findanonymity.fa.data.model.RebootRuleConfig
import io.github.findanonymity.fa.data.model.ToggleMode
import io.github.findanonymity.fa.data.model.ToggleRuleConfig
import java.time.Instant
import java.time.ZoneId

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
            ToggleMode.SCHEDULED -> computeScheduledPhase(config, now)
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

    /**
     * SCHEDULED mode: a daily clock-time window ([scheduleStartMinute], [scheduleEndMinute]) on the
     * weekdays in [scheduleDaysMask] (bit0=Mon…bit6=Sun), evaluated in the device's local time.
     * Inside the window on a selected day the target is [scheduleOnDuringWindow]; outside it is the
     * opposite. State only ever flips at a window edge on a selected day, so the next transition is
     * the earliest such edge after [now].
     */
    fun computeScheduledPhase(config: ToggleRuleConfig, now: Long): TogglePhase {
        val zone = ZoneId.systemDefault()
        val startMin = config.scheduleStartMinute.coerceIn(0, 1440)
        val endMin = config.scheduleEndMinute.coerceIn(0, 1440)
        val days = config.scheduleDaysMask
        val validWindow = startMin < endMin && days != 0

        val nowZdt = Instant.ofEpochMilli(now).atZone(zone)

        fun inWindowAt(epochMs: Long): Boolean {
            if (!validWindow) return false
            val zdt = Instant.ofEpochMilli(epochMs).atZone(zone)
            val dayBit = 1 shl (zdt.dayOfWeek.value - 1) // Monday.value == 1 -> bit0
            if (days and dayBit == 0) return false
            val minute = zdt.hour * 60 + zdt.minute
            return minute in startMin until endMin
        }

        val onNow = if (config.scheduleOnDuringWindow) inWindowAt(now) else !inWindowAt(now)

        var nextEdge = Long.MAX_VALUE
        if (validWindow) {
            val today = nowZdt.toLocalDate()
            for (d in 0..7) {
                val date = today.plusDays(d.toLong())
                val dayBit = 1 shl (date.dayOfWeek.value - 1)
                if (days and dayBit == 0) continue
                val midnight = date.atStartOfDay(zone)
                for (edgeMin in intArrayOf(startMin, endMin)) {
                    val edgeMs = midnight.plusMinutes(edgeMin.toLong()).toInstant().toEpochMilli()
                    if (edgeMs > now) {
                        nextEdge = edgeMs
                        break
                    }
                }
                if (nextEdge != Long.MAX_VALUE) break
            }
        }
        val millisUntil = if (nextEdge == Long.MAX_VALUE) Long.MAX_VALUE else (nextEdge - now)
        return TogglePhase(onNow, millisUntil)
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
