package com.opneai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opneai.android.data.ApiClient
import com.opneai.android.data.FileItem
import com.opneai.android.ui.screens.PlayerScreen
import com.opneai.android.ui.theme.OpenAIAndroidTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenAIAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FileBrowserScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileBrowserScreen() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentPath by remember { mutableStateOf("/media") }
    var history by remember { mutableStateOf(listOf<String>()) }
    var items by remember { mutableStateOf(emptyList<FileItem>()) }
    var activeItem by remember { mutableStateOf<FileItem?>(null) }
    var isPlayerVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun loadPath(path: String, forceRefresh: Boolean = false) {
        currentPath = path
        isLoading = true
        errorMessage = null
        loadJob?.cancel()
        loadJob = scope.launch {
            if (forceRefresh) ApiClient.resetBaseUrl()
            val result = ApiClient.fetchFolder(path)
            items = result.items
            errorMessage = result.error
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPath(currentPath)
    }

    BackHandler(enabled = !isPlayerVisible && (drawerState.isOpen || history.isNotEmpty())) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            history.isNotEmpty() -> {
                val prev = history.last()
                history = history.dropLast(1)
                loadPath(prev)
            }
        }
    }

    if (isPlayerVisible && activeItem != null) {
        PlayerScreen(
            item = activeItem!!,
            onClose = { isPlayerVisible = false }
        )
        return
    }

    val colorScheme = MaterialTheme.colorScheme

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(220.dp),
                drawerContainerColor = colorScheme.surface,
                drawerContentColor = colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "OpenAI Files",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isLoading) "Loading..." else (errorMessage ?: "Ready"),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp)
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            scope.launch { drawerState.close() }
                            history = emptyList()
                            loadPath("/media")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.onSurface,
                            contentColor = colorScheme.surface
                        )
                    ) {
                        Text("Home")
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            loadPath(currentPath, forceRefresh = true)
                        },
                        border = BorderStroke(1.dp, colorScheme.onSurface),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorScheme.onSurface
                        )
                    ) {
                        Text("Refresh")
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = currentPath,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { loadPath(currentPath, forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface,
                    actionIconContentColor = colorScheme.onSurface
                )
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorScheme.onBackground)
                    }
                }
                errorMessage != null && errorMessage != "Empty folder" -> {
                    val friendly = when {
                        errorMessage!!.startsWith("Tunnel starting") -> "Tunnel is starting. Please wait."
                        errorMessage!!.startsWith("Tunnel") -> "Backend is offline. Start it and retry."
                        errorMessage!!.startsWith("Socket") -> "Network issue. Check connection and retry."
                        errorMessage!!.startsWith("API HTTP") -> "Server error. Try again."
                        errorMessage!!.startsWith("API error") -> "Server error. Try again."
                        else -> "Something went wrong. Try again."
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = friendly,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onBackground
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { loadPath(currentPath, forceRefresh = true) },
                            border = BorderStroke(1.dp, colorScheme.onBackground),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colorScheme.onBackground
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
                items.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No files found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onBackground
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items, key = { it.path }) { item ->
                            FileRow(item = item) {
                                if (item.type == "folder") {
                                    history = history + currentPath
                                    loadPath(item.path)
                                } else {
                                    activeItem = item
                                    isPlayerVisible = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(item: FileItem, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.type == "folder") Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = colorScheme.onSurface
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.type == "folder") "Folder" else "File",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
