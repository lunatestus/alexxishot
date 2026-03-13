package com.vibe.player.ui.screens

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    item: FileItem,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // Polling faster (100ms) for smoother updates
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)
            isPlaying = exoPlayer.isPlaying
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)), startY = 0.6f))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                Text(
                    text = item.name,
                    color = TextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // --- ADVANCED SEEKBAR ---
                var isProgressFocused by remember { mutableStateOf(false) }
                val barHeight by animateDpAsState(targetValue = if (isProgressFocused) 10.dp else 4.dp)
                val dotSize by animateDpAsState(targetValue = if (isProgressFocused) 20.dp else 0.dp)
                
                // Smoothly animate the bar fill to prevent jumping
                val rawProgress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = rawProgress.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 150)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isProgressFocused = it.isFocused }
                        .onKeyEvent { keyEvent ->
                            // Handle D-Pad Left/Right for seeking when focused
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L))
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .focusable()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Background Track
                        Box(modifier = Modifier.fillMaxWidth().height(barHeight).clip(CircleShape).background(ProgressTrack))
                        
                        // Animated Progress Fill
                        Box(modifier = Modifier.fillMaxWidth(animatedProgress).height(barHeight).background(ProgressFill))
                        
                        // Scrubber Dot (Positions based on animated progress)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.weight(animatedProgress.coerceAtLeast(0.001f)))
                            Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(Color.White))
                            Spacer(modifier = Modifier.weight((1f - animatedProgress).coerceAtLeast(0.001f)))
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition), color = Color(0xCCFFFFFF), fontSize = 14.sp, fontFamily = SpaceGrotesk)
                        Text(formatTime(duration), color = Color(0xCCFFFFFF), fontSize = 14.sp, fontFamily = SpaceGrotesk)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- CONTROLS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerButton(
                            icon = if (isPlaying) PlayerIcons.Pause else PlayerIcons.Play,
                            onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                            isPrimary = true
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        PlayerButton(
                            icon = PlayerIcons.Rewind,
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        PlayerButton(
                            icon = PlayerIcons.Forward,
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)) }
                        )
                    }

                    PlayerButton(icon = PlayerIcons.Settings, onClick = {})
                }
            }
        }
    }
}

@Composable
fun PlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val size = if (isPrimary) 64.dp else 48.dp
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.1f else 1f)

    Box(
        modifier = Modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .background(if (isFocused) Color.White else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(if (isPrimary) 32.dp else 24.dp)
        )
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
