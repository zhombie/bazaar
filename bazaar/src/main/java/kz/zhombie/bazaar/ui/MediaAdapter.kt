package kz.zhombie.bazaar.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.ImageLoader
import kz.zhombie.bazaar.core.Logger
import kz.zhombie.bazaar.core.ViewHolderException
import kz.zhombie.bazaar.model.Image
import kz.zhombie.bazaar.model.Video
import kz.zhombie.bazaar.ui.model.UIMedia

internal class MediaAdapter constructor(
    private val imageLoader: ImageLoader,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = MediaAdapter::class.java.simpleName

        private val diffCallback = object : DiffUtil.ItemCallback<UIMedia>() {
            override fun areItemsTheSame(oldItem: UIMedia, newItem: UIMedia): Boolean =
                oldItem.media.id == newItem.media.id

            override fun areContentsTheSame(oldItem: UIMedia, newItem: UIMedia): Boolean =
                oldItem == newItem

            override fun getChangePayload(oldItem: UIMedia, newItem: UIMedia): Any? {
                return when {
                    oldItem.isSelected != newItem.isSelected -> "toggle_selected"
                    else -> null
                }
            }
        }
    }

    private object ViewType {
        const val IMAGE = 100
        const val VIDEO = 101
    }

    private val asyncListDiffer: AsyncListDiffer<UIMedia> by lazy {
        AsyncListDiffer(this, diffCallback)
    }

    fun submitList(data: List<UIMedia>) {
        Logger.d(TAG, "submitList() -> ${data.size}")
        asyncListDiffer.submitList(data)
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    private fun getItem(position: Int): UIMedia = asyncListDiffer.currentList[position]

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).media) {
            is Image -> ViewType.IMAGE
            is Video -> ViewType.VIDEO
            else -> super.getItemViewType(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.IMAGE -> {
                ImageViewHolder(
                    LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.cell_image, parent, false)
                )
            }
            ViewType.VIDEO -> {
                VideoViewHolder(
                    LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.cell_video, parent, false)
                )
            }
            else ->
                throw ViewHolderException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ImageViewHolder -> {
                holder.bind(item)
            }
            is VideoViewHolder -> {
                if (item.media is Video) {
                    holder.bind(item.media, item.isSelected)
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        Logger.d(TAG, "payloads: $payloads")
        if (payloads.isNullOrEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            var isProcessed = false
            payloads.forEach {
                if (it == "toggle_selected") {
                    when (holder) {
                        is ImageViewHolder -> {
                            if (!isProcessed) {
                                isProcessed = true
                            }
                            holder.toggleSelection(getItem(position))
                        }
                    }
                }
            }
            if (!isProcessed) {
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

    private inner class ImageViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)
        private val checkbox = view.findViewById<MaterialButton>(R.id.checkbox)

        fun bind(uiMedia: UIMedia) {
            imageLoader.loadImage(itemView.context, imageView, uiMedia.media.uri)

            if (uiMedia.isSelected) {
                imageView.scaleX = 0.9F
                imageView.scaleY = 0.9F

                checkbox.setIconResource(R.drawable.ic_checked)
            } else {
                imageView.scaleX = 1.0F
                imageView.scaleY = 1.0F

                checkbox.setIconResource(R.drawable.ic_unchecked)
            }

            checkbox.setOnClickListener {
                callback.onImageCheckboxClicked(uiMedia)
            }
        }

        fun toggleSelection(uiMedia: UIMedia) {
            if (uiMedia.isSelected) {
                imageView.animate()
                    .setDuration(100L)
                    .scaleX(0.9F)
                    .scaleY(0.9F)
                    .withStartAction {
                        checkbox.setIconResource(R.drawable.ic_checked)
                    }
                    .start()
            } else {
                imageView.animate()
                    .setDuration(100L)
                    .scaleX(1.0F)
                    .scaleY(1.0F)
                    .withStartAction {
                        checkbox.setIconResource(R.drawable.ic_unchecked)
                    }
                    .start()
            }
        }
    }

    private inner  class VideoViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)
        private val checkbox = view.findViewById<MaterialButton>(R.id.checkbox)

        fun bind(video: Video, isSelected: Boolean) {
            imageLoader.loadImage(itemView.context, imageView, video.uri)

            if (isSelected) {
                checkbox.setIconResource(R.drawable.ic_checked)
            } else {
                checkbox.setIconResource(R.drawable.ic_unchecked)
            }
        }
    }

    interface Callback {
        fun onImageCheckboxClicked(uiMedia: UIMedia)
        fun onVideoCheckboxClicked(position: Int, video: Video, isSelected: Boolean)
    }

}