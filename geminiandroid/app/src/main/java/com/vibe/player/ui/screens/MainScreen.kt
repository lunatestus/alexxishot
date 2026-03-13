package com.vibe.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
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
import android.widget.Toast
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
    var updateInProgress by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "MovieApp", color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = DmSans)
                    Spacer(modifier = Modifier.width(14.dp))
                    if (currentPath != "/") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "/ ${currentPath.removePrefix("/")}",
                                color = BreadcrumbColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = DmSans
                            )
                        }
                    }
                }

                var isToggleFocused by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (updateInProgress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = TextColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Updating…",
                                color = BreadcrumbColor,
                                fontSize = 12.sp,
                                fontFamily = DmSans
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                        .background(CardBg)
                        .border(width = if (isToggleFocused) 2.dp else 1.dp, color = if (isToggleFocused) AccentColor else ViewToggleBorder, shape = RoundedCornerShape(10.dp))
                        .focusProperties {
                            down = if (items.isNotEmpty()) firstItemFocusRequester else FocusRequester.Default
                            left = FocusRequester.Default
                        }
                        .onFocusChanged { isToggleFocused = it.isFocused }
                        .focusable()
                        .clickable { 
                            isListView = !isListView
                            shouldRequestContentFocus = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = if (isListView) PlayerIcons.LayoutGrid else PlayerIcons.LayoutList, contentDescription = "Toggle View", tint = TextColor, modifier = Modifier.size(16.dp))
                }
            }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                }
            }
 else if (errorMessage != null && errorMessage != "Empty folder") {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Error: $errorMessage", color = Color.Red, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
                    Button(
                        onClick = { loadPath(currentPath) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
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
                    itemsIndexed(items) { index, item ->
                        MediaCard(
                            item = item, 
                            index = index, 
                            isListView = true, 
                            modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                            onClick = {
                                if (item.type == "folder") { history = history + currentPath; loadPath(item.path) }
                                else { playingItem = item }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(minSize = 180.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                    itemsIndexed(items) { index, item ->
                        MediaCard(
                            item = item, 
                            index = index, 
                            isListView = false, 
                            modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
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
            onFocusChange = { focused ->
                isSidebarFocused = focused
                if (!focused) {
                    shouldRequestContentFocus = true
                }
            },
            onNavClick = { id ->
                if (id == "home" || id == "movies") {
                    loadPath("/media")
                } else if (id == "update") {
                    if (updateInProgress) {
                        Toast.makeText(context, "Update already downloading", Toast.LENGTH_SHORT).show()
                    } else {
                        updateInProgress = true
                        Toast.makeText(context, "Downloading update…", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            val result = AppUpdater.downloadAndInstall(
                                context,
                                "https://github.com/lunatestus/alexxishot/releases/download/latest-dev/app-debug.apk"
                            ) { progress ->
                                // Optional progress handling
                            }
                            updateInProgress = false
                            if (result.isFailure) {
                                Toast.makeText(context, "Update failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
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
