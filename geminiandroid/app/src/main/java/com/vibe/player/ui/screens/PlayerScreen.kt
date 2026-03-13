package com.vibe.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.player.data.FileItem
import com.vibe.player.ui.theme.*

@Composable
fun PlayerScreen(
    item: FileItem,
    onClose: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.22f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Frame Placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("VIDEO CONTENT", color = Color.DarkGray, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }

        // HUD Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE6000000)),
                        startY = 0.5f
                    )
                )
                .padding(40.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    color = TextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Progress Bar
                var isProgressFocused by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isProgressFocused = it.isFocused }
                        .focusable()
                        .padding(vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isProgressFocused) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(ProgressTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(ProgressFill)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("00:12", color = Color(0xCCFFFFFF), fontSize = 16.sp)
                        Text("04:36", color = Color(0xCCFFFFFF), fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerButton(
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = { isPlaying = !isPlaying },
                            isPrimary = true
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        PlayerButton(
                            icon = Icons.Default.FastRewind,
                            onClick = { progress = (progress - 0.02f).coerceAtLeast(0f) }
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        PlayerButton(
                            icon = Icons.Default.FastForward,
                            onClick = { progress = (progress + 0.02f).coerceAtMost(1f) }
                        )
                    }

                    PlayerButton(
                        icon = Icons.Default.Settings,
                        label = "SETTINGS",
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerButton(
    icon: ImageVector,
    label: String? = null,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val size = if (isPrimary) 72.dp else 56.dp
    
    Box(
        modifier = Modifier
            .then(if (label != null) Modifier.wrapContentWidth() else Modifier.size(size))
            .then(if (label != null) Modifier.height(56.dp) else Modifier)
            .clip(CircleShape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .background(if (isFocused) Color.White else Color.Transparent)
            .padding(horizontal = if (label != null) 20.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else Color.White,
                modifier = Modifier.size(if (isPrimary) 40.dp else 28.dp)
            )
            if (label != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = if (isFocused) Color.Black else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
