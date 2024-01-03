package com.rachitbhutani.allesample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.rachitbhutani.allesample.databinding.ActivityMainBinding
import com.rachitbhutani.allesample.share.ShareFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUi()
    }

    private fun setupUi() {
        val shareFragment = ShareFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(binding.fragContainer.id, shareFragment)
            .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            DELETE_ITEM_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.activeScreenshotLiveData.postValue(null)
                    viewModel.refreshData()
                }
            }
            else -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.activeScreenshotLiveData.value?.uri?.let {
                        contentResolver.delete(it, null, null)
                        viewModel.refreshData()
                    }
                }
            }
        }
    }

    companion object {
        const val DELETE_ITEM_REQUEST_CODE = 101
        const val DELETE_EXCEPTION_ALLOWED = 102
    }
}