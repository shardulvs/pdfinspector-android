package SVS.pdfinspector

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Bug
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.FileText
import compose.icons.tablericons.Folder
import compose.icons.tablericons.Heart
import compose.icons.tablericons.Settings
import SVS.pdfinspector.engine.NodeKind
import SVS.pdfinspector.engine.findNode
import SVS.pdfinspector.ui.Dock
import SVS.pdfinspector.ui.ElementEditSheet
import SVS.pdfinspector.ui.FitMode
import SVS.pdfinspector.ui.InspectorDock
import SVS.pdfinspector.ui.InspectorPane
import SVS.pdfinspector.ui.InspectorToolbar
import SVS.pdfinspector.ui.LeafRect
import SVS.pdfinspector.ui.PdfCanvas
import SVS.pdfinspector.ui.ThemeSettingsSheet
import SVS.pdfinspector.ui.theme.InspectorTheme
import SVS.pdfinspector.ui.theme.ThemeState
import SVS.pdfinspector.ui.theme.rememberThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialUri: Uri? = if (intent?.action == Intent.ACTION_VIEW) intent?.data else null
        setContent {
            val themeState = rememberThemeState()
            InspectorTheme(
                mode = themeState.mode,
                dynamicColor = themeState.dynamic,
                accent = themeState.accent,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    InspectorScreen(initialUri = initialUri, themeState = themeState)
                }
            }
        }
    }
}

@Composable
fun InspectorScreen(
    initialUri: Uri? = null,
    themeState: ThemeState,
    viewModel: PdfDocumentViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val state = viewModel.state
    var showSettings by remember { mutableStateOf(false) }
    var showInspector by remember { mutableStateOf(true) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) viewModel.open(context, initialUri)
    }

    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.open(context, it) } }
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> uri?.let { viewModel.saveCopy(context, it) } }

    fun pickPdf() = openLauncher.launch(arrayOf("application/pdf"))

    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    var dock by remember(landscape) { mutableStateOf(if (landscape) Dock.SIDE else Dock.BOTTOM) }
    var transparent by rememberSaveable { mutableStateOf(false) }
    var bottomHeight by remember { mutableStateOf(300.dp) }
    var sideWidth by remember { mutableStateOf(340.dp) }
    val density = LocalDensity.current
    val sizeDp = if (dock == Dock.BOTTOM) bottomHeight else sideWidth
    val onResize: (Float) -> Unit = { delta ->
        if (dock == Dock.BOTTOM) {
            bottomHeight = with(density) { (bottomHeight.toPx() - delta).toDp() }.coerceIn(140.dp, 600.dp)
        } else {
            sideWidth = with(density) { (sideWidth.toPx() - delta).toDp() }.coerceIn(240.dp, 600.dp)
        }
    }

    var fullscreen by remember { mutableStateOf(false) }
    var fitMode by remember { mutableStateOf(FitMode.WIDTH) }
    LaunchedEffect(state.documentToken) { fitMode = FitMode.WIDTH }

    LaunchedEffect(fullscreen) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (fullscreen) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(enabled = state.hasDocument) {
        if (state.busy != null) return@BackHandler
        fullscreen = false
        viewModel.closeDocument()
    }

    val copyText = state.page
        ?.let { findNode(it.root, state.selectedId) }
        ?.takeIf { it.kind == NodeKind.TEXT }
        ?.text

    if (state.hasDocument) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    InspectorToolbar(
                        fileName = state.fileName,
                        fullscreen = fullscreen,
                        pageIndex = state.pageIndex,
                        pageCount = state.pageCount,
                        dirty = state.dirty,
                        canUndo = state.canUndo,
                        canRedo = state.canRedo,
                        showInspector = showInspector,
                        copyText = copyText,
                        onCopyText = { viewModel.copySelectedText(context) },
                        onFitWidth = { fitMode = FitMode.WIDTH },
                        onFitHeight = { fitMode = FitMode.HEIGHT },
                        onToggleFullscreen = { fullscreen = !fullscreen },
                        onToggleInspector = { showInspector = !showInspector },
                        onPrev = { viewModel.showPage(state.pageIndex - 1) },
                        onNext = { viewModel.showPage(state.pageIndex + 1) },
                        onJumpToPage = { index -> viewModel.showPage(index) },
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onOpen = { pickPdf() },
                        onSave = { saveLauncher.launch("inspected.pdf") },
                        onSettings = { showSettings = true },
                    )
                },
            ) { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                ) {
                    Workspace(
                        viewModel = viewModel,
                        state = state,
                        dock = dock,
                        transparent = transparent,
                        sizeDp = sizeDp,
                        onResize = onResize,
                        fitMode = fitMode,
                        onUserTransform = { fitMode = FitMode.NONE },
                        onToggleDock = { dock = if (dock == Dock.BOTTOM) Dock.SIDE else Dock.BOTTOM },
                        onToggleTransparent = {
                            transparent = !transparent
                            fitMode = FitMode.NONE
                        },
                        showInspector = showInspector,
                    )
                }
            }
            state.busy?.let { BusyOverlay(it) }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CircularProgressIndicator()
                    state.error != null -> Text("Error: ${state.error}")
                    else -> EmptyState(onOpen = ::pickPdf)
                }
            }
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(8.dp),
            ) {
                Icon(TablerIcons.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp))
            }
        }
    }

    if (showSettings) {
        ThemeSettingsSheet(theme = themeState, onDismiss = { showSettings = false })
    }

    if (state.editingId != null) {
        val target = remember(state.editingId, state.page, state.fontCatalogTick) {
            viewModel.editTarget()
        }
        if (target != null) {
            ElementEditSheet(
                target = target,
                onApply = { req -> viewModel.applyEdit(context, req) },
                onImportFont = { uri -> viewModel.importFont(context, uri) },
                onDismiss = { viewModel.cancelEdit() },
            )
        }
    }
}

@Composable
private fun Workspace(
    viewModel: PdfDocumentViewModel,
    state: PdfUiState,
    dock: Dock,
    transparent: Boolean,
    sizeDp: Dp,
    onResize: (Float) -> Unit,
    fitMode: FitMode,
    onUserTransform: () -> Unit,
    onToggleDock: () -> Unit,
    onToggleTransparent: () -> Unit,
    showInspector: Boolean,
) {
    val page = state.page ?: return
    val transform = state.pageTransform

    val context = LocalContext.current
    val leafRects = remember(page, transform) {
        if (transform != null) {
            page.leaves.mapNotNull { n -> n.bounds?.let { LeafRect(n.id, transform.toRect(it)) } }
        } else {
            emptyList()
        }
    }
    // Every Tj/TJ run inside a BT..ET, boxed so it can be tapped and retyped.
    val runBoxes = remember(page, transform) {
        if (transform == null) {
            emptyList()
        } else {
            page.leaves
                .asSequence()
                .filter { it.kind == NodeKind.TEXT }
                .flatMap { it.children.asSequence() }
                .filter { it.kind == NodeKind.TEXT && !it.text.isNullOrBlank() && it.bounds != null }
                .map { LeafRect(it.id, transform.toRect(it.bounds!!)) }
                .toList()
        }
    }
    val selectedRect = remember(page, transform, state.selectedId) {
        val node = findNode(page.root, state.selectedId)
        val bounds = node?.bounds
        if (bounds != null && transform != null) transform.toRect(bounds) else null
    }

    val highlight = MaterialTheme.colorScheme.primary
    val backdrop = MaterialTheme.colorScheme.surfaceVariant
    val runBoxColor = MaterialTheme.colorScheme.outline
    var editingRunId by remember(page) { mutableStateOf<Int?>(null) }
    val bmp = state.bitmap

    // Held here, not inside PdfCanvas, so zoom/pan persist when the canvas moves
    // between the docked and transparent layouts.
    val scaleState = remember { mutableStateOf(1f) }
    val offsetState = remember { mutableStateOf(Offset.Zero) }

    // Last font-match decision, kept for the debug overlay; refreshed on each tap.
    var fontDebug by remember(page) { mutableStateOf<FontCatalog.MatchExplain?>(null) }
    LaunchedEffect(editingRunId, state.fontCatalogTick) {
        editingRunId?.let { fontDebug = viewModel.fontDecisionFor(it) }
    }
    val fontDebugRows: List<Pair<String, String>> = fontDebug?.let { e ->
        buildList {
            add("Font" to e.original)
            add("Step" to e.step + if (e.confident) "" else " (soft)")
            add("Pick" to (e.match ?: "-"))
            e.detail?.let { add("Width" to it) }
        }
    } ?: emptyList()

    val canvas: @Composable (Modifier) -> Unit = { mod ->
        if (bmp != null) {
            Box(mod) {
                PdfCanvas(
                    bitmap = bmp,
                    pageIndex = state.pageIndex,
                    scaleState = scaleState,
                    offsetState = offsetState,
                    leaves = leafRects,
                    selectedRect = selectedRect,
                    highlightColor = highlight,
                    backdropColor = backdrop,
                    runBoxes = runBoxes,
                    editingRunId = editingRunId,
                    textBoxColor = runBoxColor,
                    onEditRun = { id -> editingRunId = id },
                    debugRows = fontDebugRows,
                    fitMode = fitMode,
                    onUserTransform = onUserTransform,
                    onSelect = { id -> viewModel.select(id, reveal = true) },
                    renderTile = { idx, src, outW, outH ->
                        viewModel.renderRegion(idx, src.left, src.top, src.right, src.bottom, outW, outH)
                            ?.asImageBitmap()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                val er = editingRunId
                val run = if (er != null && transform != null) findNode(page.root, er) else null
                val rect = run?.bounds?.let { transform?.toRect(it) }
                if (er != null && run != null && rect != null) {
                    val fontFamily = remember(er) {
                        when (val face = viewModel.inlineFontFace(er)) {
                            is FontCatalog.FaceSource.Asset ->
                                FontFamily(Font(path = face.path, assetManager = context.assets))
                            is FontCatalog.FaceSource.FileFace -> FontFamily(Font(file = face.file))
                            null -> null
                        }
                    }
                    val fontScale = remember(er, state.fontCatalogTick) {
                        viewModel.inlineFontScale(er)
                    }
                    InlineTextEditor(
                        runId = er,
                        rect = rect,
                        scale = scaleState.value,
                        offset = offsetState.value,
                        initial = run.text ?: "",
                        textColor = run.colorArgb?.let { Color(it) } ?: Color.Black,
                        fontFamily = fontFamily,
                        fontScale = fontScale,
                        onCommit = { newText ->
                            editingRunId = null
                            viewModel.applyInlineText(context, er, newText)
                        },
                    )
                }
            }
        } else {
            Box(mod, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
    val pane: @Composable () -> Unit = {
        InspectorPane(
            page = page,
            expanded = state.expanded,
            selectedId = state.selectedId,
            swatchColors = state.swatchColors,
            revealTick = state.revealTick,
            showRaw = state.showRaw,
            canDelete = state.selectedId != null,
            dock = dock,
            transparent = transparent,
            onSelect = { id -> viewModel.select(id) },
            onToggleExpand = { id -> viewModel.toggleExpand(id) },
            onToggleRaw = { viewModel.toggleRaw() },
            onToggleDock = onToggleDock,
            onToggleTransparent = onToggleTransparent,
            onDelete = { viewModel.deleteSelected() },
            onEdit = { id -> viewModel.beginEdit(id) },
        )
    }

    if (transparent) {
        Box(Modifier.fillMaxSize()) {
            canvas(Modifier.fillMaxSize())
            if (showInspector) {
                InspectorDock(
                    dock = dock,
                    transparent = true,
                    sizeDp = sizeDp,
                    onResizePx = onResize,
                    modifier = Modifier
                        .align(if (dock == Dock.SIDE) Alignment.CenterEnd else Alignment.BottomCenter)
                        .then(if (dock == Dock.SIDE) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()),
                ) { pane() }
            }
        }
    } else if (dock == Dock.SIDE) {
        Row(Modifier.fillMaxSize()) {
            canvas(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            if (showInspector) {
                InspectorDock(dock, false, sizeDp, onResize, Modifier.fillMaxHeight()) { pane() }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            canvas(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            if (showInspector) {
                InspectorDock(dock, false, sizeDp, onResize, Modifier.fillMaxWidth()) { pane() }
            }
        }
    }
}

// A text field pinned over the tapped run. Position and size follow the live
// zoom/pan so it sits exactly on the glyphs; committing hands the new string to
// the same run rewrite the edit sheet uses. The scrim commits on a tap away.
@Composable
private fun InlineTextEditor(
    runId: Int,
    rect: Rect,
    scale: Float,
    offset: Offset,
    initial: String,
    textColor: Color,
    fontFamily: FontFamily?,
    fontScale: Float,
    onCommit: (String) -> Unit,
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    var value by remember(runId) {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    val left = rect.left * scale + offset.x
    val top = rect.top * scale + offset.y
    val widthPx = (rect.width * scale).coerceAtLeast(32f)
    val heightPx = (rect.height * scale).coerceAtLeast(22f)
    // The run box is the text's em (ascent 0.75 to descent -0.25), so the font
    // fills it and the baseline rests 75% down. Bottom aligning a naturally
    // sized field lands the glyphs on that line with no top padding or clipping.
    val fontPx = (rect.height * scale * fontScale).coerceAtLeast(14f)
    val fontSp = with(density) { fontPx.toSp() }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(runId) { detectTapGestures { onCommit(value.text) } },
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
                .background(Color.White)
                .border(1.dp, textColor.copy(alpha = 0.6f)),
        ) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = fontSp,
                    fontFamily = fontFamily,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                cursorBrush = SolidColor(textColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCommit(value.text) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                    .focusRequester(focusRequester),
            )
        }
    }
    LaunchedEffect(runId) { focusRequester.requestFocus() }
}

private const val FEATURE_URL = "https://github.com/shardulvs/pdfinspector-android/issues/new?labels=enhancement"
private const val BUG_URL = "https://github.com/shardulvs/pdfinspector-android/issues/new?labels=bug"
private const val SPONSOR_URL = "https://github.com/sponsors/shardulvs"

@Composable
private fun EmptyState(onOpen: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp),
    ) {
        Icon(
            imageVector = TablerIcons.FileText,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("Inspect any PDF", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Open a document to explore its elements, select them on the page or in the tree, then move, resize, recolor, edit text or delete them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onOpen) {
            Icon(TablerIcons.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open a PDF")
        }
        Spacer(Modifier.height(40.dp))
        Text(
            "Help make PdfInspector better",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { uriHandler.openUri(FEATURE_URL) },
                label = { Text("Feature") },
                leadingIcon = {
                    Icon(TablerIcons.Bulb, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            AssistChip(
                onClick = { uriHandler.openUri(BUG_URL) },
                label = { Text("Bug") },
                leadingIcon = {
                    Icon(TablerIcons.Bug, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            AssistChip(
                onClick = { uriHandler.openUri(SPONSOR_URL) },
                label = { Text("Sponsor") },
                leadingIcon = {
                    Icon(TablerIcons.Heart, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}

@Composable
private fun BusyOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.width(14.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
