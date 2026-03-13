package com.vibe.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.player.data.FileItem
import com.vibe.player.data.MockFileSystem
import com.vibe.player.ui.components.MediaCard
import com.vibe.player.ui.components.Sidebar
import com.vibe.player.ui.theme.*

@Composable
fun MainScreen() {
    var currentPath by remember { mutableStateOf("/") }
    var history by remember { mutableStateOf(listOf<String>()) }
    var items by remember { mutableStateOf(MockFileSystem.fetchFolder("/")) }
    var isListView by remember { mutableStateOf(true) }
    var playingItem by remember { mutableStateOf<FileItem?>(null) }
    var isSidebarFocused by remember { mutableStateOf(false) }

    // Scroll States
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    fun loadPath(path: String) {
        currentPath = path
        items = MockFileSystem.fetchFolder(path)
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

    // Parallax and Dimming
    val contentAlpha by animateFloatAsState(targetValue = if (isSidebarFocused) 0.5f else 1f)
    val contentParallax by animateDpAsState(targetValue = if (isSidebarFocused) 20.dp else 0.dp)

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                isExpanded = isSidebarFocused,
                onFocusChange = { isSidebarFocused = it },
                onNavClick = { /* Handle nav */ }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = contentParallax) // Parallax shift
                    .alpha(contentAlpha)
                    .padding(top = 24.dp, start = 24.dp, bottom = 24.dp, end = 40.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MovieApp",
                            color = TextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (currentPath == "/") "" else "/ ${currentPath.removePrefix("/")}",
                            color = BreadcrumbColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SpaceGrotesk
                        )
                    }

                    var isToggleFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBg)
                            .border(
                                width = if (isToggleFocused) 2.dp else 1.dp,
                                color = if (isToggleFocused) AccentColor else ViewToggleBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .onFocusChanged { isToggleFocused = it.isFocused }
                            .focusable()
                            .clickable { isListView = !isListView },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListView) Icons.Default.GridView else Icons.Default.List,
                            contentDescription = "Toggle View",
                            tint = TextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Grid/List with Scroll-to-Center Logic
                if (isListView) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp) // Extra padding for centering
                    ) {
                        itemsIndexed(items) { index, item ->
                            val scope = rememberCoroutineScope()
                            MediaCard(
                                item = item,
                                index = index,
                                isListView = true,
                                onClick = {
                                    if (item.type == "folder") {
                                        history = history + currentPath
                                        loadPath(item.path)
                                    } else {
                                        playingItem = item
                                    }
                                }
                            )
                            
                            // Scroll centering logic (simplified for demo)
                            // In a real app, you'd use a custom focus responder or 
                            // check which item is focused to trigger animateScrollToItem
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 220.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        itemsIndexed(items) { index, item ->
                            MediaCard(
                                item = item,
                                index = index,
                                isListView = false,
                                onClick = {
                                    if (item.type == "folder") {
                                        history = history + currentPath
                                        loadPath(item.path)
                                        isSidebarFocused = false
                                    } else {
                                        playingItem = item
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (playingItem != null) {
            PlayerScreen(item = playingItem!!, onClose = { playingItem = null })
        }
    }
}
