package kz.zhombie.bazaar.ui.presentation

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kz.garage.multimedia.store.model.Content
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.load
import kz.zhombie.bazaar.ui.components.view.SquareImageView
import kz.zhombie.bazaar.utils.inflate
import kz.zhombie.museum.PaintingLoader

internal class AlbumsAdapter : PagingDataAdapter<Content, AlbumsAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Content>() {
            override fun areContentsTheSame(oldItem: Content, newItem: Content): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: Content, newItem: Content): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(parent.inflate(R.layout.bazaar_cell_folder_square))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        holder.unbind()
    }

    internal inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<SquareImageView>(R.id.imageView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val subtitleView = view.findViewById<MaterialTextView>(R.id.subtitleView)

        fun bind(image: Content) {
            imageView.load(image.localFile?.uri ?: image.uri) {
                setCrossfade(PaintingLoader.Request.Crossfade(100, true))
//                setSize(275, 275)
                setScale(PaintingLoader.Request.Scale.FIT)
            }
        }

        fun unbind() {
//            imageView.dispose()
        }

    }

}