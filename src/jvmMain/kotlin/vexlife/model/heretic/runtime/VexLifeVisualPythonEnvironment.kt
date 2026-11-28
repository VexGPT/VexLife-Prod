package vexlife.model.heretic.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.filesystem.VexOwnedPath
import vexlife.model.VexLifeVisualInputQualification
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState
import vexlife.platform.python.ManagedPython

/**
 * Exact Home-owned Python environment for attached-image interpretation.
 *
 * This is deliberately separate from the qualified mlx-lm text environment. It owns only exact
 * visual package materialization/currentness and the join into current visual-input evidence.
 * It owns no model selection, image interpretation policy, Conversation, or persistent model
 * process.
 */
object VexLifeVisualPythonEnvironment {
    const val InstallReportFilename = "vexlife-mlx-vlm-install-report.json"
    const val RequirementsFilename = "vexlife-mlx-vlm-a005.lock"
    const val RequirementsSha256 =
        "f0e2d30fa485180b80516f3bd2b17b85071f8a925d4a2c2becb75729a9bd03a6"

    val EnvironmentDirectoryName =
        "python-" +
            VexLifeVisualInputQualification.PythonVersion +
            "-mlx-vlm-" +
            VexLifeVisualInputQualification.MlxVlmVersion +
            "-a005"

    private val RequirementsText =
        listOf(
            "mlx-vlm==0.8.0 --hash=sha256:712bd3f19d5c2e1c7f44938401c2110e4912a0a4ac596cae38389893fdfa04d9",
            "cryptography==50.0.2 --hash=sha256:fa8f5efb344d6908a1ce62f4a24e2e5780f825d6f53f5f50ec5ffacac72936cb",
            "cffi==2.1.1 --hash=sha256:661c298b4821edebead0c91edd2b00374d67ad7c5a1f7a91d4442633b79d6a72",
            "datasets==5.2.0 --hash=sha256:4be9e2882caaf5db13eadc39e93f3da2a995187b99acebac1595bf6c58dfa927",
            "dill==0.4.1 --hash=sha256:1e1ce33e978ae97fcfcff5638477032b801c46c7c65cf717f95fbc2248f79a9d",
            "fsspec==2026.9.0 --hash=sha256:8dd6e646e99ea382bd85f97a45e6b526a442d79423a7dc673f1e2756d05fcb5f",
            "huggingface_hub==2.4.0 --hash=sha256:42ecfd93ede47bdd333daebbf66a32d7bfe2e18752754fe4d2d4f98a0179d801",
            "click==8.5.1 --hash=sha256:8bb5376d2006950386f8c6427613334a2acadc5f559cb80e638d00b4df957e37",
            "hf-xet==1.8.0 --hash=sha256:3e8934784425781f0f779b91044df441f3a7af4dab47375d7e3aeaecbb8eecb4",
            "httpx2==2.16.2 --hash=sha256:f05c13aade4c45dbf1c9060647a1058f0acad62398fad96e741269edb33d46f5",
            "httpcore2==2.16.2 --hash=sha256:16051ae2a42f776007543f32de9349a317b23d2cbb4e1456b4bac0e88b87419a",
            "multiprocess==0.70.19 --hash=sha256:e8cc7fbdff15c0613f0a1f1f8744bef961b0a164c0ca29bdff53e9d2d93c5e5f",
            "anyio==4.16.0 --hash=sha256:c66ae634e0b8228396b801741beb3294326d4b6102f7bba4041c0dfb58c29026",
            "fastapi==0.165.0 --hash=sha256:896f48462042bb4f162147eb8fac7e858753dc1f486765709baf105d70e4fc5b",
            "annotated-doc==0.0.7 --hash=sha256:6bc31bbbec6dfaaedc1676dbc11dbda96fe22a2b5c400e9ebad113fec44a9dc6",
            "filelock==4.2.12 --hash=sha256:21decfec02f116ed4c954433cdb31f28c3b346b784543177b6d7059da2702ee9",
            "h11==0.16.0 --hash=sha256:63cf8bbe7522de3bf65932fda1d9c2772064ffb3dae62d55932da54b31cb6c86",
            "httpx==0.28.1 --hash=sha256:d909fcccc110f8c7faf814ca82a9a4d816bc5a6dbfea25d6591d6985b8ba59ad",
            "httpcore==1.0.9 --hash=sha256:2d400746a40668fc9dec9810239072b40b4484b640a8c38fd654a024c7a1bf55",
            "idna==3.20 --hash=sha256:ab7ae7122974553370f0bdb919e1a960b2cd1bc1ef0276416d896db81c14582c",
            "Jinja2==3.1.6 --hash=sha256:85ece4451f492d0c13c5dd7c13a64681a86afae63a5f347908daf103ce6d2f67",
            "llguidance==1.9.1 --hash=sha256:a942487cd6221b793df3a11450881a6c9d25a45460a900c56d99b682633c45fe",
            "MarkupSafe==3.0.4 --hash=sha256:52704c5d36eb6dda8866493decd61111fff86244c9b1ad225ca01b9e91e5970f",
            "miniaudio==1.73 --hash=sha256:2a53a82faf51b499db439a8916237b0ad21797395c86b1be73db4006634cd121",
            "mlx==0.32.4 --hash=sha256:1dd56491b44a2bfa217f60da66a5c9c889add7ad9dc2cbcf526389ea31d83bee",
            "mlx-metal==0.32.4 --hash=sha256:514df92d41a286895d2564b4b18f4da45096bd7a4a0c47e1042f345ad5a7f936",
            "mlx-audio==0.6.7 --hash=sha256:ed7c2abc5d20d9eadf33086e222ce51bf3c4be77384dc55a8c5729485bbc1b56",
            "certifi==2026.10.23 --hash=sha256:e9b54de76e530bb6cf0ccd036787103acb58fd42ba930ea8bf948ddbaa2f61d1",
            "numpy==2.5.5 --hash=sha256:229743571f901a082f031f511dc4f926b4b4a714075d98b1ff2af62d836365ac",
            "opencv-python==5.1.0.95 --hash=sha256:0b62fb3c71086ea96369e0f47e3b11949345b2f301b88dff97c92c5118176d7b",
            "opentelemetry-api==1.45.2 --hash=sha256:2cbd737377e710ee740d261c94e93fa0f08e6d037c18f17513cc0a2d94479a34",
            "packaging==26.3 --hash=sha256:d7193f7c8e4e93f444fde0262bf90af30e16fa0ad0ad44cb553c87339b23cd1c",
            "pillow==13.0.0 --hash=sha256:d9b0a2bee3f3e03e9f29057aedb807dbf1e1d96ee1b9d0f422d9db6c8583271d",
            "pyarrow==26.0.1 --hash=sha256:15435b595c14583f6ac8495c3ef89ac3fed6bc94f168467bfd3b6ba53ad3a643",
            "pydantic==2.14.4 --hash=sha256:653a2ad9f6d8858529c77c72717d0ea991d6c6837fe683246b2f4b2bbfc3fadc",
            "pydantic_core==2.50.5 --hash=sha256:e8fe516a4b3b06e4ff83b9adee4a786d28a617e5fb5fe90bda49fb6f52a5ac19",
            "annotated-types==0.8.0 --hash=sha256:f072f4d804ea359e4eaf198b1af7a8b0943881a87f31bb764f8bf219bb9419e0",
            "python-multipart==0.0.34 --hash=sha256:ebacd224bd686eee2f9aa1066a657affe07435a185834e05e5235e2e59173e91",
            "PyYAML==6.0.3 --hash=sha256:34d5fcd24b8445fadc33f9cf348c1047101756fd760b4dacb5c3e99755703310",
            "requests==2.34.2 --hash=sha256:2a0d60c172f83ac6ab31e4554906c0f3b3588d37b5cb939b1c061f4907e278e0",
            "charset-normalizer==3.5.2 --hash=sha256:fcff63213e8e6e47770541a4607175404f47cbb3ebea7b6058cc82d524a0e424",
            "urllib3==2.8.0 --hash=sha256:0cf3cae568d36aa9576b28dfb35f11328f1cb974ca7647d9475ebb86c75ac6e3",
            "scipy==1.18.1 --hash=sha256:011413b7426b75012840e35649e00fe0a2c3bae89fed433876e3a99251572efc",
            "sentencepiece==0.2.3 --hash=sha256:ba4dfdbb2972e42d01ad32fe03788717e94d92830f981b7895835e65bfe92b2c",
            "sounddevice==0.5.6 --hash=sha256:e3aef00ad8b1d1740eb66d9a7671eab88a4d2b8fa4ab33498d742e63b65c309c",
            "starlette==1.10.0 --hash=sha256:478c0ab3b4ced6e82719849c118fa406f0278a3499c1a898cd15cd31bdc38ce4",
            "tqdm==4.70.1 --hash=sha256:c293e525e6fef9c20e8728fd4612df02a0aa31bb5fe91ecd93e123b1b7bffa73",
            "transformers==5.23.0 --hash=sha256:018e82fd07490ad5bb58414f65e31576033dc7f36bed10c574843f69b90991bf",
            "tokenizers==0.23.3 --hash=sha256:68649e97d5b43c44c031d8d848874a6eecae8f8fe40ea989aa777a5a83aca716",
            "regex==2026.11.19 --hash=sha256:6b06df7ca4feb4b65402f79cb21939494c28bc54f20ae2fffcee39e9054b7d23",
            "safetensors==0.8.0 --hash=sha256:c80201d22cbf405b80647a60ada77bba06c8fba2da2743ba1e89cdcc39a81f25",
            "truststore==0.10.5 --hash=sha256:9aaaedaefaf06d8b206278cf8b5012bc897f485a874503501e12d776df78951c",
            "typing_extensions==4.16.0 --hash=sha256:481caa481374e813c1b176ada14e97f1f67a4539ce9cfeb3f350d78d6370c2e8",
            "typing-inspection==0.4.5 --hash=sha256:bc210535442b39e1bf2805e0c7a17dfd6a4ace760c84c7f7d16245d23a6b1c39",
            "websockets==17.4 --hash=sha256:a0457561e8a87a6a3b509e21c35084d7d207ced262139de3f738e9676a476bbd",
            "pandas==3.1.1 --hash=sha256:862f6fbef3f05005d78df13ffd9c44606fdcb76af8748ab14c901f6181bf3789",
            "python-dateutil==4.0.0 --hash=sha256:a5e246f79eb80475e691bf93862cead98decd8a498cf53ba0516fcdc0f50dc1c",
            "pycparser==3.12 --hash=sha256:a076f3f489b39b43ed13563a1dafe42447445bec2e294154ec5c57fcdda4c85e",
            "typer==0.29.2 --hash=sha256:f30731f8ca758368b836b3714872e58e395cef8fcaff918c853b1969ed5274e7",
            "rich==15.0.0 --hash=sha256:33bd4ef74232fb73fe9279a257718407f169c09b78a87ad3d296f548e27de0bb",
            "Pygments==2.21.0 --hash=sha256:2363c69b61c4a97c838da3b130dcd6468f4848992b21a82f2a63ec34377137d9",
            "markdown-it-py==4.3.0 --hash=sha256:7aa1511a46c39138de78d9eba92ce47e63124d8003fc98b3a351909e04239efa",
            "mdurl==0.1.2 --hash=sha256:84008a41e51615a49fc9966191ff91509e3c40b939176e643fd50a5c2196b8f8",
            "shellingham==1.5.4 --hash=sha256:7ecfff8f2fd72616f7481040475a65b2bf8af90a56c89140852d1120324e8686",
            "tzdata==2026.6 --hash=sha256:0a10eec6e13ef16b627fabed9a5f2ba61a62b1c67f327305f4e7e412a70b4aec",
            "uvicorn==0.54.0 --hash=sha256:505bdb0f318731d45f1f712071fc781a8981f6847a31c902c9f5e652d4f67faf",
            "xxhash==4.1.0 --hash=sha256:ebe8d35912ce55e0f36532dd36afff76b7893b92bb127916b870094d97cc2d0f",
        ).joinToString(separator = "\n", postfix = "\n")

    init {
        require(
            sha256(
                RequirementsText.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            ) ==
                RequirementsSha256,
        ) {
            "visual Python requirements lock does not match the A005 source-owned digest"
        }
    }

    data class CommandResult(
        val status: Int?,
        val stdout: String,
        val stderr: String,
        val errorMessage: String? = null,
        val timedOut: Boolean = false,
    )

    fun interface Runner {
        fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): CommandResult
    }

    object SystemRunner : Runner {
        override fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): CommandResult =
            runCommand(
                program = program,
                args = args,
                timeoutMs = timeoutMs,
            )
    }

    data class Probe(
        val pythonVersion: String,
        val mlxVlmVersion: String,
        val mlxVersion: String,
        val transformersVersion: String,
        val prefix: String,
        val executable: String,
    )

    data class Result(
        val state: String,
        val environmentRoot: String,
        val pythonPath: String,
        val requirementsPath: String,
        val requirementsSha256: String,
        val pipReportPath: String,
        val pipReportSha256: String,
        val pythonVersion: String,
        val mlxVlmVersion: String,
        val mlxVersion: String,
        val transformersVersion: String,
        val environmentFingerprintSha256: String,
        val environmentMutationPerformed: Boolean,
    )

    data class QualifiedCurrent(
        val environment: Result,
        val evidence: VexLifeVisualInputQualification.QualifiedRuntimeEvidence,
    )

    fun environmentRoot(
        productHome: Path,
    ): Path {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        return VexOwnedPath.resolveInside(
            home,
            "runtime/visual-input/" +
                EnvironmentDirectoryName,
        )
    }

    fun pythonPath(
        environmentRoot: Path,
    ): Path {
        require(
            environmentRoot.isAbsolute,
        ) {
            "visual environment root must be absolute"
        }
        return environmentRoot
            .normalize()
            .resolve(
                "bin/python",
            )
    }

    /** Missing is an ordinary held visual capability. Present-but-invalid fails closed. */
    fun observeCurrent(
        productHome: Path,
        runner: Runner = SystemRunner,
    ): Result? {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val root =
            environmentRoot(
                home,
            )
        if (
            VexOwnedPath.observeDirectory(
                home,
                root,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }

        requireRealDirectory(
            root,
            "visual environment root",
        )
        val python =
            pythonPath(
                root,
            )
        val requirements =
            root.resolve(
                RequirementsFilename,
            )
        val report =
            root.resolve(
                InstallReportFilename,
            )

        requireRegularFile(
            requirements,
            "visual requirements lock",
        )
        val lockBytes =
            Files.readAllBytes(
                requirements,
            )
        require(
            sha256(
                lockBytes,
            ) ==
                RequirementsSha256 &&
                lockBytes.contentEquals(
                    RequirementsText.toByteArray(
                        StandardCharsets.UTF_8,
                    ),
                ),
        ) {
            "visual requirements lock differs from the A005-qualified source lock"
        }

        requireRegularFile(
            report,
            "visual pip installation report",
        )
        validatePipReport(
            report,
        )
        val reportSha256 =
            sha256(
                Files.readAllBytes(
                    report,
                ),
            )

        val probe =
            runtimeProbe(
                python,
                runner,
            )
        verifyProbe(
            probe = probe,
            environmentRoot = root,
            pythonPath = python,
        )

        val fingerprint =
            sha256(
                listOf(
                    RequirementsSha256,
                    reportSha256,
                    probe.pythonVersion,
                    probe.mlxVlmVersion,
                    probe.mlxVersion,
                    probe.transformersVersion,
                    root.toString(),
                    python.toString(),
                )
                    .joinToString(
                        "\n",
                        postfix = "\n",
                    )
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    ),
            )

        return Result(
            state = "REUSED_VERIFIED",
            environmentRoot = root.toString(),
            pythonPath = python.toString(),
            requirementsPath = requirements.toString(),
            requirementsSha256 = RequirementsSha256,
            pipReportPath = report.toString(),
            pipReportSha256 = reportSha256,
            pythonVersion = probe.pythonVersion,
            mlxVlmVersion = probe.mlxVlmVersion,
            mlxVersion = probe.mlxVersion,
            transformersVersion = probe.transformersVersion,
            environmentFingerprintSha256 = fingerprint,
            environmentMutationPerformed = false,
        )
    }

    /**
     * Authorized Product setup convergence.
     *
     * Missing + no creation authority stays absent/held. Existing invalid material may be repaired
     * only when creation is authorized and only after exact non-symlink directory custody is
     * established.
     */
    fun ensure(
        managedPython: ManagedPython.Result,
        productHome: Path,
        allowCreation: Boolean,
        runner: Runner = SystemRunner,
    ): Result? {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val bootstrapPython =
            validateManagedPython(
                home = home,
                managedPython = managedPython,
            )
        val root =
            environmentRoot(
                home,
            )

        if (
            Files.exists(
                root,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            try {
                return observeCurrent(
                    home,
                    runner,
                )
            } catch (
                error: Exception,
            ) {
                if (
                    !allowCreation
                ) {
                    return null
                }
                require(
                    VexOwnedPath.observeDirectory(
                        home,
                        root,
                    ) ==
                        VexOwnedPath.DirectoryState.Present,
                )
                removeRealDirectoryTree(
                    root,
                )
            }
        } else if (
            !allowCreation
        ) {
            return null
        }

        VexOwnedPath.ensureDirectory(
            home,
            requireNotNull(
                root.parent,
            ),
        )

        val bootstrap =
            bootstrapProbe(
                bootstrapPython.toString(),
                runner,
            )
        require(
            bootstrap.first ==
                VexLifeVisualInputQualification.PythonVersion &&
                Path.of(
                    bootstrap.second,
                )
                    .normalize() ==
                bootstrapPython,
        ) {
            "managed bootstrap Python identity/version does not match visual runtime prerequisite"
        }

        val python =
            pythonPath(
                root,
            )
        val requirements =
            root.resolve(
                RequirementsFilename,
            )
        val report =
            root.resolve(
                InstallReportFilename,
            )

        try {
            requireCommandPass(
                runner.run(
                    program = bootstrapPython.toString(),
                    args =
                        listOf(
                            "-m",
                            "venv",
                            root.toString(),
                        ),
                    timeoutMs = 120_000,
                ),
                "visual virtual environment creation",
            )
            requireRealDirectory(
                root,
                "visual environment root",
            )

            Files.write(
                requirements,
                RequirementsText.toByteArray(
                    StandardCharsets.UTF_8,
                ),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
            requireRegularFile(
                requirements,
                "visual requirements lock",
            )

            requireCommandPass(
                runner.run(
                    program = python.toString(),
                    args =
                        listOf(
                            "-m",
                            "pip",
                            "install",
                            "--isolated",
                            "--only-binary=:all:",
                            "--disable-pip-version-check",
                            "--no-input",
                            "--require-hashes",
                            "--report",
                            report.toString(),
                            "-r",
                            requirements.toString(),
                        ),
                    timeoutMs = 1_800_000,
                ),
                "hash-locked mlx-vlm installation",
            )

            val observed =
                requireNotNull(
                    observeCurrent(
                        home,
                        runner,
                    ),
                )
            return observed.copy(
                state = "CREATED_VERIFIED",
                environmentMutationPerformed = true,
            )
        } catch (
            error: Exception,
        ) {
            if (
                Files.exists(
                    root,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                if (
                    Files.isSymbolicLink(
                        root,
                    )
                ) {
                    Files.deleteIfExists(
                        root,
                    )
                } else {
                    removeRealDirectoryTree(
                        root,
                    )
                }
            }
            throw error
        }
    }

    /**
     * Join exact current visual environment truth with the exact currently serving text-model
     * relationship. Environment presence alone never promotes another model family.
     *
     * The visual realization is Mac-arm64-qualified. A same-model runtime on another target cannot
     * borrow that qualification merely because its artifact identity matches.
     */
    fun observeQualifiedCurrent(
        productHome: Path,
        currentTextRuntime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        runner: Runner = SystemRunner,
    ): QualifiedCurrent? {
        val model =
            HereticModelCatalog.Models.Qwen4B
        val textRealization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        if (
            currentTextRuntime.runtimeState !=
                HereticRuntimeState.Qualified ||
                currentTextRuntime.realizationRef !=
                textRealization.realizationRef ||
                currentTextRuntime.modelProfileRef !=
                textRealization.modelProfileRef ||
                currentTextRuntime.modelRef !=
                model.modelRef ||
                currentTextRuntime.artifactRef !=
                model.identity.artifactSetRef ||
                currentTextRuntime.artifactContentSetSha256 !=
                model.identity.artifactContentSetSha256
        ) {
            return null
        }

        val environment =
            observeCurrent(
                productHome,
                runner,
            )
                ?: return null
        val evidence =
            VexLifeVisualInputQualification.validate(
                VexLifeVisualInputQualification.QualifiedRuntimeEvidence(
                    evidenceRef =
                        "evidence.vexlife-prod.visual-runtime." +
                            environment.environmentFingerprintSha256.take(
                                32,
                            ),
                    modelRef = model.modelRef,
                    artifactRef = model.identity.artifactSetRef,
                    artifactContentSetSha256 =
                        model.identity.artifactContentSetSha256,
                    environmentFingerprintSha256 =
                        environment.environmentFingerprintSha256,
                ),
            )
        return QualifiedCurrent(
            environment = environment,
            evidence = evidence,
        )
    }

    fun qualifyCurrent(
        productHome: Path,
        currentTextRuntime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        runner: Runner = SystemRunner,
    ): VexLifeVisualInputQualification.QualifiedRuntimeEvidence? =
        observeQualifiedCurrent(
            productHome = productHome,
            currentTextRuntime = currentTextRuntime,
            runner = runner,
        )
            ?.evidence

    private fun validateManagedPython(
        home: Path,
        managedPython: ManagedPython.Result,
    ): Path {
        val managedRoot =
            VexOwnedPath.resolveInside(
                home,
                ManagedPython.PythonInstallRelativePath,
            )
        val python =
            Path.of(
                managedPython.pythonPath,
            )
                .normalize()

        require(
            (
                managedPython.state ==
                    "REUSED_VERIFIED" ||
                    managedPython.state ==
                    "INSTALLED_VERIFIED"
            ) &&
                managedPython.pythonVersion ==
                VexLifeVisualInputQualification.PythonVersion &&
                managedPython.uvVersion ==
                ManagedPython.UvVersion &&
                !managedPython.globalBinMutationPerformed &&
                Path.of(
                    managedPython.pythonInstallRoot,
                )
                    .normalize() ==
                managedRoot &&
                python.startsWith(
                    managedRoot,
                ),
        ) {
            "visual environment requires one exact VexLife-Prod managed Python result"
        }

        requireRegularFile(
            python,
            "managed bootstrap Python",
        )
        return python
    }

    private fun validatePipReport(
        report: Path,
    ) {
        val root =
            Json
                .parseToJsonElement(
                    Files.readString(
                        report,
                        StandardCharsets.UTF_8,
                    ),
                )
                .jsonObject
        val actual =
            requireNotNull(
                root[
                    "install"
                ],
            )
                .jsonArray
                .associate {
                    entry ->
                    val value =
                        entry.jsonObject
                    val metadata =
                        requireNotNull(
                            value[
                                "metadata"
                            ],
                        )
                            .jsonObject
                    val name =
                        requireNotNull(
                            metadata[
                                "name"
                            ],
                        )
                            .jsonPrimitive
                            .content
                    val version =
                        requireNotNull(
                            metadata[
                                "version"
                            ],
                        )
                            .jsonPrimitive
                            .content
                    val sha256 =
                        requireNotNull(
                            requireNotNull(
                                requireNotNull(
                                    requireNotNull(
                                        value[
                                            "download_info"
                                        ],
                                    )
                                        .jsonObject[
                                            "archive_info"
                                        ],
                                )
                                    .jsonObject[
                                        "hashes"
                                    ],
                            )
                                .jsonObject[
                                    "sha256"
                                ],
                        )
                            .jsonPrimitive
                            .content
                    normalizePackageName(
                        name,
                    ) to
                        (
                            version to
                                sha256
                        )
                }

        val expected =
            RequirementsText
                .lineSequence()
                .filter(
                    String::isNotBlank,
                )
                .associate {
                    line ->
                    val nameVersion =
                        line.substringBefore(
                            " --hash=sha256:",
                        )
                    val name =
                        nameVersion.substringBeforeLast(
                            "==",
                        )
                    val version =
                        nameVersion.substringAfterLast(
                            "==",
                        )
                    normalizePackageName(
                        name,
                    ) to
                        (
                            version to
                                line.substringAfter(
                                    " --hash=sha256:",
                                )
                        )
                }

        require(
            actual.size ==
                expected.size &&
                actual ==
                expected,
        ) {
            "visual pip installation report does not match the exact A005 wheel lock"
        }
    }

    private fun runtimeProbe(
        python: Path,
        runner: Runner,
    ): Probe {
        val script =
            "import importlib.metadata as md,sys\n" +
                "import mlx_vlm\n" +
                "from mlx_vlm import load,generate\n" +
                "from mlx_vlm.prompt_utils import apply_chat_template\n" +
                "print('%d.%d.%d'%sys.version_info[:3])\n" +
                "print(md.version('mlx-vlm'))\n" +
                "print(md.version('mlx'))\n" +
                "print(md.version('transformers'))\n" +
                "print(sys.prefix)\n" +
                "print(sys.executable)"

        val lines =
            requireCommandPass(
                runner.run(
                    program = python.toString(),
                    args =
                        listOf(
                            "-c",
                            script,
                        ),
                    timeoutMs = 60_000,
                ),
                "visual Python environment probe",
            )
                .lineSequence()
                .map(
                    String::trim,
                )
                .filter(
                    String::isNotEmpty,
                )
                .toList()

        require(
            lines.size ==
                6,
        ) {
            "visual Python environment probe returned an invalid result"
        }

        return Probe(
            pythonVersion = lines[0],
            mlxVlmVersion = lines[1],
            mlxVersion = lines[2],
            transformersVersion = lines[3],
            prefix = lines[4],
            executable = lines[5],
        )
    }

    private fun verifyProbe(
        probe: Probe,
        environmentRoot: Path,
        pythonPath: Path,
    ) {
        require(
            probe.pythonVersion ==
                VexLifeVisualInputQualification.PythonVersion &&
                probe.mlxVlmVersion ==
                VexLifeVisualInputQualification.MlxVlmVersion &&
                probe.mlxVersion ==
                VexLifeVisualInputQualification.MlxVersion &&
                probe.transformersVersion ==
                VexLifeVisualInputQualification.TransformersVersion,
        ) {
            "visual Python package versions differ from the A005-qualified runtime identity"
        }
        require(
            Path.of(
                probe.prefix,
            )
                .normalize() ==
                environmentRoot &&
                Path.of(
                    probe.executable,
                )
                    .normalize() ==
                pythonPath,
        ) {
            "visual Python probe does not bind the selected environment root/executable"
        }
    }

    private fun bootstrapProbe(
        python: String,
        runner: Runner,
    ): Pair<String, String> {
        val lines =
            requireCommandPass(
                runner.run(
                    program = python,
                    args =
                        listOf(
                            "-c",
                            "import sys\nprint('%d.%d.%d'%sys.version_info[:3])\nprint(sys.executable)",
                        ),
                    timeoutMs = 30_000,
                ),
                "visual bootstrap Python probe",
            )
                .lineSequence()
                .map(
                    String::trim,
                )
                .filter(
                    String::isNotEmpty,
                )
                .toList()
        require(
            lines.size ==
                2,
        ) {
            "visual bootstrap Python probe returned an invalid result"
        }
        return lines[0] to
            lines[1]
    }

    private fun normalizePackageName(
        name: String,
    ): String =
        name
            .lowercase()
            .replace(
                Regex(
                    "[-_.]+",
                ),
                "-",
            )

    private fun requireCommandPass(
        result: CommandResult,
        label: String,
    ): String {
        require(
            result.errorMessage ==
                null &&
                !result.timedOut &&
                result.status ==
                0,
        ) {
            label +
                " failed: " +
                (
                    result.errorMessage
                        ?: result.stderr.take(
                            4_000,
                        )
                )
        }
        return result.stdout
    }

    private fun requireRealDirectory(
        path: Path,
        label: String,
    ) {
        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )
        require(
            attributes.isDirectory &&
                !attributes.isSymbolicLink,
        ) {
            label +
                " must be one real non-symlink directory"
        }
    }

    private fun requireRegularFile(
        path: Path,
        label: String,
    ) {
        require(
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            label +
                " is missing"
        }
        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )
        require(
            attributes.isRegularFile &&
                !attributes.isSymbolicLink,
        ) {
            label +
                " must be one regular non-symlink file"
        }
    }

    private fun removeRealDirectoryTree(
        root: Path,
    ) {
        requireRealDirectory(
            root,
            "visual environment root",
        )
        Files.walkFileTree(
            root,
            object :
                SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    Files.delete(
                        file,
                    )
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(
                    directory: Path,
                    error: java.io.IOException?,
                ): FileVisitResult {
                    if (
                        error !=
                            null
                    ) {
                        throw error
                    }
                    Files.delete(
                        directory,
                    )
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    private fun sha256(
        bytes: ByteArray,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                bytes,
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }

    private fun runCommand(
        program: String,
        args: List<String>,
        timeoutMs: Long,
    ): CommandResult =
        try {
            val process =
                ProcessBuilder(
                    listOf(
                        program,
                    ) +
                        args,
                )
                    .start()
            var stdout =
                ""
            var stderr =
                ""
            val stdoutThread =
                thread(
                    start = true,
                    name = "vexlife-visual-python-stdout",
                ) {
                    stdout =
                        process.inputStream
                            .bufferedReader()
                            .readText()
                }
            val stderrThread =
                thread(
                    start = true,
                    name = "vexlife-visual-python-stderr",
                ) {
                    stderr =
                        process.errorStream
                            .bufferedReader()
                            .readText()
                }
            val completed =
                process.waitFor(
                    timeoutMs,
                    TimeUnit.MILLISECONDS,
                )
            if (
                !completed
            ) {
                process.destroyForcibly()
                process.waitFor()
            }
            stdoutThread.join()
            stderrThread.join()
            CommandResult(
                status =
                    if (
                        completed
                    ) {
                        process.exitValue()
                    } else {
                        null
                    },
                stdout = stdout,
                stderr = stderr,
                timedOut =
                    !completed,
            )
        } catch (
            error: Exception,
        ) {
            CommandResult(
                status = null,
                stdout = "",
                stderr = "",
                errorMessage =
                    error.message
                        ?: error::class.simpleName
                        ?: "command failed",
            )
        }
}

// [VXG RealForever]
