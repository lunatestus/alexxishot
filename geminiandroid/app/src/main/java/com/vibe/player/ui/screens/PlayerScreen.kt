package com.vibe.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.player.data.FileItem
import com.vibe.player.ui.theme.*

@Composable
fun PlayerScreen(item: FileItem, onClose: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0.22f) }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)), startY = 0.5f)).padding(40.dp)) {
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                Text(item.name, color = TextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                var isProgFocused by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().height(if (isProgFocused) 10.dp else 6.dp).clip(CircleShape).background(ProgressTrack).onFocusChanged { isProgFocused = it.isFocused }.focusable()) {
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(ProgressFill))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerButton(">")
                    Spacer(modifier = Modifier.width(20.dp))
                    PlayerButton("<<")
                    Spacer(modifier = Modifier.width(20.dp))
                    PlayerButton(">>")
                }
            }
        }
    }
}

@Composable
fun PlayerButton(text: String) {
    var isFocused by remember { mutableStateOf(false) }
    Box(modifier = Modifier.size(56.dp).clip(CircleShape).onFocusChanged { isFocused = it.isFocused }.focusable().background(if (isFocused) Color.White else Color.Transparent), contentAlignment = Alignment.Center) {
        Text(text, color = if (isFocused) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}
