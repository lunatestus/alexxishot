package com.vibe.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import android.view.KeyEvent
import com.vibe.player.data.ApiClient
import com.vibe.player.data.FileItem
import com.vibe.player.ui.components.MediaCard
import com.vibe.player.ui.components.Sidebar
import com.vibe.player.ui.theme.*
import com.vibe.player.util.AppUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf("/media") }
    var history by remember { mutableStateOf(listOf<String>()) }
    var items by remember { mutableStateOf(emptyList<FileItem>()) }
    var isListView by remember { mutableStateOf(true) }
    var activeItem by remember { mutableStateOf<FileItem?>(null) }
    var isPlayerVisible by remember { mutableStateOf(false) }
    var isSidebarFocused by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shouldRequestContentFocus by remember { mutableStateOf(true) }
    var focusedItemPath by remember { mutableStateOf<String?>(null) } // Only used for initial load tracking now
    val updateInProgress = remember { mutableStateOf(false) }
    val updateStatus = remember { mutableStateOf<String?>(null) }
    var loadRequestId by remember { mutableIntStateOf(0) }
    var loadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Focus Requesters
    val sidebarFocusRequester = remember { FocusRequester() }
    val gridFirstItemFocusRequester = remember { FocusRequester() }

    fun loadPath(path: String) {
        currentPath = path
        isLoading = true
        errorMessage = null
        shouldRequestContentFocus = true
        focusedItemPath = null
        loadRequestId += 1
        val requestId = loadRequestId
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            val result = ApiClient.fetchFolder(path)
            if (requestId != loadRequestId) return@launch
            items = result.items
            errorMessage = result.error
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPath("/media")
    }

    LaunchedEffect(isLoading, shouldRequestContentFocus, items.size) {
        if (!isLoading && !isSidebarFocused && items.isNotEmpty() && shouldRequestContentFocus) {
            delay(100)
            gridFirstItemFocusRequester.requestFocus()
            shouldRequestContentFocus = false
        }
    }

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            if (focusedItemPath == null || items.none { it.path == focusedItemPath }) {
                focusedItemPath = items.first().path
            }
        } else {
            focusedItemPath = null
        }
    }

    LaunchedEffect(isListView) {
        // no-op
    }


    BackHandler(enabled = history.isNotEmpty() || isPlayerVisible) {
        if (isPlayerVisible) {
            isPlayerVisible = false
            shouldRequestContentFocus = true
        } else if (history.isNotEmpty()) {
            val prev = history.last()
            history = history.dropLast(1)
            loadPath(prev)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        SidebarScaffold(
            sidebarExpanded = isSidebarFocused,
            modifier = Modifier.fillMaxSize(),
            collapsedSidebarWidth = 56.dp,
            expandedSidebarWidth = 200.dp,
            dimAlpha = 0.5f
        ) { contentModifier ->
            // Content Area
            Column(
                modifier = contentModifier
                    .padding(top = 24.dp, start = 24.dp, bottom = 16.dp, end = 24.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                    }
                } else if (errorMessage != null && errorMessage != "Empty folder") {
                    val friendlyMessage = when {
                        errorMessage!!.startsWith("Tunnel starting") -> "Tunnel is starting. Please wait…"
                        errorMessage!!.startsWith("Tunnel") -> "Backend is offline. Start it and try again."
                        errorMessage!!.startsWith("Socket") -> "Network issue. Check connection and retry."
                        errorMessage!!.startsWith("API HTTP") -> "Server error. Try again."
                        errorMessage!!.startsWith("API error") -> "Server error. Try again."
                        else -> "Something went wrong. Try again."
                    }
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            friendlyMessage,
                            color = TextColor,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                        Button(
                            onClick = {
                                ApiClient.resetBaseUrl()
                                loadPath(currentPath)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = TextColor
                            ),
                            border = BorderStroke(1.dp, ViewToggleBorder),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reload")
                        }
                    }
                } else if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No files found", color = TextColor, fontSize = 18.sp, fontFamily = DmSans)
                    }
                } else if (isListView) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 40.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
                            val itemKey = item.path
                            val focusRequester = remember(itemKey) { FocusRequester() }
                            val cardModifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        // No longer tracking focusedItemKey to avoid whole-screen recompositions
                                        focusedItemPath = itemKey
                                    }
                                }
                            MediaCard(
                                item = item,
                                index = index,
                                isListView = true,
                                modifier = cardModifier,
                                onClick = {
                                    focusRequester?.requestFocus()
                                    if (item.type == "folder") { history = history + currentPath; loadPath(item.path) }
                                    else {
                                        activeItem = item
                                        isPlayerVisible = true
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 180.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                        itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
                            val itemKey = item.path
                            val focusRequester = remember(itemKey) { FocusRequester() }
                            val cardModifier = Modifier
                                .then(if (index == 0) Modifier.focusRequester(gridFirstItemFocusRequester) else Modifier)
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        focusedItemPath = itemKey
                                    }
                                }
                            MediaCard(
                                item = item,
                                index = index,
                                isListView = false,
                                modifier = cardModifier,
                                onClick = {
                                    if (item.type == "folder") { history = history + currentPath; loadPath(item.path) }
                                    else {
                                        activeItem = item
                                        isPlayerVisible = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sidebar
        Sidebar(
            isExpanded = isSidebarFocused,
            focusRequester = sidebarFocusRequester,
            isListView = isListView,
            onToggleView = {
                isListView = !isListView
                shouldRequestContentFocus = true
            },
            updateStatus = updateStatus,
            updateInProgress = updateInProgress,
            onFocusChange = { focused ->
                if (isSidebarFocused != focused) {
                    isSidebarFocused = focused
                }
                if (!focused) {
                    shouldRequestContentFocus = true
                }
            },
            onNavClick = { id ->
                if (id == "home" || id == "movies") {
                    loadPath("/media")
                } else if (id == "update") {
                    if (updateInProgress.value) {
                        updateStatus.value = "Update already in progress"
                    } else {
                        updateInProgress.value = true
                        updateStatus.value = "Preparing update…"
                        coroutineScope.launch {
                            val cacheBustedUrl =
                                "https://github.com/lunatestus/alexxishot/releases/download/latest-dev/app-debug.apk?t=${System.currentTimeMillis()}"
                            val result = AppUpdater.downloadAndInstall(
                                context,
                                cacheBustedUrl,
                                onProgress = { bytesRead, totalBytes ->
                                    val downloadedMb = bytesRead / (1024f * 1024f)
                                    val totalMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else null
                                    updateStatus.value = if (totalMb != null) {
                                        "Downloading %.1f / %.1f MB".format(downloadedMb, totalMb)
                                    } else {
                                        "Downloading %.1f MB".format(downloadedMb)
                                    }
                                },
                                onStatus = { status ->
                                    updateStatus.value = status
                                }
                            )
                            updateInProgress.value = false
                            if (result.isFailure) {
                                updateStatus.value = "Update failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                            }
                        }
                    }
                }
            }
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = isPlayerVisible,
            enter = androidx.compose.animation.fadeIn(animationSpec = tween(300)),
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize().zIndex(2f)
        ) {
            if (activeItem != null) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    PlayerScreen(
                        item = activeItem!!,
                        onClose = {
                            isPlayerVisible = false
                            shouldRequestContentFocus = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarScaffold(
    sidebarExpanded: Boolean,
    modifier: Modifier = Modifier,
    collapsedSidebarWidth: Dp,
    expandedSidebarWidth: Dp,
    dimAlpha: Float,
    content: @Composable (Modifier) -> Unit
) {
    val density = LocalDensity.current
    val shiftPx = remember(collapsedSidebarWidth, expandedSidebarWidth, density) {
        with(density) { (expandedSidebarWidth - collapsedSidebarWidth).toPx() }
    }
    val progress = remember { Animatable(if (sidebarExpanded) 1f else 0f) }

    LaunchedEffect(sidebarExpanded) {
        progress.animateTo(
            targetValue = if (sidebarExpanded) 1f else 0f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier) {
        // Keep the heavy content fully opaque; dim using a cheap scrim overlay.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = collapsedSidebarWidth)
                .graphicsLayer {
                    translationX = shiftPx * progress.value
                }
        ) {
            content(Modifier.fillMaxSize())
        }

        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = dimAlpha * progress.value))
            )
        }
    }
}
