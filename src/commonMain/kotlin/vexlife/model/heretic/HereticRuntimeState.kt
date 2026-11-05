package vexlife.model.heretic

/**
 * Portable runtime state meaning recovered from the current VexLife persistent-runtime owner.
 *
 * These states describe qualified runtime truth. They do not start, stop, observe, or own a process.
 */
enum class HereticRuntimeState(
    val wireValue: String,
) {
    Starting(
        "PERSISTENT_MLX_RUNTIME_STARTING",
    ),
    Qualified(
        "PERSISTENT_MLX_RUNTIME_QUALIFIED",
    ),
}

// [VXG RealForever]
