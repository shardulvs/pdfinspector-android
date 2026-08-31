package SVS.pdfinspector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Code
import compose.icons.tablericons.Droplet
import compose.icons.tablericons.Edit
import compose.icons.tablericons.LayoutBottombar
import compose.icons.tablericons.LayoutSidebarRight
import compose.icons.tablericons.Search
import compose.icons.tablericons.Trash
import SVS.pdfinspector.engine.DrawNode
import SVS.pdfinspector.engine.NodeKind
import SVS.pdfinspector.engine.ParsedPage

private class TreeRow(
    val node: DrawNode,
    val depth: Int,
    val hasChildren: Boolean,
    val expanded: Boolean,
)

@Composable
fun InspectorPane(
    page: ParsedPage,
    expanded: Set<Int>,
    selectedId: Int?,
    swatchColors: Map<Int, Int>,
    revealTick: Int,
    showRaw: Boolean,
    canDelete: Boolean,
    dock: Dock,
    transparent: Boolean,
    onSelect: (Int) -> Unit,
    onToggleExpand: (Int) -> Unit,
    onToggleRaw: () -> Unit,
    onToggleDock: () -> Unit,
    onToggleTransparent: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchField by rememberSaveable { mutableStateOf(false) }
    Column(modifier
        .fillMaxWidth()
        .onKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.isCtrlPressed && keyEvent.key == Key.F) {
                showSearchField = !showSearchField
                true
            } else {
                false
            }
        }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Inspector",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${page.leaves.size} elements",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconToggleButton(checked = showRaw, onCheckedChange = { onToggleRaw() }) {
                Icon(TablerIcons.Code, "Toggle raw operators", Modifier.size(20.dp))
            }
            IconToggleButton(checked = showSearchField, onCheckedChange = { showSearchField = it }) {
                Icon(TablerIcons.Search, "Toggle search", Modifier.size(20.dp))
            }
            IconButton(onClick = onToggleDock) {
                Icon(
                    imageVector = if (dock == Dock.BOTTOM) TablerIcons.LayoutSidebarRight else TablerIcons.LayoutBottombar,
                    contentDescription = "Dock side or bottom",
                    modifier = Modifier.size(20.dp),
                )
            }
            IconToggleButton(checked = transparent, onCheckedChange = { onToggleTransparent() }) {
                Icon(TablerIcons.Droplet, "Toggle transparency", Modifier.size(20.dp))
            }
            FilledTonalIconButton(
                onClick = onDelete,
                enabled = canDelete,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(TablerIcons.Trash, "Delete element", Modifier.size(20.dp))
            }
        }
        HorizontalDivider()
        if (showSearchField) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Search elements") },
            )
            HorizontalDivider()
        }

        val rows = remember(page, expanded, searchQuery, showSearchField) { 
            flatten(page.root, expanded, if (showSearchField) searchQuery else "") 
        }
        val listState = rememberLazyListState()
        LaunchedEffect(revealTick) {
            if (selectedId != null) {
                val index = rows.indexOfFirst { it.node.id == selectedId }
                if (index >= 0) listState.animateScrollToItem(index)
            }
        }
        LazyColumn(state = listState, modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            items(rows, key = { it.node.id }) { row ->
                val swatch = swatchColors[row.node.id] ?: row.node.colorArgb
                TreeRowItem(row, row.node.id == selectedId, showRaw, swatch, onSelect, onToggleExpand, onEdit)
            }
        }
    }
}

private fun flatten(root: DrawNode, expanded: Set<Int>, query: String = ""): List<TreeRow> {
    val out = ArrayList<TreeRow>()
    val normalizedQuery = query.trim()

    fun matchesQuery(node: DrawNode): Boolean {
        if (normalizedQuery.isEmpty()) return true
        return listOfNotNull(node.label, node.detail, node.raw, node.text)
            .any { it.contains(normalizedQuery, ignoreCase = true) }
    }

    fun hasMatchingDescendant(node: DrawNode): Boolean {
        if (node.children.isEmpty()) return matchesQuery(node)
        return node.children.any { matchesQuery(it) || hasMatchingDescendant(it) }
    }

    fun walk(node: DrawNode, depth: Int) {
        for (child in node.children) {
            val hasChildren = child.children.isNotEmpty()
            val childMatches = matchesQuery(child)
            val descendantMatches = hasMatchingDescendant(child)
            val visible = normalizedQuery.isEmpty() || childMatches || descendantMatches
            if (!visible) continue

            val isOpen = child.id in expanded || (normalizedQuery.isNotEmpty() && descendantMatches)
            out.add(TreeRow(child, depth, hasChildren, isOpen))
            if (hasChildren && isOpen) walk(child, depth + 1)
        }
    }
    walk(root, 0)
    return out
}

@Composable
private fun TreeRowItem(
    row: TreeRow,
    selected: Boolean,
    showRaw: Boolean,
    swatchColor: Int?,
    onSelect: (Int) -> Unit,
    onToggleExpand: (Int) -> Unit,
    onEdit: (Int) -> Unit,
) {
    val node = row.node
    val background =
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable { onSelect(node.id) }
            .padding(start = (8 + row.depth * 16).dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            if (row.hasChildren) {
                Text(
                    text = if (row.expanded) "▾" else "▸",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onToggleExpand(node.id) },
                )
            }
        }
        KindBadge(node.kind)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = node.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val secondary = if (showRaw) node.raw else node.detail
            if (secondary.isNotEmpty()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (showRaw) FontFamily.Monospace else FontFamily.Default,
                    maxLines = if (showRaw) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        swatchColor?.let { argb ->
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(argb)),
            )
        }
        if (selected && node.kind != NodeKind.GROUP) {
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { onEdit(node.id) }, modifier = Modifier.size(28.dp)) {
                Icon(TablerIcons.Edit, "Edit element", Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun KindBadge(kind: NodeKind) {
    val scheme = MaterialTheme.colorScheme
    val (label, color) = when (kind) {
        NodeKind.GROUP -> "{ }" to scheme.outline
        NodeKind.TEXT -> "T" to scheme.primary
        NodeKind.PATH -> "◑" to scheme.tertiary
        NodeKind.IMAGE -> "▣" to scheme.secondary
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
