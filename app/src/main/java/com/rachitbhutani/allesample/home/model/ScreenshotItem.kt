package com.rachitbhutani.allesample.home.model

import android.graphics.Bitmap
import android.net.Uri

data class ScreenshotItem(
    val id: Long?,
    val uri: Uri? = null,
    val bitmap: Bitmap? = null,
    val path: String? = null,
) {
    var isActive: Boolean = false
    var description: ScreenshotDescription? = null
}

data class ScreenshotDescription(
    val note: String? = null,
    val description: String? = null,
    val labels: List<String>?
)