@file:OptIn(ExperimentalComposeUiApi::class)

package com.vibe.player.ui.screens

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
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
    val streamUrl = remember(item.path) { ApiClient.getStreamUrl(item.path) }
    
    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBackBuffer(60_000, true)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    
    val seekbarFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val forwardFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val screenFocusRequester = remember { FocusRequester() }

    // Listen to exoPlayer play state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "Playback error"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(streamUrl) {
        playbackError = null
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        if (!streamUrl.isNullOrBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            playbackError = "Stream unavailable"
        }
    }

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    LaunchedEffect(lastInteraction, showControls) {
        if (showControls) {
            delay(3000)
            if (System.currentTimeMillis() - lastInteraction >= 3000) {
                showControls = false
                screenFocusRequester.requestFocus() // Steal focus so hidden controls don't keep it
            }
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            playPauseFocusRequester.requestFocus() // Return focus to controls when shown
        } else {
            screenFocusRequester.requestFocus()
        }
    }

    // Wrap AndroidView in a focusable box to capture D-Pad events when controls are hidden
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(screenFocusRequester)
            .focusable(true)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                            onClose()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_SPACE -> {
                            lastInteraction = System.currentTimeMillis()
                            if (!showControls) {
                                showControls = true
                            }
                            if (playbackError == null) {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            lastInteraction = System.currentTimeMillis()
                            if (!showControls) {
                                showControls = true
                                val offset = if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -10000L else 10000L
                                if (playbackError == null) {
                                    val rawDuration = exoPlayer.duration
                                    val safeDuration = if (rawDuration > 0 && rawDuration != C.TIME_UNSET) rawDuration else Long.MAX_VALUE
                                    val next = (exoPlayer.currentPosition + offset).coerceAtLeast(0L).coerceAtMost(safeDuration)
                                    exoPlayer.seekTo(next)
                                }
                                true
                            } else {
                                false
                            }
                        }
                        else -> {
                            lastInteraction = System.currentTimeMillis()
                            if (!showControls) {
                                showControls = true
                                true
                            } else {
                                false
                            }
                        }
                    }
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = isPlaying
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.keepScreenOn = isPlaying
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = playbackError ?: "Playback error",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = DmSans
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Press Back to exit",
                        color = Color(0xB3FFFFFF),
                        fontSize = 12.sp,
                        fontFamily = DmSans
                    )
                }
            }
        }

        // Use alpha instead of AnimatedVisibility so the UI tree doesn't change, 
        // which prevents focus from dropping when controls hide.
        val controlsAlpha by animateFloatAsState(
            targetValue = if (showControls) 1f else 0f, 
            animationSpec = tween(300)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(controlsAlpha)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)), startY = 0.6f))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                Text(
                    text = item.name,
                    color = TextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = DmSans
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                PlayerSeekBar(
                    exoPlayer = exoPlayer,
                    onInteraction = { lastInteraction = System.currentTimeMillis() },
                    showControls = showControls,
                    focusRequester = seekbarFocusRequester,
                    downRequester = playPauseFocusRequester
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            modifier = Modifier.focusRequester(playPauseFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                right = rewindFocusRequester
                                left = FocusRequester.Cancel
                            },
                            enabled = showControls
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerButton(
                            icon = PlayerIcons.Rewind,
                            onClick = { 
                                lastInteraction = System.currentTimeMillis()
                                val next = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(next)
                            },
                            modifier = Modifier.focusRequester(rewindFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                left = playPauseFocusRequester
                                right = forwardFocusRequester
                            },
                            enabled = showControls
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerButton(
                            icon = PlayerIcons.Forward,
                            onClick = { 
                                lastInteraction = System.currentTimeMillis()
                                val rawDuration = exoPlayer.duration
                                val safeDuration = if (rawDuration > 0 && rawDuration != C.TIME_UNSET) rawDuration else Long.MAX_VALUE
                                val next = (exoPlayer.currentPosition + 10000).coerceAtMost(safeDuration)
                                exoPlayer.seekTo(next)
                            },
                            modifier = Modifier.focusRequester(forwardFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                left = rewindFocusRequester
                                right = settingsFocusRequester
                            },
                            enabled = showControls
                        )
                    }

                    PlayerButton(
                        icon = PlayerIcons.Settings, 
                        onClick = { lastInteraction = System.currentTimeMillis() },
                        modifier = Modifier.focusRequester(settingsFocusRequester),
                        focusProps = {
                            up = seekbarFocusRequester
                            left = forwardFocusRequester
                            right = FocusRequester.Cancel
                        },
                        enabled = showControls
                    )
                }
            }
        }
    }
}

// Extract seekbar to scope state reads and prevent whole-screen recomposition
@Composable
fun PlayerSeekBar(
    exoPlayer: ExoPlayer, 
    onInteraction: () -> Unit,
    showControls: Boolean,
    focusRequester: FocusRequester,
    downRequester: FocusRequester
) {
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var isProgressFocused by remember { mutableStateOf(false) }

    LaunchedEffect(exoPlayer, showControls) {
        while (true) {
            if (!showControls) {
                delay(500)
                continue
            }
            // Don't update from player if user is actively seeking (debounce)
            if (System.currentTimeMillis() - lastSeekTime > 500) {
                currentPosition = exoPlayer.currentPosition
            }
            val rawDuration = exoPlayer.duration
            duration = if (rawDuration > 0 && rawDuration != C.TIME_UNSET) rawDuration else 0L
            delay(250) // Reduced refresh rate to save CPU
        }
    }

    val barHeight by animateDpAsState(targetValue = if (isProgressFocused) 8.dp else 3.dp)
    val dotSize by animateDpAsState(targetValue = if (isProgressFocused) 14.dp else 0.dp)
    
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    // Don't animate the progress bar filling, as it fights with the user seeking
    val animatedProgress = progress.coerceIn(0f, 1f)
    val containerHeight = 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusProperties { down = downRequester }
            .onFocusChanged { 
                if (isProgressFocused != it.isFocused) {
                    isProgressFocused = it.isFocused
                }
                if (it.isFocused) onInteraction()
            }
            .onKeyEvent { keyEvent ->
                onInteraction()
                
                val isLeft = keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                val isRight = keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                
                if (isLeft || isRight) {
                    val offset = if (isLeft) -15000L else 15000L
                    
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        lastSeekTime = System.currentTimeMillis()
                        // Rapidly update local UI state without blocking main thread
                        val next = (currentPosition + offset).coerceAtLeast(0L)
                        currentPosition = if (duration > 0) next.coerceAtMost(duration) else next
                        true
                    } else if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        lastSeekTime = System.currentTimeMillis()
                        // Commit the final position to ExoPlayer once user releases the button
                        exoPlayer.seekTo(currentPosition)
                        true
                    } else {
                        false
                    }
                } else false
            }
            // Only focusable if controls are shown
            .focusable(showControls)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatTime(currentPosition),
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp,
                fontFamily = DmSans
            )
            Spacer(modifier = Modifier.width(10.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(containerHeight)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(CircleShape)
                        .background(ProgressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(ProgressFill)
                    )
                }
                val dotOffset = (maxWidth * animatedProgress - dotSize / 2).coerceAtLeast(0.dp)
                Box(
                    modifier = Modifier
                        .offset(x = dotOffset)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                formatTimeOrUnknown(duration),
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp,
                fontFamily = DmSans
            )
        }
    }
}

@Composable
fun PlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusProps: (androidx.compose.ui.focus.FocusProperties.() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val size = if (isPrimary) 44.dp else 40.dp
    val focusedScale = if (isPrimary) 1.0f else 1.1f
    val scale by animateFloatAsState(targetValue = if (isFocused) focusedScale else 1f)

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .focusProperties { focusProps?.invoke(this) }
            .onFocusChanged {
                if (isFocused != it.isFocused) {
                    isFocused = it.isFocused
                }
            }
            .focusable(enabled)
            .onKeyEvent { keyEvent ->
                if (!enabled) return@onKeyEvent false
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_SPACE -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(enabled = enabled) { onClick() }
            .background(if (isFocused) Color.White else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(if (isPrimary) 28.dp else 20.dp)
        )
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun formatTimeOrUnknown(milliseconds: Long): String {
    if (milliseconds <= 0L) return "--:--"
    return formatTime(milliseconds)
}
