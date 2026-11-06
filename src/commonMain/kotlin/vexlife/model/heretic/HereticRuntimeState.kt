package vexlife.model.heretic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Portable runtime state meaning recovered from the current VexLife persistent-runtime owner.
 *
 * These states describe qualified runtime truth. They do not start, stop, observe, or own a process.
 */
@Serializable
enum class HereticRuntimeState(
    val wireValue: String,
) {
    @SerialName("PERSISTENT_MLX_RUNTIME_STARTING")
    Starting(
        "PERSISTENT_MLX_RUNTIME_STARTING",
    ),
    @SerialName("PERSISTENT_MLX_RUNTIME_QUALIFIED")
    Qualified(
        "PERSISTENT_MLX_RUNTIME_QUALIFIED",
    ),
}

// [VXG RealForever]
