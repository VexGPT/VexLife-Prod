package vexlife.service

/**
 * Platform-neutral result of converging the long-lived local Product service.
 *
 * Platform owners retain process/lifetime mechanics. Shared setup consumes only the facts needed to
 * prove that the installed Product image and the started service are the same source.
 */
internal interface VexLifeProdServiceLifetime {
    val sourceSha: String
    val imagePayloadSha256: String
    val pid: Long
    val serviceOrigin: String
}

// [VXG RealForever]
