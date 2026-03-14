package com.opneai.android.data

data class FileItem(
    val type: String,
    val name: String,
    val path: String,
    val size: Long = 0L
)
