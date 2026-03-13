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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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

    fun loadPath(path: String) { currentPath = path; items = MockFileSystem.fetchFolder(path) }

    BackHandler(enabled = history.isNotEmpty() || playingItem != null) {
        if (playingItem != null) playingItem = null
        else if (history.isNotEmpty()) { val prev = history.last(); history = history.dropLast(1); loadPath(prev) }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(onNavClick = {})
            Column(modifier = Modifier.fillMaxSize().padding(40.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("MovieApp", color = TextColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    var isTFocused by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(CardBg).border(if (isTFocused) 3.dp else 2.dp, if (isTFocused) AccentColor else ViewToggleBorder, RoundedCornerShape(12.dp)).onFocusChanged { isTFocused = it.isFocused }.focusable().clickable { isListView = !isListView }, contentAlignment = Alignment.Center) {
                        Text(if (isListView) "[::]" else "[=]", color = TextColor)
                    }
                }
                if (isListView) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        items(items) { item -> MediaCard(item, true) { if (item.type == "folder") { history = history + currentPath; loadPath(item.path) } else playingItem = item } }
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Adaptive(280.dp), horizontalArrangement = Arrangement.spacedBy(30.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) {
                        items(items) { item -> MediaCard(item, false) { if (item.type == "folder") { history = history + currentPath; loadPath(item.path) } else playingItem = item } }
                    }
                }
            }
        }
        if (playingItem != null) PlayerScreen(playingItem!!) { playingItem = null }
    }
}
