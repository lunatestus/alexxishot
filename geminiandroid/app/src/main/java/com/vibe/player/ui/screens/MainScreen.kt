package com.vibe.player.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import com.vibe.player.data.ApiClient
import com.vibe.player.data.FileItem
import com.vibe.player.ui.components.MediaCard
import com.vibe.player.ui.components.Sidebar
import com.vibe.player.ui.theme.*
import com.vibe.player.util.AppUpdater
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf("/media") }
    var history by remember { mutableStateOf(listOf<String>()) }
    var items by remember { mutableStateOf(emptyList<FileItem>()) }
    var isListView by remember { mutableStateOf(true) }
    var playingItem by remember { mutableStateOf<FileItem?>(null) }
    var isSidebarFocused by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shouldRequestContentFocus by remember { mutableStateOf(true) }
    val updateInProgress = remember { mutableStateOf(false) }
    val updateStatus = remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val bottomRowIndices by remember {
        derivedStateOf {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            val maxOffsetY = visibleItems.maxOfOrNull { it.offset.y } ?: 0
            visibleItems.asSequence()
                .filter { it.offset.y == maxOffsetY }
                .map { it.index }
                .toSet()
        }
    }

    // Focus Requesters
    val sidebarFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }

    fun loadPath(path: String) {
        currentPath = path
        isLoading = true
        errorMessage = null
        shouldRequestContentFocus = true
        coroutineScope.launch {
            items = ApiClient.fetchFolder(path)
            errorMessage = ApiClient.lastError
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPath("/media")
    }

    LaunchedEffect(isLoading, shouldRequestContentFocus) {
        if (!isLoading && !isSidebarFocused && items.isNotEmpty() && shouldRequestContentFocus) {
            kotlinx.coroutines.delay(100)
            firstItemFocusRequester.requestFocus()
            shouldRequestContentFocus = false
        }
    }


    BackHandler(enabled = history.isNotEmpty() || playingItem != null) {
        if (playingItem != null) {
            playingItem = null
        } else if (history.isNotEmpty()) {
            val prev = history.last()
            history = history.dropLast(1)
            loadPath(prev)
        }
    }

    val contentAlpha by animateFloatAsState(targetValue = if (isSidebarFocused) 0.5f else 1f, animationSpec = tween(200))
    // Expanded width (180) - Collapsed width (56) = 124 shift
    val contentTranslation by animateFloatAsState(targetValue = if (isSidebarFocused) 124f else 0f, animationSpec = tween(200))

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 56.dp)
                .graphicsLayer {
                    translationX = contentTranslation
                    alpha = contentAlpha
                }
                .padding(top = 24.dp, start = 24.dp, bottom = 16.dp, end = 24.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                }
            } else if (errorMessage != null && errorMessage != "Empty folder") {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        "Error: $errorMessage",
                        color = TextColor,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp)
                    )
                    Button(
                        onClick = { loadPath(currentPath) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBg,
                            contentColor = TextColor
                        ),
                        border = BorderStroke(1.dp, ViewToggleBorder)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            } else if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files found", color = TextColor, fontSize = 18.sp, fontFamily = DmSans)
                }
            } else if (isListView) {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                    itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
                        val isLastItem = index == items.lastIndex
                        val cardModifier = (if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                            .then(
                                if (isLastItem) {
                                    Modifier.focusProperties { down = FocusRequester.Cancel }
                                } else {
                                    Modifier
                                }
                            )
                        MediaCard(
                            item = item, 
                            index = index, 
                            isListView = true, 
                            modifier = cardModifier,
                            onClick = {
                                if (item.type == "folder") { history = history + currentPath; loadPath(item.path) }
                                else { playingItem = item }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 180.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                    itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
                        val isLastRow = bottomRowIndices.contains(index)
                        val cardModifier = (if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                            .then(
                                if (isLastRow) {
                                    Modifier.focusProperties { down = FocusRequester.Cancel }
                                } else {
                                    Modifier
                                }
                            )
                        MediaCard(
                            item = item, 
                            index = index, 
                            isListView = false, 
                            modifier = cardModifier,
                            onClick = {
                                if (item.type == "folder") { history = history + currentPath; loadPath(item.path) }
                                else { playingItem = item }
                            }
                        )
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

        if (playingItem != null) {
            PlayerScreen(
                item = playingItem!!, 
                onClose = { 
                    playingItem = null
                    shouldRequestContentFocus = true
                }
            )
        }
    }
}
