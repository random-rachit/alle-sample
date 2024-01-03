package com.rachitbhutani.allesample.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rachitbhutani.allesample.R
import com.rachitbhutani.allesample.databinding.ScreenshotItemViewBinding
import com.rachitbhutani.allesample.home.model.ScreenshotItem

class ScreenshotListAdapter(
    private val context: Context,
    private val listener: ScreenshotListCallback
) : PagingDataAdapter<ScreenshotItem, ScreenshotListAdapter.ScreenshotViewHolder>(DIFF_CALLBACK) {

    fun markActive(pos: Int, active: Boolean) {
        getItem(pos)?.isActive = active
        notifyItemChanged(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenshotViewHolder {
        return ScreenshotViewHolder(
            ScreenshotItemViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ScreenshotViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    inner class ScreenshotViewHolder(private val binding: ScreenshotItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindTo(screenshot: ScreenshotItem?) {
            binding.run {
                screenshot?.uri?.let {
                    Glide.with(root.context).load(it).placeholder(
                        ContextCompat.getDrawable(
                            root.context,
                            android.R.color.holo_red_dark
                        )
                    ).into(ivScreenshot)
                }
                ivScreenshot.setBackgroundColor(
                    ContextCompat.getColor(
                        root.context,
                        if (screenshot?.isActive == true) R.color.yellow
                        else android.R.color.transparent
                    )
                )
                root.setOnClickListener {
                    listener.onScreenshotSelected(
                        screenshot,
                        bindingAdapterPosition
                    )
                }
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ScreenshotItem>() {
            override fun areItemsTheSame(
                oldItem: ScreenshotItem,
                newItem: ScreenshotItem
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ScreenshotItem,
                newItem: ScreenshotItem
            ): Boolean {
                return oldItem.uri == newItem.uri
            }
        }
    }
}

interface ScreenshotListCallback {
    fun onScreenshotSelected(screenshot: ScreenshotItem?, position: Int)
}

