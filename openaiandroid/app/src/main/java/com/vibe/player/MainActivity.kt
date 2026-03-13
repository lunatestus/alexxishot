package com.vibe.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VibeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = VibeColors.Bg) {
                    VibeApp()
                }
            }
        }
    }
}

private enum class FocusArea {
    Grid,
    Sidebar,
    Header,
    Player
}

private enum class ViewMode {
    Grid,
    List
}

private enum class ItemType {
    Folder,
    File
}

private data class MediaItem(
    val type: ItemType,
    val name: String,
    val path: String
)

private object VibeColors {
    val Bg = Color(0xFF121212)
    val SidebarBg = Color(0xFF1E1E1E)
    val Text = Color(0xFFFFFFFF)
    val Accent = Color(0xFF007BFF)
    val CardBg = Color(0xFF2A2A2A)
    val CardBlue = Color(0xFF0044CC)
    val MutedText = Color(0xFFB6B6B6)
}

private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_300, weight = FontWeight.W300),
    Font(R.font.space_grotesk_400, weight = FontWeight.W400),
    Font(R.font.space_grotesk_500, weight = FontWeight.W500),
    Font(R.font.space_grotesk_600, weight = FontWeight.W600),
    Font(R.font.space_grotesk_700, weight = FontWeight.W700)
)

@Composable
private fun VibeTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = VibeColors.Accent,
        background = VibeColors.Bg,
        surface = VibeColors.Bg,
        onPrimary = Color.White,
        onBackground = VibeColors.Text,
        onSurface = VibeColors.Text
    )

    val typography = MaterialTheme.typography.copy(
        displayLarge = TextStyle(fontFamily = SpaceGrotesk),
        displayMedium = TextStyle(fontFamily = SpaceGrotesk),
        displaySmall = TextStyle(fontFamily = SpaceGrotesk),
        headlineLarge = TextStyle(fontFamily = SpaceGrotesk),
        headlineMedium = TextStyle(fontFamily = SpaceGrotesk),
        headlineSmall = TextStyle(fontFamily = SpaceGrotesk),
        titleLarge = TextStyle(fontFamily = SpaceGrotesk),
        titleMedium = TextStyle(fontFamily = SpaceGrotesk),
        titleSmall = TextStyle(fontFamily = SpaceGrotesk),
        bodyLarge = TextStyle(fontFamily = SpaceGrotesk),
        bodyMedium = TextStyle(fontFamily = SpaceGrotesk),
        bodySmall = TextStyle(fontFamily = SpaceGrotesk),
        labelLarge = TextStyle(fontFamily = SpaceGrotesk),
        labelMedium = TextStyle(fontFamily = SpaceGrotesk),
        labelSmall = TextStyle(fontFamily = SpaceGrotesk)
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        content = content
    )
}

@Composable
private fun VibeApp() {
    val sidebarItems = listOf("Home", "Movies", "TV Shows", "Settings")

    val fileSystem = remember {
        mapOf(
            "/" to listOf(
                MediaItem(ItemType.Folder, "Movies", "/Movies"),
                MediaItem(ItemType.Folder, "TV Shows", "/TV Shows"),
                MediaItem(ItemType.Folder, "Music", "/Music"),
                MediaItem(ItemType.File, "Welcome Video.mp4", "/Welcome Video.mp4"),
                MediaItem(ItemType.File, "ReadMe.txt", "/ReadMe.txt")
            ),
            "/Movies" to listOf(
                MediaItem(ItemType.File, "The Matrix.mp4", "/Movies/The Matrix.mp4"),
                MediaItem(ItemType.File, "Inception.mp4", "/Movies/Inception.mp4"),
                MediaItem(ItemType.File, "Interstellar.mp4", "/Movies/Interstellar.mp4")
            ),
            "/TV Shows" to listOf(
                MediaItem(ItemType.Folder, "Breaking Bad", "/TV Shows/Breaking Bad"),
                MediaItem(ItemType.Folder, "The Wire", "/TV Shows/The Wire")
            ),
            "/TV Shows/Breaking Bad" to listOf(
                MediaItem(ItemType.File, "S01E01.mp4", "/TV Shows/Breaking Bad/S01E01.mp4"),
                MediaItem(ItemType.File, "S01E02.mp4", "/TV Shows/Breaking Bad/S01E02.mp4")
            ),
            "/TV Shows/The Wire" to listOf(
                MediaItem(ItemType.File, "S01E01.mp4", "/TV Shows/The Wire/S01E01.mp4")
            ),
            "/Music" to listOf(
                MediaItem(ItemType.File, "Song 1.mp3", "/Music/Song 1.mp3")
            )
        )
    }

    var currentPath by remember { mutableStateOf("/") }
    var items by remember { mutableStateOf(fileSystem[currentPath].orEmpty()) }
    var focusArea by remember { mutableStateOf(FocusArea.Grid) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var viewMode by remember { mutableStateOf(ViewMode.List) }
    var playerVisible by remember { mutableStateOf(false) }
    var playerFocusIndex by remember { mutableIntStateOf(1) }
    var progress by remember { mutableIntStateOf(22) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("Now Playing") }
    val history = remember { mutableStateListOf<String>() }

    val rootFocusRequester = remember { FocusRequester() }

    val loadPath: (String) -> Unit = { path ->
        currentPath = path
        items = fileSystem[path].orEmpty()
        focusedIndex = 0
        focusArea = FocusArea.Grid
    }

    val openPlayer: (MediaItem) -> Unit = { item ->
        playerVisible = true
        playerFocusIndex = 1
        focusArea = FocusArea.Player
        isPlaying = false
        currentTitle = item.name
    }

    val closePlayer: () -> Unit = {
        playerVisible = false
        focusArea = FocusArea.Grid
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeColors.Bg)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onKeyEvent false
                }

                val key = event.key

                val isEnter = key == Key.Enter || key == Key.NumPadEnter || key == Key.DirectionCenter
                val isBack = key == Key.Back || key == Key.Escape

                val gridCols = computeGridCols(maxWidth, viewMode)

                when (focusArea) {
                    FocusArea.Grid -> {
                        val row = if (gridCols > 0) focusedIndex / gridCols else 0
                        val col = if (gridCols > 0) focusedIndex % gridCols else 0

                        when (key) {
                            Key.DirectionRight -> {
                                if (col < gridCols - 1 && focusedIndex < items.lastIndex) {
                                    focusedIndex += 1
                                }
                            }
                            Key.DirectionLeft -> {
                                if (col > 0) {
                                    focusedIndex -= 1
                                } else {
                                    focusArea = FocusArea.Sidebar
                                    focusedIndex = 0
                                }
                            }
                            Key.DirectionDown -> {
                                if (focusedIndex + gridCols <= items.lastIndex) {
                                    focusedIndex += gridCols
                                }
                            }
                            Key.DirectionUp -> {
                                if (row == 0) {
                                    focusArea = FocusArea.Header
                                } else if (focusedIndex - gridCols >= 0) {
                                    focusedIndex -= gridCols
                                }
                            }
                            else -> {
                                if (isEnter) {
                                    items.getOrNull(focusedIndex)?.let { item ->
                                        if (item.type == ItemType.Folder) {
                                            history.add(currentPath)
                                            loadPath(item.path)
                                        } else {
                                            openPlayer(item)
                                        }
                                    }
                                } else if (isBack) {
                                    if (history.isNotEmpty()) {
                                        val prev = history.removeAt(history.lastIndex)
                                        loadPath(prev)
                                    }
                                }
                            }
                        }
                    }
                    FocusArea.Sidebar -> {
                        when (key) {
                            Key.DirectionDown -> if (focusedIndex < sidebarItems.lastIndex) focusedIndex += 1
                            Key.DirectionUp -> if (focusedIndex > 0) focusedIndex -= 1
                            Key.DirectionRight -> {
                                focusArea = FocusArea.Grid
                                focusedIndex = 0
                            }
                            else -> {
                                if (isEnter) {
                                    focusArea = FocusArea.Grid
                                    focusedIndex = 0
                                } else if (isBack) {
                                    focusArea = FocusArea.Grid
                                }
                            }
                        }
                    }
                    FocusArea.Header -> {
                        when (key) {
                            Key.DirectionDown, Key.DirectionRight -> {
                                focusArea = FocusArea.Grid
                            }
                            Key.DirectionLeft -> {
                                focusArea = FocusArea.Grid
                            }
                            else -> {
                                if (isEnter) {
                                    viewMode = if (viewMode == ViewMode.Grid) ViewMode.List else ViewMode.Grid
                                }
                            }
                        }
                    }
                    FocusArea.Player -> {
                        when (key) {
                            Key.DirectionRight -> {
                                if (playerFocusIndex == 0) {
                                    progress = (progress + 2).coerceAtMost(100)
                                } else if (playerFocusIndex < 4) {
                                    playerFocusIndex += 1
                                }
                            }
                            Key.DirectionLeft -> {
                                if (playerFocusIndex == 0) {
                                    progress = (progress - 2).coerceAtLeast(0)
                                } else if (playerFocusIndex > 1) {
                                    playerFocusIndex -= 1
                                }
                            }
                            Key.DirectionUp -> playerFocusIndex = 0
                            Key.DirectionDown -> if (playerFocusIndex == 0) playerFocusIndex = 1
                            else -> {
                                if (isEnter) {
                                    when (playerFocusIndex) {
                                        1 -> isPlaying = !isPlaying
                                        4 -> {
                                            // settings placeholder
                                        }
                                    }
                                } else if (isBack) {
                                    closePlayer()
                                }
                            }
                        }
                    }
                }

                true
            }
    ) {
        LaunchedEffect(Unit) {
            rootFocusRequester.requestFocus()
        }

        LaunchedEffect(items) {
            if (focusedIndex > items.lastIndex) {
                focusedIndex = 0
            }
        }

        val sidebarWidth = 250.dp
        val sidebarOffset by animateDpAsState(if (focusArea == FocusArea.Sidebar) 0.dp else -sidebarWidth, label = "sidebarOffset")
        val contentAlpha by animateFloatAsState(if (focusArea == FocusArea.Sidebar) 0.5f else 1f, label = "contentAlpha")

        val gridCols = computeGridCols(maxWidth, viewMode)

        MainContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (focusArea == FocusArea.Sidebar) sidebarWidth else 0.dp)
                .graphicsLayer { alpha = contentAlpha },
            currentPath = currentPath,
            items = items,
            viewMode = viewMode,
            gridCols = gridCols,
            focusedIndex = if (focusArea == FocusArea.Grid) focusedIndex else -1,
            headerFocused = focusArea == FocusArea.Header,
            onToggleView = { viewMode = if (viewMode == ViewMode.Grid) ViewMode.List else ViewMode.Grid },
            onSelect = { index ->
                items.getOrNull(index)?.let { item ->
                    if (item.type == ItemType.Folder) {
                        history.add(currentPath)
                        loadPath(item.path)
                    } else {
                        openPlayer(item)
                    }
                }
            }
        )

        Sidebar(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .offset(x = sidebarOffset)
                .shadow(12.dp)
                .background(VibeColors.SidebarBg),
            items = sidebarItems,
            focusedIndex = if (focusArea == FocusArea.Sidebar) focusedIndex else -1
        )

        if (playerVisible) {
            PlayerOverlay(
                modifier = Modifier.fillMaxSize(),
                title = currentTitle,
                progress = progress,
                isPlaying = isPlaying,
                focusedIndex = playerFocusIndex
            )
        }
    }
}

private fun computeGridCols(maxWidth: Dp, viewMode: ViewMode): Int {
    if (viewMode == ViewMode.List) return 1
    val cardMin = 280.dp
    val gap = 30.dp
    val cols = ((maxWidth + gap) / (cardMin + gap)).toInt()
    return if (cols < 1) 1 else cols
}

@Composable
private fun MainContent(
    modifier: Modifier,
    currentPath: String,
    items: List<MediaItem>,
    viewMode: ViewMode,
    gridCols: Int,
    focusedIndex: Int,
    headerFocused: Boolean,
    onToggleView: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = modifier.padding(start = 40.dp, top = 40.dp, end = 70.dp, bottom = 40.dp)
    ) {
        HeaderRow(
            currentPath = currentPath,
            headerFocused = headerFocused,
            viewMode = viewMode,
            onToggleView = onToggleView
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (viewMode == ViewMode.Grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCols),
                verticalArrangement = Arrangement.spacedBy(30.dp),
                horizontalArrangement = Arrangement.spacedBy(30.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { index, item ->
                    MediaCard(
                        item = item,
                        viewMode = viewMode,
                        isFocused = index == focusedIndex,
                        onClick = { onSelect(index) }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { index, item ->
                    MediaCard(
                        item = item,
                        viewMode = viewMode,
                        isFocused = index == focusedIndex,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    currentPath: String,
    headerFocused: Boolean,
    viewMode: ViewMode,
    onToggleView: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandIcon()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "MovieApp",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W700,
                    color = VibeColors.Text
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = if (currentPath == "/") "" else "/ ${currentPath.removePrefix("/")}",
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                color = VibeColors.MutedText
            )
        }

        ViewToggleButton(
            focused = headerFocused,
            viewMode = viewMode,
            onClick = onToggleView
        )
    }
}

@Composable
private fun BrandIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.55f, 0f)
            lineTo(size.width * 0.1f, size.height * 0.6f)
            lineTo(size.width * 0.42f, size.height * 0.6f)
            lineTo(size.width * 0.32f, size.height)
            lineTo(size.width * 0.9f, size.height * 0.38f)
            lineTo(size.width * 0.58f, size.height * 0.38f)
            close()
        }
        drawPath(path = path, color = VibeColors.Text, style = Fill)
    }
}

@Composable
private fun ViewToggleButton(
    focused: Boolean,
    viewMode: ViewMode,
    onClick: () -> Unit
) {
    val border = if (focused) BorderStroke(3.dp, VibeColors.Accent) else BorderStroke(2.dp, Color(0xFF2B2B2B))
    val glow = if (focused) 0.4f else 0f

    Box(
        modifier = Modifier
            .size(44.dp)
            .border(border, RoundedCornerShape(12.dp))
            .background(VibeColors.CardBg, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (focused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.dp, VibeColors.Accent.copy(alpha = glow), RoundedCornerShape(12.dp))
            )
        }

        if (viewMode == ViewMode.List) {
            ListIcon()
        } else {
            GridIcon()
        }
    }
}

@Composable
private fun ListIcon() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val barHeight = size.height / 6f
        val gap = barHeight
        repeat(3) { index ->
            val y = index * (barHeight + gap)
            drawRoundRect(
                color = VibeColors.Text,
                topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f, barHeight / 2f)
            )
        }
    }
}

@Composable
private fun GridIcon() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val cell = size.width / 2.2f
        val gap = size.width / 6f
        val positions = listOf(
            0f to 0f,
            cell + gap to 0f,
            0f to cell + gap,
            cell + gap to cell + gap
        )
        positions.forEach { (x, y) ->
            drawRoundRect(
                color = VibeColors.Text,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(cell, cell),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    viewMode: ViewMode,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val scaleTarget = if (isFocused && viewMode == ViewMode.Grid) 1.06f else 1f
    val scale by animateFloatAsState(scaleTarget, label = "cardScale")
    val border = if (isFocused) BorderStroke(3.dp, VibeColors.Accent) else BorderStroke(3.dp, Color.Transparent)
    val shadow = if (isFocused) 20.dp else 0.dp

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(shadow, shape)
            .clip(shape)
            .background(VibeColors.CardBg)
            .border(border, shape)
            .zIndex(if (isFocused) 1f else 0f)
            .clickable { onClick() }
            .then(
                if (viewMode == ViewMode.Grid) {
                    Modifier.aspectRatio(16f / 9f)
                } else {
                    Modifier
                        .height(96.dp)
                        .fillMaxWidth()
                }
            )
    ) {
        if (viewMode == ViewMode.Grid) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VibeColors.CardBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vibe",
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.W700
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xD9000000))
                            )
                        )
                        .padding(15.dp)
                ) {
                    Column {
                        Text(
                            text = item.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.type.name,
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VibeColors.CardBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vibe",
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W700
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = item.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.W600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.type.name,
                        fontSize = 14.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    modifier: Modifier,
    items: List<String>,
    focusedIndex: Int
) {
    Column(modifier = modifier.padding(top = 30.dp)) {
        Text(
            text = "Vibe Player",
            modifier = Modifier.padding(start = 20.dp, bottom = 20.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.W700,
            color = VibeColors.Text
        )
        Spacer(modifier = Modifier.height(10.dp))
        items.forEachIndexed { index, item ->
            val focused = index == focusedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (focused) VibeColors.Accent else Color.Transparent)
                    .padding(horizontal = 25.dp, vertical = 15.dp)
            ) {
                Text(
                    text = item,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.W500,
                    color = VibeColors.Text
                )
            }
        }
    }
}

@Composable
private fun PlayerOverlay(
    modifier: Modifier,
    title: String,
    progress: Int,
    isPlaying: Boolean,
    focusedIndex: Int
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xE6000000), Color(0x99000000))
                        )
                    )
                    .padding(start = 40.dp, end = 40.dp, bottom = 40.dp, top = 60.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                    PlayerProgress(progress = progress, focused = focusedIndex == 0)
                    PlayerControls(isPlaying = isPlaying, focusedIndex = focusedIndex)
                }
            }
        }
    }
}

@Composable
private fun PlayerProgress(progress: Int, focused: Boolean) {
    val trackHeight by animateDpAsState(if (focused) 10.dp else 6.dp, label = "trackHeight")
    val dotSize by animateDpAsState(if (focused) 24.dp else 14.dp, label = "dotSize")
    val dotAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "dotAlpha")

    val totalSeconds = 276
    val currentSeconds = (progress / 100f * totalSeconds).toInt()
    val minutes = currentSeconds / 60
    val seconds = currentSeconds % 60
    val currentTime = String.format("%02d:%02d", minutes, seconds)

    Column {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val dotPx = with(LocalDensity.current) { dotSize.toPx() }
            val rawX = (widthPx * (progress / 100f)) - (dotPx / 2f)
            val dotX = rawX.coerceIn(0f, widthPx - dotPx)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = if (focused) 0.4f else 0.2f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress / 100f)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(dotX.roundToInt(), 0) }
                    .align(Alignment.CenterStart)
                    .size(dotSize)
                    .graphicsLayer { alpha = dotAlpha }
                    .background(Color.White, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = currentTime,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "04:36",
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PlayerControls(isPlaying: Boolean, focusedIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerButton(
                size = 72.dp,
                focused = focusedIndex == 1,
                primary = true,
                label = if (isPlaying) "Pause" else "Play"
            ) {
                if (isPlaying) {
                    PauseIcon(40.dp)
                } else {
                    PlayIcon(40.dp)
                }
            }
            PlayerButton(
                size = 56.dp,
                focused = focusedIndex == 2,
                primary = false,
                label = "Previous"
            ) {
                RewindIcon(28.dp)
            }
            PlayerButton(
                size = 56.dp,
                focused = focusedIndex == 3,
                primary = false,
                label = "Next"
            ) {
                ForwardIcon(28.dp)
            }
        }

        PlayerPillButton(
            focused = focusedIndex == 4,
            text = "Settings"
        )
    }
}

@Composable
private fun PlayerButton(
    size: Dp,
    focused: Boolean,
    primary: Boolean,
    label: String,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, label = "playerScale")
    val bgColor = if (focused) Color.White else Color.Transparent
    val fgColor = if (focused) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(bgColor, CircleShape)
            .border(0.dp, Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(size * 0.6f), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides fgColor) {
                content()
            }
        }
    }
}

@Composable
private fun PlayerPillButton(focused: Boolean, text: String) {
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, label = "pillScale")
    val bgColor = if (focused) Color.White else Color.Transparent
    val fgColor = if (focused) Color.Black else Color.White

    Box(
        modifier = Modifier
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(bgColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.W600,
            color = fgColor
        )
    }
}

@Composable
private fun PlayIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.toPx(), size.toPx() / 2f)
            lineTo(0f, size.toPx())
            close()
        }
        drawPath(path, color = LocalContentColor.current, style = Fill)
    }
}

@Composable
private fun PauseIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val barWidth = size.toPx() * 0.25f
        val gap = size.toPx() * 0.15f
        drawRoundRect(
            color = LocalContentColor.current,
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(barWidth, size.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
        )
        drawRoundRect(
            color = LocalContentColor.current,
            topLeft = androidx.compose.ui.geometry.Offset(barWidth + gap, 0f),
            size = androidx.compose.ui.geometry.Size(barWidth, size.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

@Composable
private fun RewindIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        val path1 = Path().apply {
            moveTo(width * 0.55f, height * 0.1f)
            lineTo(0f, height * 0.5f)
            lineTo(width * 0.55f, height * 0.9f)
            close()
        }
        val path2 = Path().apply {
            moveTo(width, height * 0.1f)
            lineTo(width * 0.45f, height * 0.5f)
            lineTo(width, height * 0.9f)
            close()
        }
        drawPath(path1, color = LocalContentColor.current)
        drawPath(path2, color = LocalContentColor.current)
    }
}

@Composable
private fun ForwardIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        val path1 = Path().apply {
            moveTo(0f, height * 0.1f)
            lineTo(width * 0.55f, height * 0.5f)
            lineTo(0f, height * 0.9f)
            close()
        }
        val path2 = Path().apply {
            moveTo(width * 0.45f, height * 0.1f)
            lineTo(width, height * 0.5f)
            lineTo(width * 0.45f, height * 0.9f)
            close()
        }
        drawPath(path1, color = LocalContentColor.current)
        drawPath(path2, color = LocalContentColor.current)
    }
}
