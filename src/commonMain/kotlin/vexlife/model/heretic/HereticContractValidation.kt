package vexlife.model.heretic

internal fun requirePortableRef(
    value: String,
    label: String,
) {
    require(
        value.matches(
            Regex(
                "[A-Za-z0-9][A-Za-z0-9._:/-]*",
            ),
        ),
    ) {
        "$label must be one portable ref"
    }
}

internal fun requireSha256(
    value: String,
    label: String,
) {
    require(
        value.matches(
            Regex(
                "[0-9a-f]{64}",
            ),
        ),
    ) {
        "$label must be lowercase SHA-256"
    }
}

internal fun requireGitId(
    value: String,
    label: String,
) {
    require(
        value.matches(
            Regex(
                "[0-9a-f]{40,64}",
            ),
        ),
    ) {
        "$label must be one Git identity"
    }
}

// [VXG RealForever]
