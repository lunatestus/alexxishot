package com.vibe.player.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vibe.player.data.FileItem
import com.vibe.player.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    item: FileItem,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    
    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Using a sample Big Buck Bunny stream as a fallback if Uri is invalid
            val videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    // State for HUD
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // Polling player state
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)
            isPlaying = exoPlayer.isPlaying
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Actual Video Player
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false // We use our custom Compose HUD
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // HUD Overlay - Scaled Down Padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE6000000)),
                        startY = 0.6f
                    )
                )
                .padding(24.dp) // Scaled down from 40dp
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    color = TextColor,
                    fontSize = 18.sp, // Scaled down from 20sp
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Seekbar with Dot
                var isProgressFocused by remember { mutableStateOf(false) }
                val barHeight by animateDpAsState(targetValue = if (isProgressFocused) 8.dp else 4.dp)
                val dotAlpha by animateFloatAsState(targetValue = if (isProgressFocused) 1f else 0f)
                val dotSize by animateDpAsState(targetValue = if (isProgressFocused) 20.dp else 12.dp)
                
                val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isProgressFocused = it.isFocused }
                        .focusable()
                        .padding(vertical = 8.dp)
                        .clickable { /* D-pad center click can toggle play/pause */ }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(CircleShape)
                                .background(ProgressTrack)
                        )
                        // Fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(barHeight)
                                .background(ProgressFill)
                        )
                        // Dot
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.weight(progress.coerceAtLeast(0.001f)))
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .scale(dotAlpha)
                            )
                            Spacer(modifier = Modifier.weight((1f - progress).coerceAtLeast(0.001f)))
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls - Scaled Down
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerButton(
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                            isPrimary = true
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        PlayerButton(
                            icon = Icons.Default.FastRewind,
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        PlayerButton(
                            icon = Icons.Default.FastForward,
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)) }
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
    val size = if (isPrimary) 64.dp else 48.dp // Scaled down from 72/56
    
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.1f else 1f)

    Box(
        modifier = Modifier
            .scale(scale)
            .then(if (label != null) Modifier.wrapContentWidth() else Modifier.size(size))
            .then(if (label != null) Modifier.height(48.dp) else Modifier)
            .clip(CircleShape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .background(if (isFocused) Color.White else Color.Transparent)
            .padding(horizontal = if (label != null) 16.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else Color.White,
                modifier = Modifier.size(if (isPrimary) 32.dp else 24.dp)
            )
            if (label != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = if (isFocused) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk
                )
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
