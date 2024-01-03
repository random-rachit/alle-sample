package com.rachitbhutani.allesample.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rachitbhutani.allesample.home.model.ScreenshotItem
import javax.inject.Inject

class GalleryPagingSource @Inject constructor(private val repository: ImageRepository) :
    PagingSource<Int, ScreenshotItem>() {

    private var pageNo = 0

    override fun getRefreshKey(state: PagingState<Int, ScreenshotItem>): Int {
        return 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ScreenshotItem> {
        return try {
            val imageList = repository.getImagesFromGallery(params.key ?: pageNo)
            if (imageList.isEmpty()) {
                return LoadResult.Error(Throwable("End of data"))
            }
            pageNo++
            LoadResult.Page(imageList, null, pageNo)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}