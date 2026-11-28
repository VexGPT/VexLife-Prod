package vexlife.capability

/**
 * Atlas definition for the first effect-capable C3 relationship.
 *
 * Registration describes the request shape only. W1 deliberately does not add this definition to
 * the ordinary Direct Companion offer: current human grant/resource/Bridge admission arrives in W2.
 */
object VexLifeWorkspaceEffectDefinitions {
    const val WriteRef = "capability.vexlife.workspace.write"
    const val Handle = "workspace"
    const val PermissionRef = "permission.vexlife-prod.workspace.write"
    const val ResourceRef = "resource.vexlife-prod.workspace.current-effect-grant"

    val Definitions: List<VexLifeCapabilityAtlas.Definition> =
        listOf(
            VexLifeCapabilityAtlas.Definition(
                capabilityRef = WriteRef,
                handle = Handle,
                purpose =
                    "Create one UTF-8 text file or replace one exact expected version inside a person-granted workspace. The host owns authority, recovery and verification.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = listOf(VexLifeCapabilityAtlas.CompanionRoleRef),
                platformRefs = listOf(VexLifeCapabilityAtlas.MacPlatformRef),
                permissionRef = PermissionRef,
                resourceRef = ResourceRef,
                effectClass = "RECOVERY_GUARDED_LOCAL_WRITE",
                resourceClass = "PERSON_GRANTED_WORKSPACE",
                modelParticipationClass = "REQUEST_RETURN_EFFECT",
                sourcePath =
                    "src/jvmMain/kotlin/vexlife/capability/VexLifeWorkspaceWrite.kt",
                priority = 46,
                actions =
                    listOf(
                        VexLifeCapabilityAtlas.Action(
                            action = "write",
                            purpose =
                                "Create a text file when absent, or replace one exact current version by supplying its SHA-256.",
                            arguments =
                                listOf(
                                    VexLifeCapabilityAtlas.Argument(
                                        name = "source",
                                        schema = VexLifeCapabilityAtlas.ValueSchema("string"),
                                        required = true,
                                        meaning =
                                            "Exact Local Source ref whose current generation has a separate workspace-effect grant.",
                                    ),
                                    VexLifeCapabilityAtlas.Argument(
                                        name = "path",
                                        schema = VexLifeCapabilityAtlas.ValueSchema("string"),
                                        required = true,
                                        meaning =
                                            "Relative UTF-8 text file path inside the granted workspace. Parent directory must already exist.",
                                    ),
                                    VexLifeCapabilityAtlas.Argument(
                                        name = "content",
                                        schema = VexLifeCapabilityAtlas.ValueSchema("string"),
                                        required = true,
                                        meaning =
                                            "Complete desired UTF-8 file content, bounded by the workspace write owner.",
                                    ),
                                    VexLifeCapabilityAtlas.Argument(
                                        name = "expectedSha256",
                                        schema = VexLifeCapabilityAtlas.ValueSchema("string"),
                                        required = false,
                                        meaning =
                                            "Exact lowercase SHA-256 of the current file for replacement. Omit only for create-if-absent.",
                                    ),
                                ),
                            exampleArguments =
                                """{"source":"source.vexlife-prod.folder.sha256.<sha256>","path":"app.js","content":"export const answer = 42;\\n","expectedSha256":"<sha256>"}""",
                            returns =
                                "Verified create/replace/recovery receipt with source/path/prior/new digests and effect standing; never the private before-image body.",
                        )
                    ),
            )
        )
}

// [VXG RealForever]
