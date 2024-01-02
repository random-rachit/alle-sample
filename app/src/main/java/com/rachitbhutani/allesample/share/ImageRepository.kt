package com.rachitbhutani.allesample.share

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.rachitbhutani.allesample.MainViewModel
import com.rachitbhutani.allesample.share.model.ScreenshotItem
import javax.inject.Inject

class ImageRepository @Inject constructor(private val contentResolver: ContentResolver) {

    fun getImagesFromGallery(page: Int): List<ScreenshotItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val query =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val selection = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, MainViewModel.INITIAL_LOAD_SIZE)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, page * MainViewModel.INITIAL_LOAD_SIZE)
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%Screenshot%' OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%screenshot%' AND ${MediaStore.Images.Media.IS_TRASHED}=0"
                    )
                }
                contentResolver.query(collection, projection, selection, null)
            } else {
                contentResolver.query(
                    collection,
                    projection,
                    "${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%Screenshot%' OFFSET ${page * 20} ROWS",
                    null,
                    sortOrder
                )
            }
        query?.use { cursor ->
            val imageList = mutableListOf<ScreenshotItem>()
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, contentUri)
                val imageData = ScreenshotItem(id, contentUri, bitmap)
                imageList.add(imageData)
            }
            return imageList
        }
        return emptyList()
    }
}