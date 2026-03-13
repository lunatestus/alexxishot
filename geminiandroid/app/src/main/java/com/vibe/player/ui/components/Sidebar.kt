package com.vibe.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.player.ui.theme.*

@Composable
fun Sidebar(onNavClick: (String) -> Unit) {
    val navItems = listOf("Home", "Movies", "TV Shows", "Settings")
    Column(modifier = Modifier.fillMaxHeight().width(250.dp).background(SidebarBg).padding(top = 30.dp)) {
        Text("Vibe Player", color = TextColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(25.dp))
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(SidebarBorder))
        LazyColumn(modifier = Modifier.padding(top = 20.dp)) {
            items(navItems) { item ->
                var isFocused by remember { mutableStateOf(false) }
                Text(item, color = TextColor, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }.focusable().clickable { onNavClick(item) }
                        .background(if (isFocused) AccentColor else Color.Transparent).padding(horizontal = 25.dp, vertical = 15.dp))
            }
        }
    }
}
