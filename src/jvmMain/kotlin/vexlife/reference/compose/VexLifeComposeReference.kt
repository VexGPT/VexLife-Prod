package vexlife.reference.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import vexlife.workspace.VexLifeWorkspaceBlueprint

private enum class CompanionPane {
    Conversation,
    Settings,
}

fun main() =
    application {
        Window(
            onCloseRequest =
                ::exitApplication,
            title =
                "VexLife Prod · Compose Reference",
        ) {
            MaterialTheme(
                colorScheme =
                    darkColorScheme(),
            ) {
                VexLifeWorkspaceReference()
            }
        }
    }

/**
 * JVM Compose manifestation of the accepted static Workspace Blueprint.
 *
 * This renderer owns no product State. The Conversation/Settings pane choice is intentionally local
 * presentation state because current source has not earned a semantic Family for that visual swap.
 */
@Composable
fun VexLifeWorkspaceReference() {
    var companionPane by
        remember {
            mutableStateOf(
                CompanionPane.Conversation,
            )
        }

    Surface(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme
                            .colorScheme
                            .background,
                    ),
        ) {
            WorkspaceHeader()
            Row(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                BrowserReference(
                    modifier =
                        Modifier
                            .weight(
                                1f,
                            )
                            .fillMaxHeight(),
                )
                CompanionReference(
                    pane =
                        companionPane,
                    onSettings = {
                        companionPane =
                            CompanionPane.Settings
                    },
                    onReturn = {
                        companionPane =
                            CompanionPane.Conversation
                    },
                    modifier =
                        Modifier
                            .width(
                                360.dp,
                            )
                            .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun WorkspaceHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    64.dp,
                )
                .testTag(
                    VexLifeWorkspaceBlueprint
                        .Header
                        .ref,
                )
                .padding(
                    horizontal =
                        16.dp,
                    vertical =
                        10.dp,
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp,
            ),
    ) {
        Text(
            text =
                "VexLife",
            modifier =
                Modifier.testTag(
                    VexLifeWorkspaceBlueprint
                        .Brand
                        .ref,
                ),
            fontWeight =
                FontWeight.Bold,
        )

        OutlinedTextField(
            value =
                "",
            onValueChange =
                {},
            enabled =
                false,
            placeholder = {
                Text(
                    "Browser query · unbound",
                )
            },
            modifier =
                Modifier
                    .weight(
                        1f,
                    )
                    .testTag(
                        VexLifeWorkspaceBlueprint
                            .BrowserQuery
                            .ref,
                    ),
            singleLine =
                true,
        )

        Button(
            onClick =
                {},
            enabled =
                false,
            modifier =
                Modifier.testTag(
                    VexLifeWorkspaceBlueprint
                        .AppSelector
                        .ref,
                ),
        ) {
            Text(
                "Vex",
            )
        }

        Spacer(
            modifier =
                Modifier
                    .width(
                        20.dp,
                    )
                    .testTag(
                        VexLifeWorkspaceBlueprint
                            .Utilities
                            .ref,
                    ),
        )
    }
}

@Composable
private fun BrowserReference(
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .testTag(
                    VexLifeWorkspaceBlueprint
                        .Browser
                        .ref,
                )
                .padding(
                    12.dp,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(
                        VexLifeWorkspaceBlueprint
                            .BrowserViewport
                            .ref,
                    )
                    .border(
                        width =
                            1.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .outlineVariant,
                        shape =
                            RoundedCornerShape(
                                12.dp,
                            ),
                    ),
            contentAlignment =
                Alignment.Center,
        ) {
            Text(
                "Nested browser · not connected",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompanionReference(
    pane: CompanionPane,
    onSettings: () -> Unit,
    onReturn: () -> Unit,
    modifier: Modifier,
) {
    Card(
        modifier =
            modifier
                .testTag(
                    VexLifeWorkspaceBlueprint
                        .Companion
                        .ref,
                )
                .padding(
                    12.dp,
                ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface,
                ),
    ) {
        when (
            pane
        ) {
            CompanionPane.Conversation ->
                ConversationReference(
                    onSettings,
                )

            CompanionPane.Settings ->
                SettingsReference(
                    onReturn,
                )
        }
    }
}

@Composable
private fun ConversationReference(
    onSettings: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(
                    VexLifeWorkspaceBlueprint
                        .Conversation
                        .ref,
                )
                .padding(
                    16.dp,
                ),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Text(
                "VEX",
                fontWeight =
                    FontWeight.Bold,
            )
            Spacer(
                Modifier.weight(
                    1f,
                ),
            )
            TextButton(
                onClick =
                    onSettings,
            ) {
                Text(
                    "Settings",
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .weight(
                        1f,
                    )
                    .fillMaxWidth()
                    .testTag(
                        VexLifeWorkspaceBlueprint
                            .ConversationTimeline
                            .ref,
                    ),
            contentAlignment =
                Alignment.Center,
        ) {
            Text(
                "Conversation · unbound",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }

        Text(
            "Vex response progress · unavailable",
            modifier =
                Modifier
                    .testTag(
                        VexLifeWorkspaceBlueprint
                            .ResponseProgress
                            .ref,
                    )
                    .padding(
                        bottom =
                            8.dp,
                    ),
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )

        OutlinedTextField(
            value =
                "",
            onValueChange =
                {},
            enabled =
                false,
            placeholder = {
                Text(
                    "Ask me anything…",
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(
                        VexLifeWorkspaceBlueprint
                            .Composer
                            .ref,
                    ),
        )

        Button(
            onClick =
                {},
            enabled =
                false,
            modifier =
                Modifier
                    .align(
                        Alignment.End,
                    )
                    .padding(
                        top =
                            8.dp,
                    )
                    .testTag(
                        VexLifeWorkspaceBlueprint
                            .Send
                            .ref,
                    ),
        ) {
            Text(
                "Send",
            )
        }
    }
}

@Composable
private fun SettingsReference(
    onReturn: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(
                    VexLifeWorkspaceBlueprint
                        .Settings
                        .ref,
                )
                .padding(
                    16.dp,
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                14.dp,
            ),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Text(
                "Settings",
                fontWeight =
                    FontWeight.Bold,
            )
            Spacer(
                Modifier.weight(
                    1f,
                ),
            )
            TextButton(
                onClick =
                    onReturn,
            ) {
                Text(
                    "Return",
                )
            }
        }

        SettingsPlaceholder(
            title =
                "Context",
            status =
                "unbound",
            modifier =
                Modifier.testTag(
                    VexLifeWorkspaceBlueprint
                        .SettingsContext
                        .ref,
                ),
        )
        SettingsPlaceholder(
            title =
                "Resources",
            status =
                "unbound",
            modifier =
                Modifier.testTag(
                    VexLifeWorkspaceBlueprint
                        .SettingsResources
                        .ref,
                ),
        )
        SettingsPlaceholder(
            title =
                "Home",
            status =
                "unbound",
            modifier =
                Modifier.testTag(
                    VexLifeWorkspaceBlueprint
                        .SettingsHome
                        .ref,
                ),
        )
        SettingsPlaceholder(
            title =
                "Continuity",
            status =
                "unbound",
            modifier =
                Modifier.testTag(
                    VexLifeWorkspaceBlueprint
                        .SettingsContinuity
                        .ref,
                ),
        )
        SettingsPlaceholder(
            title =
                "Reset",
            status =
                "not connected",
            modifier =
                Modifier.testTag(
                    VexLifeWorkspaceBlueprint
                        .SettingsReset
                        .ref,
                ),
        )
    }
}

@Composable
private fun SettingsPlaceholder(
    title: String,
    status: String,
    modifier: Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width =
                        1.dp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .outlineVariant,
                    shape =
                        RoundedCornerShape(
                            8.dp,
                        ),
                )
                .padding(
                    12.dp,
                ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Text(
            title,
        )
        Spacer(
            Modifier.weight(
                1f,
            ),
        )
        Text(
            status,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )
    }
}

// [VXG RealForever]
