package com.vibe.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.player.ui.theme.*

data class NavItem(val label: String, val id: String, val icon: ImageVector)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Sidebar(
    isExpanded: Boolean,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    onNavClick: (String) -> Unit
) {
    val navItems = listOf(
        NavItem("Home", "home", PlayerIcons.Home),
        NavItem("Movies", "movies", PlayerIcons.Movies),
        NavItem("TV Shows", "tv", PlayerIcons.Tv),
        NavItem("Settings", "settings", PlayerIcons.Settings)
    )

    val expandedWidth = 200.dp
    val collapsedWidth = 64.dp

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isExpanded) expandedWidth else collapsedWidth)
            .clipToBounds()
            .background(SidebarBg)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.hasFocus) }
            .padding(top = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isExpanded) 20.dp else 10.dp, vertical = 15.dp),
            contentAlignment = if (isExpanded) Alignment.CenterStart else Alignment.Center
        ) {
            if (isExpanded) {
                Text(
                    text = "Vibe Player",
                    color = TextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontFamily = DmSans
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = TextColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(SidebarBorder))

        Column(
            modifier = Modifier.padding(top = 16.dp)
        ) {
            navItems.forEachIndexed { index, item ->
                var isFocused by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties {
                            // Ensure up/down stays within the sidebar or stops, instead of leaking out
                            if (index == 0) up = FocusRequester.Cancel
                            if (index == navItems.lastIndex) down = FocusRequester.Cancel
                            // explicitly prevent right from bleeding into random things besides the content grid
                            right = FocusRequester.Default
                        }
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                    android.view.KeyEvent.KEYCODE_ENTER,
                                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                        onNavClick(item.id)
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                        .clickable { onNavClick(item.id) }
                        .background(if (isFocused) AccentColor else Color.Transparent)
                        .padding(horizontal = if (isExpanded) 20.dp else 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                ) {
                    val contentTint = if (isFocused) Color.Black else TextColor
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = contentTint,
                        modifier = Modifier.size(22.dp)
                    )
                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.label,
                            color = contentTint,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            fontFamily = DmSans
                        )
                    }
                }
            }
        }
    }
}
