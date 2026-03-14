package com.opneai.android

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.opneai.android.data.ApiClient
import com.opneai.android.data.FileItem
import com.opneai.android.ui.FileListAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var adapter: FileListAdapter
    private lateinit var pathText: TextView
    private lateinit var errorText: TextView
    private lateinit var loading: ProgressBar
    private lateinit var sidebarStatus: TextView
    private var currentPath: String = "/media"
    private val history = ArrayDeque<String>()
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        pathText = findViewById(R.id.path_text)
        errorText = findViewById(R.id.error_text)
        loading = findViewById(R.id.loading)
        sidebarStatus = findViewById(R.id.sidebar_status)

        val menuButton: ImageButton = findViewById(R.id.menu_button)
        val refreshButton: Button = findViewById(R.id.refresh_button)
        val sidebarHome: Button = findViewById(R.id.sidebar_home)
        val sidebarRefresh: Button = findViewById(R.id.sidebar_refresh)

        val listView: RecyclerView = findViewById(R.id.file_list)
        listView.layoutManager = LinearLayoutManager(this)
        adapter = FileListAdapter { item -> handleItemClick(item) }
        listView.adapter = adapter

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        refreshButton.setOnClickListener {
            ApiClient.resetBaseUrl()
            loadPath(currentPath, showSpinner = true)
        }

        sidebarHome.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            history.clear()
            loadPath("/media", showSpinner = true)
        }

        sidebarRefresh.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            ApiClient.resetBaseUrl()
            loadPath(currentPath, showSpinner = true)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawer(GravityCompat.START)
                    history.isNotEmpty() -> {
                        val previous = history.removeLast()
                        loadPath(previous, showSpinner = true)
                    }
                    else -> finish()
                }
            }
        })

        loadPath(currentPath, showSpinner = true)
    }

    private fun handleItemClick(item: FileItem) {
        if (item.type == "folder") {
            history.addLast(currentPath)
            loadPath(item.path, showSpinner = true)
        } else {
            Toast.makeText(this, "Selected file: ${item.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPath(path: String, showSpinner: Boolean) {
        currentPath = path
        pathText.text = path
        if (showSpinner) {
            loading.visibility = View.VISIBLE
        }
        errorText.visibility = View.GONE
        sidebarStatus.text = getString(R.string.sidebar_status_idle)

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val result = ApiClient.fetchFolder(path)
            loading.visibility = View.GONE

            if (result.error != null && result.error != "Empty folder") {
                val friendly = when {
                    result.error.startsWith("Tunnel starting") -> "Tunnel is starting. Please wait."
                    result.error.startsWith("Tunnel") -> "Backend is offline. Start it and retry."
                    result.error.startsWith("Socket") -> "Network issue. Check connection and retry."
                    result.error.startsWith("API HTTP") -> "Server error. Try again."
                    result.error.startsWith("API error") -> "Server error. Try again."
                    else -> "Something went wrong. Try again."
                }
                errorText.text = friendly
                errorText.visibility = View.VISIBLE
                adapter.updateItems(emptyList())
                sidebarStatus.text = friendly
                return@launch
            }

            if (result.items.isEmpty()) {
                errorText.text = "No files found"
                errorText.visibility = View.VISIBLE
                adapter.updateItems(emptyList())
                return@launch
            }

            adapter.updateItems(result.items)
        }
    }
}
