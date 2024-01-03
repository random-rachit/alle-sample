package com.rachitbhutani.allesample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.rachitbhutani.allesample.data.GalleryPagingSource
import com.rachitbhutani.allesample.data.ImageRepository
import com.rachitbhutani.allesample.home.model.ScreenshotItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(repository: ImageRepository) : ViewModel() {

    val activeScreenshotLiveData: MutableLiveData<ScreenshotItem?> = MutableLiveData(null)

    val descriptionLiveData = MutableLiveData(activeScreenshotLiveData.value?.description?.description)
    val labelLivedata = MutableLiveData(activeScreenshotLiveData.value?.description?.labels.orEmpty())

    private val pagingSource = GalleryPagingSource(repository)

    val galleryData = Pager(
        PagingConfig(LOAD_SIZE, INITIAL_LOAD_SIZE)
    ) { pagingSource }.flow

    fun refreshData() = viewModelScope.launch {
        pagingSource.load(PagingSource.LoadParams.Refresh(0, LOAD_SIZE, false))
    }

    fun loadImageInfo() = viewModelScope.launch(Dispatchers.IO) {
        activeScreenshotLiveData.value?.bitmap?.let { bitmap ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage).addOnSuccessListener { text ->
                descriptionLiveData.postValue(text.text.takeIf { it.isNotEmpty() } ?: "No Text")
            }
            labeler.process(inputImage).addOnSuccessListener { list ->
                labelLivedata.postValue(list.take(3).sortedByDescending { it.confidence }.map { it.text })
            }
        }
    }

    companion object {
        const val INITIAL_LOAD_SIZE = 10
        const val LOAD_SIZE = 10
    }
}