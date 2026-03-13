package com.vibe.player.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.player.ui.theme.*

data class NavItem(val label: String, val id: String, val icon: ImageVector)

@Composable
fun Sidebar(
    isExpanded: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onNavClick: (String) -> Unit
) {
    val navItems = listOf(
        NavItem("Home", "home", Icons.Default.Home),
        NavItem("Movies", "movies", Icons.Default.Movie),
        NavItem("TV Shows", "tv", Icons.Default.Tv),
        NavItem("Settings", "settings", Icons.Default.Settings)
    )

    val width by animateDpAsState(targetValue = if (isExpanded) 250.dp else 80.dp)

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .background(SidebarBg)
            .onFocusChanged { onFocusChange(it.hasFocus) }
            .padding(top = 30.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isExpanded) 25.dp else 10.dp, vertical = 20.dp),
            contentAlignment = if (isExpanded) Alignment.CenterStart else Alignment.Center
        ) {
            if (isExpanded) {
                Text(
                    text = "Vibe Player",
                    color = TextColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = TextColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(SidebarBorder))

        LazyColumn(
            modifier = Modifier.padding(top = 20.dp)
        ) {
            items(navItems) { item ->
                var isFocused by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable { onNavClick(item.id) }
                        .background(if (isFocused) AccentColor else Color.Transparent)
                        .padding(horizontal = if (isExpanded) 25.dp else 20.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = TextColor,
                        modifier = Modifier.size(24.dp)
                    )
                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(15.dp))
                        Text(
                            text = item.label,
                            color = TextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
