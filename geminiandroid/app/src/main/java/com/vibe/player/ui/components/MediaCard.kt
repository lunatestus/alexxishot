package com.vibe.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.player.data.FileItem
import com.vibe.player.ui.theme.*

@Composable
fun MediaCard(
    item: FileItem,
    isListView: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Animations for scale and border
    val scale by animateFloatAsState(
        targetValue = if (isFocused && !isListView) 1.06f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AccentColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )

    val modifier = Modifier
        .scale(scale)
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .clickable { onClick() }
        .clip(RoundedCornerShape(12.dp))
        .border(
            width = if (isFocused) 3.dp else 0.dp,
            color = borderColor,
            shape = RoundedCornerShape(12.dp)
        )

    if (isListView) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(CardBg)
                .padding(12.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0044CC)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Vibe",
                    color = Color(0x33FFFFFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = SpaceGrotesk
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    color = TextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = item.type.uppercase(),
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    fontFamily = SpaceGrotesk
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .aspectRatio(16f / 9f)
                .background(CardBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0044CC)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Vibe",
                    color = Color(0x33FFFFFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    fontFamily = SpaceGrotesk
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                            startY = 0.5f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(15.dp)
            ) {
                Text(
                    text = item.name,
                    color = TextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = item.type.uppercase(),
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    fontFamily = SpaceGrotesk
                )
            }
        }
    }
}
