package com.rachitbhutani.allesample.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.rachitbhutani.allesample.MainViewModel
import com.rachitbhutani.allesample.home.model.ScreenshotItem
import javax.inject.Inject

class ImageRepository @Inject constructor(private val contentResolver: ContentResolver) {

    fun getImagesFromGallery(page: Int): List<ScreenshotItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATA,
        )

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val query =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val selection = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, MainViewModel.LOAD_SIZE)
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%Screenshot%' OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%screenshot%'"
                    )
                }
                contentResolver.query(collection, projection, selection, null)
            } else {
                contentResolver.query(
                    collection,
                    projection,
                    "${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%Screenshot%' OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%screenshot%'",
                    null,
                    sortOrder
                )
            }
        query?.use { cursor ->
            cursor.moveToPosition(page * MainViewModel.LOAD_SIZE)
            val imageList = mutableListOf<ScreenshotItem>()
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            do {
                val id = cursor.getLong(idColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, contentUri)
                val data = cursor.getString(dataColumn)
                val imageData = ScreenshotItem(id, contentUri, bitmap, data)
                imageList.add(imageData)
            } while (cursor.moveToNext())
            return imageList
        }
        return emptyList()
    }
}