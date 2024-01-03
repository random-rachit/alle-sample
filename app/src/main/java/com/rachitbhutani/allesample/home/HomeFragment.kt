package com.rachitbhutani.allesample.home

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rachitbhutani.allesample.MainActivity
import com.rachitbhutani.allesample.MainActivity.Companion.DELETE_ITEM_REQUEST_CODE
import com.rachitbhutani.allesample.MainViewModel
import com.rachitbhutani.allesample.R
import com.rachitbhutani.allesample.databinding.FragmentShareBinding
import com.rachitbhutani.allesample.databinding.LabelItemBinding
import com.rachitbhutani.allesample.home.model.ScreenshotItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.io.File


@AndroidEntryPoint
class HomeFragment : Fragment(), ScreenshotListCallback {

    private lateinit var binding: FragmentShareBinding
    private lateinit var adapter: ScreenshotListAdapter

    private var deletingAtPos: Int? = null

    private val viewModel: MainViewModel by activityViewModels()

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            if (isGranted[READ_MEDIA_IMAGES] == true || isGranted[READ_EXTERNAL_STORAGE] == true) loadImages()
            else if (isGranted[WRITE_EXTERNAL_STORAGE] == true) deleteActiveScreenshot()
            else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShareBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        setupObservers()
        setupListeners()
        if (checkAndRequestReadPermissions()) loadImages()
    }

    private fun checkAndRequestReadPermissions(): Boolean {

        val requiredPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                READ_MEDIA_IMAGES
            } else {
                READ_EXTERNAL_STORAGE
            }
        return if (ContextCompat.checkSelfPermission(
                requireContext(),
                requiredPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            requestPermissions.launch(arrayOf(requiredPermission))
            false
        }
    }

    private fun setupListeners() {
        binding.run {
            tvDelete.setOnClickListener {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                )
                    deleteActiveScreenshot()
                else requestPermissions.launch(arrayOf(WRITE_EXTERNAL_STORAGE))
            }
            tvInfo.setOnClickListener {
                if (llCollections.isVisible) {
                    showDescription(false)
                } else viewModel.loadImageInfo()
            }
            tvShare.setOnClickListener {
                shareImage()
            }
        }
    }

    private fun shareImage() {
        viewModel.activeScreenshotLiveData.value?.uri?.let {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.setDataAndNormalize(it)
            try {
                sharingIntent.type = "image/*"
                sharingIntent.putExtra(Intent.EXTRA_STREAM, it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            requireActivity().startActivity(
                Intent.createChooser(
                    sharingIntent,
                    getString(R.string.share_the_screenshot)
                )
            )
        }
    }

    private fun deleteActiveScreenshot() {
        viewModel.activeScreenshotLiveData.value?.let {
            deletingAtPos = adapter.snapshot().indexOf(it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                trashScreenshot(it.uri)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.path?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        try {
                            // Perform the deletion operation
                            requireActivity().contentResolver.delete(it.uri!!, null, null)
                            deletingAtPos?.let { pos -> adapter.notifyItemRemoved(pos) }
                            viewModel.refreshData()
                        } catch (e: RecoverableSecurityException) {
                            val intentSender: IntentSender = e.userAction.actionIntent.intentSender
                            startIntentSenderForResult(
                                intentSender,
                                MainActivity.DELETE_EXCEPTION_ALLOWED,
                                null,
                                0,
                                0,
                                0,
                                null
                            )
                        }
                    }
                }
            } else {
                it.path?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        // Perform the deletion operation
                        requireActivity().contentResolver.delete(it.uri!!, null, null)
                        deletingAtPos?.let { pos -> adapter.notifyItemRemoved(pos) }
                        viewModel.refreshData()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun trashScreenshot(uri: Uri?) {
        val intent = MediaStore.createTrashRequest(
            requireActivity().contentResolver,
            listOf(uri),
            true
        )
        requireActivity().startIntentSenderForResult(
            intent.intentSender,
            DELETE_ITEM_REQUEST_CODE,
            null,
            0,
            0,
            0
        )
    }

    private fun loadImages() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.galleryData.collectLatest {
                adapter.submitData(it)
            }
        }
    }

    private fun setupObservers() {
        viewModel.activeScreenshotLiveData.observe(viewLifecycleOwner) {
            deletingAtPos?.let { pos ->
                adapter.notifyItemRemoved(pos)
                deletingAtPos = null
            }
            Glide.with(requireContext()).load(it?.uri).into(binding.ivActiveScreenshot)
            showDescription(false)
        }
        viewModel.descriptionLiveData.observe(viewLifecycleOwner) {
            it?.let {
                setDescription(it)
                showDescription(true)
            }
        }
        viewModel.labelLivedata.observe(viewLifecycleOwner) {
            binding.run {
                llCollections.removeAllViews()
                it.forEach {
                    val labelBinding = LabelItemBinding.inflate(layoutInflater)
                    labelBinding.root.text = it
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(8)
                    labelBinding.root.layoutParams = params
                    llCollections.addView(labelBinding.root)
                }
            }
        }
    }

    private fun setDescription(description: String) {
        binding.tvDesc.text = description
    }

    private fun showDescription(show: Boolean) {
        binding.llDesc.children.forEach {
            if (it.id != binding.ivActiveScreenshot.id) {
                it.isVisible = show
                binding.tvInfo.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (show) R.color.yellow
                        else android.R.color.white
                    )
                )
            }
        }
    }

    private fun setupUi() {
        binding.run {
            adapter = ScreenshotListAdapter(requireContext(), this@HomeFragment)
            rvScreenshots.adapter = adapter
            rvScreenshots.layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            showDescription(false)
        }
    }

    override fun onScreenshotSelected(screenshot: ScreenshotItem?, position: Int) {
        viewModel.activeScreenshotLiveData.value?.let {
            it.isActive = false
            val oldPos =
                adapter.snapshot().items.indexOf(viewModel.activeScreenshotLiveData.value)
            adapter.markActive(oldPos, false)
        }
        screenshot?.isActive = true
        adapter.markActive(position, true)
        viewModel.activeScreenshotLiveData.value = screenshot
        showDescription(false)
    }
}