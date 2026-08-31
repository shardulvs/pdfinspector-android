package SVS.pdfinspector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowBackUp
import compose.icons.tablericons.ArrowForwardUp
import compose.icons.tablericons.AspectRatio
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.ChevronRight
import compose.icons.tablericons.Copy
import compose.icons.tablericons.DeviceFloppy
import compose.icons.tablericons.Folder
import compose.icons.tablericons.LayoutSidebarRight
import compose.icons.tablericons.Maximize
import compose.icons.tablericons.Minimize
import compose.icons.tablericons.Settings

@Composable
fun InspectorToolbar(
    fileName: String,
    fullscreen: Boolean,
    pageIndex: Int,
    pageCount: Int,
    dirty: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    showInspector: Boolean,
    copyText: String?,
    onCopyText: () -> Unit,
    onFitWidth: () -> Unit,
    onFitHeight: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleInspector: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageJumpOpen by remember { mutableStateOf(false) }
    var pageJumpInput by remember(pageIndex, pageCount) { mutableStateOf((pageIndex + 1).toString()) }
    var pageJumpError by remember { mutableStateOf("") }
    val pageLabel = pageLabel(pageIndex, pageCount)

    if (pageJumpOpen) {
        AlertDialog(
            onDismissRequest = { pageJumpOpen = false },
            title = { Text("Go to page") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pageJumpInput,
                        onValueChange = { 
                            pageJumpInput = it.filter(Char::isDigit)
                            pageJumpError = ""
                        },
                        singleLine = true,
                        label = { Text("Page number") },
                        placeholder = { Text("1") },
                        isError = pageJumpError.isNotEmpty(),
                    )
                    if (pageJumpError.isNotEmpty()) {
                        Text(
                            text = pageJumpError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp, start = 16.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pageJumpTarget(pageJumpInput, pageCount)
                        if (target != null) {
                            onJumpToPage(target)
                            pageJumpOpen = false
                            pageJumpError = ""
                        } else {
                            pageJumpError = getPageJumpError(pageJumpInput, pageCount)
                        }
                    },
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { pageJumpOpen = false }) { Text("Cancel") }
            },
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp, end = 8.dp),
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (copyText != null) {
                    IconButton(onClick = onCopyText) {
                        Icon(TablerIcons.Copy, "Copy text", Modifier.size(20.dp))
                    }
                }

                IconButton(
                    onClick = onSave,
                    enabled = dirty,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(TablerIcons.DeviceFloppy, "Save a copy", Modifier.size(20.dp))
                }
                IconButton(onClick = onOpen) {
                    Icon(TablerIcons.Folder, "Open a PDF", Modifier.size(20.dp))
                }

                ToolDivider()
                IconButton(onClick = onPrev, enabled = pageIndex > 0) {
                    Icon(TablerIcons.ChevronLeft, "Previous page", Modifier.size(20.dp))
                }
                Text(
                    text = pageLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { 
                        if (pageCount > 0) {
                            pageJumpInput = (pageIndex + 1).toString()
                            pageJumpOpen = true
                        }
                    },
                )
                IconButton(onClick = onNext, enabled = pageIndex < pageCount - 1) {
                    Icon(TablerIcons.ChevronRight, "Next page", Modifier.size(20.dp))
                }

                ToolDivider()
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(TablerIcons.ArrowBackUp, "Undo", Modifier.size(20.dp))
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(TablerIcons.ArrowForwardUp, "Redo", Modifier.size(20.dp))
                }

                ToolDivider()
                IconToggleButton(checked = showInspector, onCheckedChange = { onToggleInspector() }) {
                    Icon(TablerIcons.LayoutSidebarRight, "Toggle inspector pane", Modifier.size(20.dp))
                }
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        imageVector = if (fullscreen) TablerIcons.Minimize else TablerIcons.Maximize,
                        contentDescription = "Toggle full screen",
                        modifier = Modifier.size(20.dp),
                        tint = if (fullscreen) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FitMenuButton(onFitWidth = onFitWidth, onFitHeight = onFitHeight)
                IconButton(onClick = onSettings) {
                    Icon(TablerIcons.Settings, "Settings", Modifier.size(20.dp))
                }
            }
        }
    }
}

fun pageLabel(pageIndex: Int, pageCount: Int): String =
    if (pageCount > 0) "${pageIndex + 1} / $pageCount" else "0 / 0"

fun pageJumpTarget(rawInput: String, pageCount: Int): Int? {
    if (pageCount <= 0) return null
    if (!rawInput.matches(Regex("\\d+"))) return null
    val pageNumber = rawInput.toIntOrNull() ?: return null
    if (pageNumber <= 0) return null
    val target = pageNumber - 1
    return target.takeIf { it in 0 until pageCount }
}

fun getPageJumpError(rawInput: String, pageCount: Int): String {
    if (pageCount <= 0) return "No pages available"
    if (rawInput.isEmpty()) return "Page number is required"
    if (!rawInput.matches(Regex("\\d+"))) return "Page number must contain only digits"
    val pageNumber = rawInput.toIntOrNull() ?: return "Invalid page number"
    if (pageNumber <= 0) return "Page number must be greater than 0"
    if (pageNumber > pageCount) return "Page number must be between 1 and $pageCount"
    return "Invalid page number"
}

@Composable
private fun FitMenuButton(onFitWidth: () -> Unit, onFitHeight: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(TablerIcons.AspectRatio, "Fit page", Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Fit to width") },
                onClick = { expanded = false; onFitWidth() },
            )
            DropdownMenuItem(
                text = { Text("Fit to height") },
                onClick = { expanded = false; onFitHeight() },
            )
        }
    }
}

@Composable
private fun ToolDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(28.dp)
            .padding(horizontal = 4.dp),
    )
}
