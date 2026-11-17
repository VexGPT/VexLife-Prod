package vexlife.model.heretic

/**
 * Platform/runtime realization admitted for one current Product model choice.
 *
 * The model catalog remains the human/Product choice owner. A realization answers only how that
 * chosen model is represented and served on one platform.
 *
 * Existing catalog identities continue to represent the current Mac/MLX realization exactly as
 * before. The temporary Windows patient-zero adds one extra realization for the already-offered
 * heretic-4b choice; it does not add another selectable model.
 */
object HereticRuntimeRealization {
    const val Windows4BPatientZeroReceiptRelativePath =
        "runtime/vexlife-prod/windows-4b-patient-zero-runtime-receipt.json"

    data class Value(
        val realizationRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val endpoint: String,
        val requestModel: String,
        val platform: String,
        val architecture: String,
    )

    val Windows4BPatientZero =
        Value(
            realizationRef =
                "realization.vexlife-prod.heretic-4b.windows-x64.llama-cpp.patient-zero.001",
            modelRef =
                HereticModelCatalog.Candidate4B.modelRef,
            modelProfileRef =
                HereticModelCatalog.Candidate4B.identity.modelProfileRef,
            artifactRef =
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-gguf-q4-k-m.c0807aa5",
            artifactContentSetSha256 =
                "7b95847ce1684f98d96f13bad2daaeffcfd2f95ea2ff3821ee68943590a10159",
            runtimeAdapterRef =
                "adapter.runtime.llama-cpp.windows-x64.cuda.patient-zero.001",
            runtimeClass =
                "LLAMA_CPP_OPENAI_RUNTIME",
            endpoint =
                "http://127.0.0.1:18080",
            requestModel =
                "default_model",
            platform =
                "windows",
            architecture =
                "x86_64",
        )

    fun admits(
        model: HereticModelCatalog.Entry,
        artifactRef: String,
        artifactContentSetSha256: String,
        runtimeAdapterRef: String,
        runtimeClass: String,
        endpoint: String,
        requestModel: String,
    ): Boolean {
        val identity =
            model.identity

        val currentMac =
            artifactRef == identity.artifactSetRef &&
                artifactContentSetSha256 == identity.artifactContentSetSha256 &&
                runtimeAdapterRef == identity.runtimeAdapterRef &&
                runtimeClass == identity.runtimeClass &&
                endpoint == identity.endpoint &&
                requestModel == identity.requestModel

        if (currentMac) {
            return true
        }

        val windows =
            Windows4BPatientZero

        return model.modelRef == windows.modelRef &&
            model.identity.modelProfileRef == windows.modelProfileRef &&
            artifactRef == windows.artifactRef &&
            artifactContentSetSha256 == windows.artifactContentSetSha256 &&
            runtimeAdapterRef == windows.runtimeAdapterRef &&
            runtimeClass == windows.runtimeClass &&
            endpoint == windows.endpoint &&
            requestModel == windows.requestModel
    }
}

// [VXG RealForever]
