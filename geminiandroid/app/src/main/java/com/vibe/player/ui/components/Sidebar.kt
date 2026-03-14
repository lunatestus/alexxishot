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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
    val navItems = remember {
        listOf(
            NavItem("Home", "home", PlayerIcons.Home),
            NavItem("Movies", "movies", PlayerIcons.Movies),
            NavItem("TV Shows", "tv", PlayerIcons.Tv),
            NavItem("Update", "update", PlayerIcons.Update),
            NavItem("Settings", "settings", PlayerIcons.Settings)
        )
    }

    val expandedWidth = 180.dp
    val collapsedWidth = 56.dp
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = tween(200),
        label = "sidebarWidth"
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(sidebarWidth)
            .clipToBounds()
            .background(SidebarBg)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.hasFocus) }
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp)
        ) {
            navItems.forEachIndexed { index, item ->
                SidebarItem(
                    item = item,
                    isExpanded = isExpanded,
                    isFirst = index == 0,
                    isLast = index == navItems.lastIndex,
                    onNavClick = onNavClick
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SidebarItem(
    item: NavItem,
    isExpanded: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onNavClick: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val resolvedPadding = if (isExpanded) 16.dp else 12.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties {
                if (isFirst) up = FocusRequester.Cancel
                if (isLast) down = FocusRequester.Cancel
                right = FocusRequester.Default
            }
            .onFocusChanged {
                if (isFocused != it.isFocused) {
                    isFocused = it.isFocused
                }
            }
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
            .padding(horizontal = resolvedPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        val contentTint = if (isFocused) Color.Black else TextColor
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentTint,
            modifier = Modifier.size(20.dp)
        )
        if (isExpanded) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.label,
                color = contentTint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                fontFamily = DmSans
            )
        }
    }
}
