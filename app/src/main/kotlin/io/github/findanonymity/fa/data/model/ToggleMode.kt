package io.github.findanonymity.fa.data.model

import androidx.annotation.StringRes
import io.github.findanonymity.fa.R
import kotlinx.serialization.Serializable

@Serializable
enum class ToggleMode(@StringRes val labelRes: Int) {
    CYCLICAL(R.string.mode_cyclical),
    SCHEDULED(R.string.mode_scheduled),
    ALWAYS_ON(R.string.mode_always_on),
    ALWAYS_OFF(R.string.mode_always_off),
    UNMANAGED(R.string.mode_unmanaged),
}
