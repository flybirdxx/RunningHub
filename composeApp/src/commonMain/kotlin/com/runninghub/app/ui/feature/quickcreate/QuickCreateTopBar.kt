package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.runninghub.app.ui.designsystem.components.navigation.RhTopBar
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.feature.quickcreate.presentation.project.QuickCreateCreateProjectAction
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateNavigationLabel
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_top_bar_back_content_description
import runninghub.composeapp.generated.resources.quick_create_top_bar_menu_content_description

@Composable
internal fun QuickCreateTopBar(
    onBack: (() -> Unit)?,
    onCreateProject: (String) -> Unit,
) {
    RhTopBar(
        title = quickCreateNavigationText(QuickCreateNavigationLabel.CreationMode),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            Res.string.quick_create_top_bar_back_content_description,
                        ),
                        tint = RhTheme.colors.textSecondary,
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = stringResource(
                            Res.string.quick_create_top_bar_menu_content_description,
                        ),
                        tint = RhTheme.colors.textSecondary,
                    )
                }
            }
        },
        actions = { QuickCreateCreateProjectAction(onCreateProject = onCreateProject) },
    )
}
