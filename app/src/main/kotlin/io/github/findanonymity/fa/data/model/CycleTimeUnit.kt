package io.github.findanonymity.fa.data.model

import androidx.annotation.StringRes
import io.github.findanonymity.fa.R
import kotlinx.serialization.Serializable

@Serializable
enum class CycleTimeUnit(val millisPerUnit: Long, @StringRes val labelRes: Int) {
    SECONDS(1_000L, R.string.duration_unit_seconds),
    MINUTES(60_000L, R.string.duration_unit_minutes),
    HOURS(3_600_000L, R.string.duration_unit_hours),
    DAYS(86_400_000L, R.string.duration_unit_days),
    MONTHS(30L * 86_400_000L, R.string.duration_unit_months),
    YEARS(365L * 86_400_000L, R.string.duration_unit_years),
}

@Serializable
data class Duration2(val value: Long, val unit: CycleTimeUnit) {
    fun toMillis(): Long = value * unit.millisPerUnit

    companion object {
        val ZERO = Duration2(0, CycleTimeUnit.SECONDS)
    }
}
