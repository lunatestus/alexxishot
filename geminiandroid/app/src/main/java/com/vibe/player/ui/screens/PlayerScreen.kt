package com.vibe.player.ui.screens

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vibe.player.data.ApiClient
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
    val streamUrl = remember { ApiClient.getStreamUrl(item.path) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUrl = streamUrl ?: ""
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)
            isPlaying = exoPlayer.isPlaying
            delay(100)
        }
    }

    LaunchedEffect(lastInteraction) {
        delay(3000)
        showControls = false
    }

    DisposableEffect(Unit) {
        focusRequester.requestFocus()
        onDispose { 
            exoPlayer.stop()
            exoPlayer.release() 
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { 
                lastInteraction = System.currentTimeMillis()
                if (!showControls) {
                    showControls = true
                    true
                } else false
            }
    ) {
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

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
                        fontSize = 16.sp, // Scaled down
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp)) // Tightened spacing

                    // --- SEEKBAR ---
                    var isProgressFocused by remember { mutableStateOf(false) }
                    val barHeight by animateDpAsState(targetValue = if (isProgressFocused) 8.dp else 3.dp)
                    val dotSize by animateDpAsState(targetValue = if (isProgressFocused) 14.dp else 0.dp)
                    
                    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(150))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { 
                                isProgressFocused = it.isFocused 
                                if (it.isFocused) lastInteraction = System.currentTimeMillis()
                            }
                            .onKeyEvent { keyEvent ->
                                lastInteraction = System.currentTimeMillis()
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
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(barHeight).clip(CircleShape).background(ProgressTrack))
                            Box(modifier = Modifier.fillMaxWidth(animatedProgress).height(barHeight).background(ProgressFill))
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val dotOffset = (maxWidth - dotSize) * animatedProgress
                                Box(
                                    modifier = Modifier
                                        .offset(x = dotOffset)
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(currentPosition), color = Color(0xCCFFFFFF), fontSize = 12.sp, fontFamily = SpaceGrotesk)
                            Text(formatTime(duration), color = Color(0xCCFFFFFF), fontSize = 12.sp, fontFamily = SpaceGrotesk)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp)) // Reduced space to controls row

                    // --- CONTROLS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerButton(
                                icon = if (isPlaying) PlayerIcons.Pause else PlayerIcons.Play,
                                onClick = { 
                                    lastInteraction = System.currentTimeMillis()
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play() 
                                },
                                isPrimary = true,
                                modifier = Modifier.focusRequester(focusRequester)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            PlayerButton(
                                icon = PlayerIcons.Rewind,
                                onClick = { 
                                    lastInteraction = System.currentTimeMillis()
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) 
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            PlayerButton(
                                icon = PlayerIcons.Forward,
                                onClick = { 
                                    lastInteraction = System.currentTimeMillis()
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)) 
                                }
                            )
                        }

                        PlayerButton(icon = PlayerIcons.Settings, onClick = { lastInteraction = System.currentTimeMillis() })
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val size = if (isPrimary) 56.dp else 40.dp // Further scaled down
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.1f else 1f)

    Box(
        modifier = modifier
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
            modifier = Modifier.size(if (isPrimary) 28.dp else 20.dp) // Scaled down icons
        )
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
