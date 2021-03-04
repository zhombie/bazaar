package kz.zhombie.bazaar.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.ImageLoader
import kz.zhombie.bazaar.core.Logger
import kz.zhombie.bazaar.core.ViewHolderException
import kz.zhombie.bazaar.model.Entity
import kz.zhombie.bazaar.model.Image
import kz.zhombie.bazaar.model.Media
import kz.zhombie.bazaar.model.Video

internal class MediaAdapter constructor(
    private val imageLoader: ImageLoader
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = MediaAdapter::class.java.simpleName

        private val diffCallback = object : DiffUtil.ItemCallback<Media>() {
            override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean =
                oldItem.id == newItem.id && oldItem.uri == newItem.uri
        }
    }

    private object ViewType {
        const val IMAGE = 100
        const val VIDEO = 101
    }

    private val asyncListDiffer: AsyncListDiffer<Media> by lazy {
        AsyncListDiffer(this, diffCallback)
    }

    fun submitList(data: List<Media>) {
        Logger.d(TAG, "submitList() -> ${data.size}")
        asyncListDiffer.submitList(data)
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    private fun getItem(position: Int): Media = asyncListDiffer.currentList[position]

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
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
                if (item is Image) {
                    holder.bind(item)
                }
            }
            is VideoViewHolder -> {
                if (item is Video) {
                    holder.bind(item)
                }
            }
        }
    }

    private inner class ImageViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)

        fun bind(image: Image) {
            imageLoader.loadImage(itemView.context, imageView, image.uri)
        }
    }

    private inner  class VideoViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)

        fun bind(video: Video) {
            imageLoader.loadImage(itemView.context, imageView, video.uri)
        }
    }

}