package kz.zhombie.bazaar.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.ImageLoader
import kz.zhombie.bazaar.core.Logger
import kz.zhombie.bazaar.ui.components.SquareImageView
import kz.zhombie.bazaar.ui.model.UIAlbum

internal class AlbumsAdapter constructor(
    private val imageLoader: ImageLoader
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = AlbumsAdapter::class.java.simpleName

        private val diffCallback = object : DiffUtil.ItemCallback<UIAlbum>() {
            override fun areItemsTheSame(oldItem: UIAlbum, newItem: UIAlbum): Boolean =
                oldItem.album.id == newItem.album.id

            override fun areContentsTheSame(oldItem: UIAlbum, newItem: UIAlbum): Boolean =
                oldItem == newItem
        }
    }

    private val asyncListDiffer: AsyncListDiffer<UIAlbum> by lazy {
        AsyncListDiffer(this, diffCallback)
    }

    fun submitList(data: List<UIAlbum>) {
        Logger.d(TAG, "submitList() -> ${data.size}")
        asyncListDiffer.submitList(data)
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    private fun getItem(position: Int): UIAlbum = asyncListDiffer.currentList[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.cell_album, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<SquareImageView>(R.id.imageView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val subtitleView = view.findViewById<MaterialTextView>(R.id.subtitleView)

        fun bind(uiAlbum: UIAlbum) {
            val cover = uiAlbum.album.cover
            if (cover == null) {
                imageView.setImageResource(R.drawable.ic_placeholder_black)
            } else {
                imageLoader.loadGridItemImage(itemView.context, imageView, cover)
            }

            titleView.text = uiAlbum.album.displayName

            subtitleView.text = "Элементы: ${uiAlbum.album.size}"
        }

    }

}