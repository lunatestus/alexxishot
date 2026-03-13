package com.vibe.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            Sidebar(
                isExpanded = isSidebarFocused,
                onFocusChange = { isSidebarFocused = it },
                onNavClick = { /* Handle nav */ }
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "MovieApp",
                            color = TextColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text(
                            text = if (currentPath == "/") "" else "/ ${currentPath.removePrefix("/")}",
                            color = BreadcrumbColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // View Toggle
                    var isToggleFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBg)
                            .border(
                                width = if (isToggleFocused) 3.dp else 2.dp,
                                color = if (isToggleFocused) AccentColor else ViewToggleBorder,
                                shape = RoundedCornerShape(12.dp)
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Grid/List
                if (isListView) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items) { item ->
                            MediaCard(item = item, isListView = true, onClick = {
                                if (item.type == "folder") {
                                    history = history + currentPath
                                    loadPath(item.path)
                                } else {
                                    playingItem = item
                                }
                            })
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        horizontalArrangement = Arrangement.spacedBy(30.dp),
                        verticalArrangement = Arrangement.spacedBy(30.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items) { item ->
                            MediaCard(item = item, isListView = false, onClick = {
                                if (item.type == "folder") {
                                    history = history + currentPath
                                    loadPath(item.path)
                                } else {
                                    playingItem = item
                                }
                            })
                        }
                    }
                }
            }
        }

        // Player Overlay
        if (playingItem != null) {
            PlayerScreen(item = playingItem!!, onClose = { playingItem = null })
        }
    }
}
