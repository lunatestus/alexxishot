package com.vibe.player.data

data class FileItem(val type: String, val name: String, val path: String, val thumb: String = "")

object MockFileSystem {
    val data = mapOf(
        "/" to listOf(
            FileItem("folder", "Movies", "/Movies"),
            FileItem("folder", "TV Shows", "/TV Shows"),
            FileItem("folder", "Music", "/Music"),
            FileItem("file", "Welcome Video.mp4", "/Welcome Video.mp4"),
            FileItem("file", "ReadMe.txt", "/ReadMe.txt")
        ),
        "/Movies" to listOf(
            FileItem("file", "The Matrix.mp4", "/Movies/The Matrix.mp4"),
            FileItem("file", "Inception.mp4", "/Movies/Inception.mp4")
        )
    )
    fun fetchFolder(path: String) = data[path] ?: emptyList()
}
