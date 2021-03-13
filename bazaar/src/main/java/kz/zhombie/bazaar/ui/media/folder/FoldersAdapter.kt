package kz.zhombie.bazaar.ui.media.folder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.components.view.SquareImageView
import kz.zhombie.bazaar.ui.model.UIFolder
import kz.zhombie.bazaar.utils.inflate

internal class FoldersAdapter constructor(
    private val imageLoader: ImageLoader,
    private val onFolderClicked: (uiFolder: UIFolder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = FoldersAdapter::class.java.simpleName

        private val diffCallback = object : DiffUtil.ItemCallback<UIFolder>() {
            override fun areItemsTheSame(oldItem: UIFolder, newItem: UIFolder): Boolean =
                oldItem.folder.id == newItem.folder.id

            override fun areContentsTheSame(oldItem: UIFolder, newItem: UIFolder): Boolean =
                oldItem == newItem
        }
    }

    private val asyncListDiffer: AsyncListDiffer<UIFolder> by lazy {
        AsyncListDiffer(this, diffCallback)
    }

    fun submitList(data: List<UIFolder>) {
        Logger.d(TAG, "submitList() -> ${data.size}")
        asyncListDiffer.submitList(data)
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    private fun getItem(position: Int): UIFolder = asyncListDiffer.currentList[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.cell_folder))
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

        fun bind(uiFolder: UIFolder) {
            val cover = uiFolder.folder.cover
            if (cover == null) {
                imageView.setImageResource(R.drawable.bg_black)
            } else {
                imageLoader.loadGridItemImage(itemView.context, imageView, cover)
            }

            titleView.text = uiFolder.folder.displayName

            subtitleView.text = "Элементы: ${uiFolder.folder.size}"

            itemView.setOnClickListener { onFolderClicked(uiFolder) }
        }

    }

}