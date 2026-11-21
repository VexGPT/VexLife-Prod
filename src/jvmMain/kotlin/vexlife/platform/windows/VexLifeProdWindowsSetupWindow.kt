package vexlife.platform.windows

import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URI
import java.nio.file.Path
import java.util.Locale
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.SwingWorker
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.local.VexLifeProdSetupProgress
import vexlife.model.heretic.HereticModelCatalog
import vexlife.person.VexLifePersonSettings
import vexlife.setup.VexLifeProdSetupLanguage
import vexlife.setup.VexLifeProdSetupModel
import vexlife.text.VexLifeStrings
import vexlife.text.VexLifeText

/**
 * Native Windows setup manifestation for the lived release preview.
 *
 * The window renders portable Entry Family truth and emits only Family Controls. Home/model/language
 * ownership and Windows runtime effects stay with their existing owners; Swing owns presentation only.
 */
internal class VexLifeProdWindowsSetupWindow private constructor(
    private val plan: VexLifeProdWindowsPreview.LaunchPlan,
) : AutoCloseable {
    companion object {
        private const val WindowsStringScope = "windows-setup"

        internal fun offeredLanguagesForWindow(): List<VexLifeProdSetupLanguage.Language> {
            val complete = VexLifeStrings.catalog.completeLanguages(WindowsStringScope).toSet()
            return VexLifeProdSetupLanguage.offered().filter { it.code in complete }
        }

        internal fun windowsLanguageFor(selectedLanguage: String): String =
            VexLifeStrings.selectLanguage(WindowsStringScope, selectedLanguage)

        internal fun windowsTextFor(
            key: String,
            selectedLanguage: String,
        ): String = VexLifeStrings.text(key, windowsLanguageFor(selectedLanguage))

        internal fun windowsResolveFor(
            key: String,
            args: Map<String, String>,
            selectedLanguage: String,
        ): String =
            VexLifeStrings.resolve(
                VexLifeText(
                    key = key,
                    args = args,
                ),
                windowsLanguageFor(selectedLanguage),
            )
    }

    private data class LanguageChoice(
        val code: String,
        val name: String,
    ) {
        override fun toString(): String = name
    }

    private data class ModelChoice(
        val model: HereticModelCatalog.Entry,
        val label: String,
    ) {
        override fun toString(): String = label
    }

    private val frame = JFrame("VexLife")
    private val languageBox = JComboBox<LanguageChoice>()
    private val homeField = JTextField(plan.productHome.toString())
    private val chooseHomeButton = JButton()
    private val modelBox = JComboBox<ModelChoice>()
    private val railLabel = JLabel()
    private val titleLabel = JLabel()
    private val body = JTextArea()
    private val progressLabel = JLabel(" ")
    private val progressBar = JProgressBar()
    private val primaryButton = JButton()
    private val recheckButton = JButton()
    private val closeButton = JButton()

    private var selectedHome: Path = plan.productHome
    private var selectedLanguage: String =
        runCatching {
                VexLifeProdSetupLanguage.resolve(
                    productHome = selectedHome,
                    explicit = null,
                    system = Locale.getDefault().language,
                )
            }
            .getOrDefault(VexLifePersonSettings.DefaultLanguage)
    private var selectedModelHandle: String? = null
    private var observation: VexLifeProdWindowsSetup.Observation? = null
    private var runningProduct: VexLifeProdWindowsSetup.RunningProduct? = null
    private var worker: SwingWorker<*, *>? = null
    private var updatingControls = false
    private var closed = false

    init {
        configureWindow()
        refresh()
    }

    private fun configureWindow() {
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.minimumSize = Dimension(760, 620)
        frame.preferredSize = Dimension(820, 680)
        frame.addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(event: WindowEvent?) {
                    if (worker != null) {
                        JOptionPane.showMessageDialog(
                            frame,
                            windowsText("windows-setup.description.working-close-held"),
                            "VexLife",
                            JOptionPane.INFORMATION_MESSAGE,
                        )
                    } else {
                        close()
                    }
                }
            },
        )

        val root = JPanel(BorderLayout(0, 16))
        root.border = BorderFactory.createEmptyBorder(20, 22, 20, 22)

        val selectors = JPanel(GridBagLayout())
        val constraints =
            GridBagConstraints().apply {
                insets = Insets(4, 4, 4, 4)
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
            }

        constraints.gridx = 0
        constraints.gridy = 0
        constraints.weightx = 0.0
        selectors.add(JLabel(setupText("setup.button.change-language")), constraints)
        constraints.gridx = 1
        constraints.weightx = 1.0
        selectors.add(languageBox, constraints)

        constraints.gridx = 0
        constraints.gridy = 1
        constraints.weightx = 0.0
        selectors.add(JLabel(setupText("setup.label.step-home")), constraints)
        constraints.gridx = 1
        constraints.weightx = 1.0
        selectors.add(homeField, constraints)
        constraints.gridx = 2
        constraints.weightx = 0.0
        chooseHomeButton.text = setupText("setup.button.choose-folder")
        selectors.add(chooseHomeButton, constraints)

        constraints.gridx = 0
        constraints.gridy = 2
        selectors.add(JLabel(setupText("setup.button.change-model")), constraints)
        constraints.gridx = 1
        constraints.gridwidth = 2
        constraints.weightx = 1.0
        selectors.add(modelBox, constraints)
        constraints.gridwidth = 1

        val center = JPanel(BorderLayout(0, 10))
        railLabel.font = railLabel.font.deriveFont(Font.PLAIN, 13f)
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 22f)
        body.isEditable = false
        body.lineWrap = true
        body.wrapStyleWord = true
        body.font = body.font.deriveFont(Font.PLAIN, 14f)
        body.background = root.background
        body.border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
        val bodyScroll = JScrollPane(body)
        bodyScroll.border = BorderFactory.createEmptyBorder()
        center.add(railLabel, BorderLayout.NORTH)

        val card = JPanel(BorderLayout(0, 10))
        card.add(titleLabel, BorderLayout.NORTH)
        card.add(bodyScroll, BorderLayout.CENTER)
        center.add(card, BorderLayout.CENTER)

        progressBar.isIndeterminate = true
        progressBar.isVisible = false
        val progress = JPanel(BorderLayout(0, 4))
        progress.add(progressLabel, BorderLayout.NORTH)
        progress.add(progressBar, BorderLayout.SOUTH)
        center.add(progress, BorderLayout.SOUTH)

        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
        actions.add(recheckButton)
        actions.add(primaryButton)
        actions.add(closeButton)

        root.add(selectors, BorderLayout.NORTH)
        root.add(center, BorderLayout.CENTER)
        root.add(actions, BorderLayout.SOUTH)

        frame.contentPane = root
        frame.pack()
        frame.setLocationRelativeTo(null)

        languageBox.addActionListener {
            if (!updatingControls) {
                (languageBox.selectedItem as? LanguageChoice)?.let {
                    selectedLanguage = it.code
                    persistLanguageIfPossible()
                    // Language selection changes presentation only. Reuse the already-observed
                    // setup truth instead of synchronously re-running exact Windows runtime/model
                    // observation (including multi-GiB hashing) on Swing's event-dispatch thread.
                    observation
                        ?.let(
                            ::renderObservation,
                        )
                        ?: refresh()
                }
            }
        }

        homeField.addActionListener {
            if (worker == null) {
                runCatching {
                        val candidate = Path.of(homeField.text.trim())
                        require(candidate.isAbsolute) {
                            "Vex Home must be an absolute path"
                        }
                        selectedHome = candidate.normalize()
                    }
                    .onSuccess {
                        refresh()
                    }
                    .onFailure(::showError)
            }
        }

        chooseHomeButton.addActionListener {
            if (worker != null) return@addActionListener
            val chooser =
                JFileChooser(selectedHome.toFile()).apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = setupText("setup.heading.choose-home")
                }
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                selectedHome = chooser.selectedFile.toPath().toAbsolutePath().normalize()
                homeField.text = selectedHome.toString()
                refresh()
            }
        }

        modelBox.addActionListener {
            if (!updatingControls && worker == null) {
                (modelBox.selectedItem as? ModelChoice)?.let {
                    selectedModelHandle = it.model.handle
                    persistModelIfPossible(it.model)
                    refresh()
                }
            }
        }

        recheckButton.addActionListener {
            if (worker == null) refresh()
        }
        primaryButton.addActionListener {
            if (worker == null) performPrimary()
        }
        closeButton.addActionListener {
            if (worker == null) close()
        }

        Runtime.getRuntime()
            .addShutdownHook(
                Thread(
                    {
                        runCatching {
                            runningProduct?.close()
                        }
                    },
                    "vexlife-windows-preview-shutdown",
                ),
            )
    }

    fun show() {
        frame.isVisible = true
    }

    private fun refresh() {
        if (closed || worker != null) return

        if (runningProduct != null) {
            renderRunning()
            return
        }

        val observed =
            try {
                VexLifeProdWindowsSetup.observe(
                    desiredSourceSha = plan.sourceSha,
                    productHome = selectedHome,
                    explicitModelHandle = selectedModelHandle,
                )
            } catch (error: Throwable) {
                showError(error)
                return
            }
        observation = observed

        if (selectedModelHandle == null) {
            selectedModelHandle = observed.selectedModel?.handle
        }
        renderObservation(
            observed,
        )
    }

    private fun renderObservation(
        observed: VexLifeProdWindowsSetup.Observation,
    ) {
        updatingControls = true
        try {
            populateLanguages()
            populateModels(observed)
        } finally {
            updatingControls = false
        }

        homeField.text = selectedHome.toString()
        homeField.isEnabled = true
        chooseHomeButton.isEnabled = true
        languageBox.isEnabled = true
        modelBox.isEnabled = observed.supportedModels.isNotEmpty()

        railLabel.text = journeyText(observed)
        titleLabel.text = titleText(observed)
        body.text = bodyText(observed)
        body.caretPosition = 0

        val primaryRef = observed.displayState.primaryControlRef
        primaryButton.isVisible = primaryRef != null
        primaryButton.isEnabled = primaryRef != null
        primaryButton.text =
            observed.displayState.primaryLabel?.let {
                VexLifeStrings.resolve(it, selectedLanguage)
            } ?: ""

        recheckButton.text = setupText("setup.button.check-again")
        recheckButton.isVisible = observed.displayState.recheckLabel != null
        recheckButton.isEnabled = true

        closeButton.text =
            VexLifeStrings.resolve(
                observed.displayState.cancelLabel,
                selectedLanguage,
            )
        closeButton.isEnabled = true

        progressLabel.text = " "
        progressBar.isVisible = false
    }

    private fun renderRunning() {
        homeField.isEnabled = false
        chooseHomeButton.isEnabled = false
        languageBox.isEnabled = false
        modelBox.isEnabled = false

        railLabel.text =
            listOf(
                    windowsText("windows-setup.label.step-computer"),
                    setupText("setup.label.step-home"),
                    setupText("setup.label.step-model"),
                    setupText("setup.label.step-start"),
                    setupText("setup.label.step-ready"),
                )
                .joinToString("   ") {
                    "✓ $it"
                }
        titleLabel.text = setupText("setup.heading.current")
        body.text =
            windowsText("windows-setup.description.running") +
                "\n\n" +
                windowsResolve(
                    "windows-setup.label.home",
                    mapOf("path" to selectedHome.toString()),
                ) +
                "\n" +
                observation
                    ?.selectedModel
                    ?.let {
                        windowsResolve(
                            "windows-setup.label.model",
                            mapOf("model" to it.humanLabel),
                        )
                    }
                    .orEmpty()
        body.caretPosition = 0

        primaryButton.isVisible = true
        primaryButton.isEnabled = true
        primaryButton.text = setupText("setup.button.open-vexlife")
        recheckButton.isVisible = false
        closeButton.text = setupText("setup.button.close")
        closeButton.isEnabled = true
        progressBar.isVisible = false
        progressLabel.text = " "
    }

    private fun populateLanguages() {
        val choices =
            offeredLanguagesForWindow().map {
                LanguageChoice(
                    code = it.code,
                    name = it.name,
                )
            }
        languageBox.removeAllItems()
        choices.forEach(languageBox::addItem)
        languageBox.selectedItem =
            choices.firstOrNull {
                it.code == selectedLanguage
            }
                ?: choices.firstOrNull()
    }

    private fun populateModels(
        observed: VexLifeProdWindowsSetup.Observation,
    ) {
        val memory = observed.currentState?.totalMemoryBytes
        val choices =
            observed.supportedModels.map { model ->
                val fits = HereticModelCatalog.fits(model, memory)
                val fitKey =
                    if (model.autoRecommended && fits == true) {
                        "windows-setup.status.model-recommended"
                    } else {
                        "windows-setup.status.model-fits"
                    }
                val label =
                    VexLifeStrings.resolve(
                        VexLifeText(
                            key = "setup.label.model-choice",
                            args = mapOf("model" to model.humanLabel),
                            gib = mapOf("size" to model.totalBytes),
                            parts =
                                mapOf(
                                    "fit" to
                                        VexLifeText(
                                            fitKey,
                                        ),
                                ),
                        ),
                        "en",
                    )
                ModelChoice(
                    model = model,
                    label = label,
                )
            }
        modelBox.removeAllItems()
        choices.forEach(modelBox::addItem)
        val selected =
            choices.firstOrNull {
                it.model.handle == selectedModelHandle
            }
                ?: choices.firstOrNull {
                    it.model.handle == observed.selectedModel?.handle
                }
        if (selected != null) {
            modelBox.selectedItem = selected
        } else {
            modelBox.selectedIndex = -1
        }
    }

    private fun titleText(
        observed: VexLifeProdWindowsSetup.Observation,
    ): String =
        when (observed.entryState) {
            is VexLifeProdEntryFamily.HostHeld ->
                windowsText("windows-setup.heading.host-held")
            else ->
                VexLifeStrings.resolve(
                    observed.displayState.title,
                    selectedLanguage,
                )
        }

    private fun detailText(
        observed: VexLifeProdWindowsSetup.Observation,
    ): String =
        when (val state = observed.entryState) {
            is VexLifeProdEntryFamily.HomeFormationRequired ->
                windowsText("windows-setup.description.home-formation-required")
            is VexLifeProdEntryFamily.StartRequired ->
                windowsText(
                    if (
                        state.priorServiceSourceSha != null &&
                            state.priorServiceSourceSha != plan.sourceSha
                    ) {
                        "windows-setup.description.start-required-newer-version"
                    } else {
                        "windows-setup.description.start-required"
                    },
                )
            is VexLifeProdEntryFamily.HostHeld ->
                windowsText("windows-setup.description.host-held")
            is VexLifeProdEntryFamily.HomeHeld ->
                windowsText("windows-setup.description.home-held")
            is VexLifeProdEntryFamily.HeldUnknown ->
                if (state.detail == "SELECTED_MODEL_NOT_SUPPORTED_ON_WINDOWS") {
                    windowsText("windows-setup.description.model-not-supported")
                } else {
                    windowsText("windows-setup.description.held-unknown")
                }
            else ->
                VexLifeStrings.resolve(
                    observed.displayState.detail,
                    selectedLanguage,
                )
        }

    private fun journeyText(
        observed: VexLifeProdWindowsSetup.Observation,
    ): String =
        observed.displayState.journey.joinToString("   ") { step ->
            val mark =
                when (step.statusRef) {
                    VexLifeProdEntryFamily.Complete -> "✓"
                    VexLifeProdEntryFamily.JourneyCurrent -> "●"
                    VexLifeProdEntryFamily.JourneyHeld -> "⊘"
                    else -> "○"
                }
            val label =
                if (step.stepRef == VexLifeProdEntryFamily.JourneyStepMac) {
                    windowsText("windows-setup.label.step-computer")
                } else {
                    VexLifeStrings.resolve(step.label, selectedLanguage)
                }
            "$mark $label"
        }

    private fun bodyText(
        observed: VexLifeProdWindowsSetup.Observation,
    ): String =
        buildString {
            append(detailText(observed))
            append("\n\n")
            append(
                windowsResolve(
                    "windows-setup.label.home",
                    mapOf("path" to selectedHome.toString()),
                ),
            )
            observed.selectedModel?.let { model ->
                append("\n")
                append(
                    windowsResolve(
                        "windows-setup.label.model",
                        mapOf("model" to model.humanLabel),
                    ),
                )
            }
            observed.freeStorageBytes?.let { bytes ->
                append("\n")
                append(
                    windowsResolve(
                        "windows-setup.label.free-space",
                        mapOf(
                            "space" to
                                String.format(
                                    Locale.US,
                                    "%.1f",
                                    bytes.toDouble() / (1024.0 * 1024.0 * 1024.0),
                                ),
                        ),
                    ),
                )
            }
            observed.materialization?.let {
                append("\n")
                append(
                    windowsText(
                        if (it.ready) {
                            "windows-setup.label.runtime-ready"
                        } else {
                            "windows-setup.label.runtime-needed"
                        },
                    ),
                )
            }
        }

    private fun performPrimary() {
        runningProduct?.let {
            openBrowser(it.origin)
            return
        }

        val observed = requireNotNull(observation)
        val ref = observed.displayState.primaryControlRef ?: return
        val control = VexLifeProdEntryFamily.controlForRef(ref) ?: return
        val act =
            VexLifeProdEntryFamily.Semantics.resolve(
                currentState = observed.entryState,
                control = control,
            ) ?: return

        when (val effect = act.effect) {
            VexLifeProdEntryFamily.FormCanonicalHome -> {
                runEffect(
                    initialLine = setupText("setup.status.working"),
                ) {
                    VexLifeProdWindowsSetup.formHome(selectedHome)
                    val model =
                        requireNotNull(
                            observed.selectedModel,
                        ) {
                            "Windows setup has no selected model to carry into the new Home"
                        }
                    VexLifeProdWindowsSetup.persistSelections(
                        productHome = selectedHome,
                        language = selectedLanguage,
                        model = model,
                    )
                    null
                }
            }

            is VexLifeProdEntryFamily.ConvergeProduct -> {
                val model =
                    requireNotNull(
                        observed.selectedModel,
                    ) {
                        "Windows setup has no selected model"
                    }
                runEffect(
                    initialLine = setupText("setup.status.working"),
                ) {
                    val progress =
                        VexLifeProdSetupProgress.Lines(
                            sink = { line ->
                                SwingUtilities.invokeLater {
                                    progressLabel.text =
                                        line.removePrefix(
                                            VexLifeProdSetupProgress.LinePrefix,
                                        )
                                }
                            },
                            language = selectedLanguage,
                        )
                    VexLifeProdWindowsSetup.startProduct(
                        desiredSourceSha = effect.desiredSourceSha,
                        productHome = selectedHome,
                        language = selectedLanguage,
                        model = model,
                        allowAcquisition = effect.allowAcquisition,
                        progress = progress,
                    )
                }
            }

            VexLifeProdEntryFamily.OpenPresentation -> {
                runningProduct?.let {
                    openBrowser(it.origin)
                }
            }

            is VexLifeProdEntryFamily.RetireStaleService -> {
                showError(
                    IllegalStateException(
                        "Windows setup does not currently own stale-service retirement; recheck the current Windows service state instead",
                    ),
                )
            }

            null -> Unit
        }
    }

    private fun runEffect(
        initialLine: String,
        block: () -> VexLifeProdWindowsSetup.RunningProduct?,
    ) {
        setWorking(true)
        progressLabel.text = initialLine

        val nextWorker =
            object : SwingWorker<VexLifeProdWindowsSetup.RunningProduct?, Unit>() {
                override fun doInBackground(): VexLifeProdWindowsSetup.RunningProduct? = block()

                override fun done() {
                    if (worker !== this) return
                    worker = null
                    try {
                        val running = get()
                        if (running != null) {
                            runningProduct = running
                            openBrowser(running.origin)
                        }
                    } catch (error: Throwable) {
                        showError(error.cause ?: error)
                    } finally {
                        setWorking(false)
                        if (!closed) refresh()
                    }
                }
            }
        worker = nextWorker
        nextWorker.execute()
    }

    private fun setWorking(
        working: Boolean,
    ) {
        languageBox.isEnabled = !working
        homeField.isEnabled = !working
        chooseHomeButton.isEnabled = !working
        modelBox.isEnabled = !working
        primaryButton.isEnabled = !working
        recheckButton.isEnabled = !working
        progressBar.isVisible = working
        closeButton.text = setupText("setup.button.close")
        closeButton.isEnabled = !working
    }

    private fun persistLanguageIfPossible() {
        runCatching {
            VexLifeProdSetupLanguage.persist(
                productHome = selectedHome,
                language = selectedLanguage,
            )
        }
    }

    private fun persistModelIfPossible(
        model: HereticModelCatalog.Entry,
    ) {
        runCatching {
            VexLifeProdSetupModel.persist(
                productHome = selectedHome,
                model = model,
            )
        }
    }

    private fun openBrowser(
        origin: String,
    ) {
        val opened =
            runCatching {
                    require(Desktop.isDesktopSupported())
                    val desktop = Desktop.getDesktop()
                    require(desktop.isSupported(Desktop.Action.BROWSE))
                    desktop.browse(URI.create(origin))
                }
                .isSuccess
        if (!opened) {
            JOptionPane.showMessageDialog(
                frame,
                origin,
                "VexLife",
                JOptionPane.INFORMATION_MESSAGE,
            )
        }
    }

    private fun showError(
        error: Throwable,
    ) {
        JOptionPane.showMessageDialog(
            frame,
            error.javaClass.simpleName + ": " + (error.message ?: "unknown failure"),
            "VexLife",
            JOptionPane.ERROR_MESSAGE,
        )
    }

    private fun setupText(
        key: String,
    ): String =
        runCatching {
                VexLifeStrings.text(
                    key,
                    selectedLanguage,
                )
            }
            .getOrElse {
                VexLifeStrings.text(
                    key,
                    "en",
                )
            }

    private fun windowsText(
        key: String,
    ): String = windowsTextFor(key, selectedLanguage)

    private fun windowsResolve(
        key: String,
        args: Map<String, String>,
    ): String = windowsResolveFor(key, args, selectedLanguage)

    override fun close() {
        if (closed) return
        closed = true
        worker?.cancel(true)
        worker = null
        runCatching {
            runningProduct?.close()
        }
        runningProduct = null
        frame.dispose()
    }

    companion object {
        fun open(
            plan: VexLifeProdWindowsPreview.LaunchPlan,
        ) {
            runCatching {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName(),
                )
            }
            SwingUtilities.invokeLater {
                try {
                    VexLifeProdWindowsPreview.StartupDiagnostics.record("SWING_OPEN")
                    VexLifeProdWindowsSetupWindow(plan).show()
                    VexLifeProdWindowsPreview.StartupDiagnostics.record("WINDOW_VISIBLE")
                } catch (error: Throwable) {
                    VexLifeProdWindowsPreview.fatalStartup(
                        phase = "SWING_OPEN",
                        error = error,
                    )
                }
            }
        }
    }
}

// [VXG RealForever]
