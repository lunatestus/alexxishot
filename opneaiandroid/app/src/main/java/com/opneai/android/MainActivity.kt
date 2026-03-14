package com.opneai.android

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.tv.foundation.lazy.TvLazyColumn
import androidx.tv.foundation.lazy.itemsIndexed
import androidx.tv.foundation.lazy.rememberTvLazyListState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.opneai.android.data.ApiClient
import com.opneai.android.data.FileItem
import com.opneai.android.ui.screens.PlayerScreen
import com.opneai.android.ui.theme.OpenAIAndroidTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberTvLazyListState()

    var currentPath by remember { mutableStateOf("/media") }
    var history by remember { mutableStateOf(listOf<String>()) }
    var items by remember { mutableStateOf(emptyList<FileItem>()) }
    var activeItem by remember { mutableStateOf<FileItem?>(null) }
    var isPlayerVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadJob by remember { mutableStateOf<Job?>(null) }
    var upgradeDownloadId by remember { mutableLongStateOf(-1L) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    val firstItemFocusRequester = remember { FocusRequester() }

    DisposableEffect(upgradeDownloadId) {
        if (upgradeDownloadId == -1L) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != upgradeDownloadId) return
                val file = File(ctx.externalCacheDir, "updates/app-debug.apk")
                if (!file.exists()) {
                    Toast.makeText(ctx, "Download failed", Toast.LENGTH_SHORT).show()
                    return
                }
                val apkUri: Uri = FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.fileprovider",
                    file
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(installIntent)
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

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

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            focusedIndex = 0
            kotlinx.coroutines.delay(50)
            firstItemFocusRequester.requestFocus()
        }
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
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))
                    SidebarButton(
                        label = "Home",
                        onClick = {
                            scope.launch { drawerState.close() }
                            history = emptyList()
                            loadPath("/media")
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    SidebarButton(
                        label = "Refresh",
                        onClick = {
                            scope.launch { drawerState.close() }
                            loadPath(currentPath, forceRefresh = true)
                        }
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Quick Links",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp
                        ),
                        color = colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(10.dp))
                    SidebarButton(
                        label = "Library",
                        onClick = { scope.launch { drawerState.close() } }
                    )
                    Spacer(Modifier.height(8.dp))
                    SidebarButton(
                        label = "Downloads",
                        onClick = { scope.launch { drawerState.close() } }
                    )
                    Spacer(Modifier.height(8.dp))
                    SidebarButton(
                        label = "Settings",
                        onClick = { scope.launch { drawerState.close() } }
                    )
                    Spacer(Modifier.height(8.dp))
                    SidebarButton(
                        label = "Upgrade",
                        onClick = {
                            scope.launch { drawerState.close() }
                            startUpgradeDownload(context) { id -> upgradeDownloadId = id }
                        }
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (!drawerState.isOpen) {
                                    scope.launch { drawerState.open() }
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionRight -> {
                                if (drawerState.isOpen) {
                                    scope.launch { drawerState.close() }
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
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
                    TvLazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
                            FileRow(
                                item = item,
                                focusRequester = if (index == 0) firstItemFocusRequester else null,
                                onFocused = { focusedIndex = index }
                            ) {
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
private fun FileRow(
    item: FileItem,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = if (isFocused) colorScheme.onSurface else colorScheme.surfaceVariant,
        label = "file_row_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isFocused) colorScheme.surface else colorScheme.onSurface,
        label = "file_row_text"
    )
    val subTextColor by animateColorAsState(
        targetValue = if (isFocused) colorScheme.surface.copy(alpha = 0.7f) else colorScheme.onSurfaceVariant,
        label = "file_row_subtext"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
        label = "file_row_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        label = "file_row_scale"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onFocused()
                }
            }
            .focusable()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = BorderStroke(1.dp, borderColor),
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
                tint = textColor
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.type == "folder") "Folder" else "File",
                    style = MaterialTheme.typography.labelLarge,
                    color = subTextColor
                )
            }
        }
    }
}

@Composable
private fun SidebarButton(
    label: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = if (isFocused) colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
        label = "sidebar_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isFocused) colorScheme.surface else colorScheme.onSurface,
        label = "sidebar_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) androidx.compose.ui.graphics.Color.Transparent else colorScheme.onSurface.copy(alpha = 0.25f),
        label = "sidebar_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        label = "sidebar_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isFocused) 2.dp else 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = textColor
            )
        }
    }
}

private const val UPGRADE_APK_URL =
    "https://github.com/lunatestus/alexxishot/releases/download/latest-dev/app-debug.apk"

private fun startUpgradeDownload(context: Context, onEnqueued: (Long) -> Unit) {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val targetDir = File(context.externalCacheDir, "updates")
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }
    val targetFile = File(targetDir, "app-debug.apk")
    if (targetFile.exists()) {
        targetFile.delete()
    }
    val request = DownloadManager.Request(Uri.parse(UPGRADE_APK_URL)).apply {
        setTitle("OpenAI Android Update")
        setDescription("Downloading update...")
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        setDestinationUri(Uri.fromFile(targetFile))
    }
    val downloadId = manager.enqueue(request)
    onEnqueued(downloadId)
    Toast.makeText(context, "Downloading update…", Toast.LENGTH_SHORT).show()
}
