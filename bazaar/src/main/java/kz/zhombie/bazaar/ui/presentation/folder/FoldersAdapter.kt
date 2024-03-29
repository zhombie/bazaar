package kz.zhombie.bazaar.ui.presentation.folder

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.marginLeft
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.dispose
import kz.zhombie.bazaar.load
import kz.zhombie.bazaar.ui.components.view.SquareImageView
import kz.zhombie.bazaar.ui.model.UIFolder
import kz.zhombie.bazaar.utils.inflate

internal class FoldersAdapter constructor(
    private val type: Type,
    private val isCoverEnabled: Boolean,
    private val onFolderClicked: (uiFolder: UIFolder) -> Unit,
    private val onLeftOffsetReadyListener: ((leftOffset: Float) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = FoldersAdapter::class.java.simpleName

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UIFolder>() {
            override fun areItemsTheSame(oldItem: UIFolder, newItem: UIFolder): Boolean =
                oldItem.folder.id == newItem.folder.id

            override fun areContentsTheSame(oldItem: UIFolder, newItem: UIFolder): Boolean =
                oldItem == newItem
        }
    }

    enum class Type {
        SQUARE,
        RECTANGLE
    }

    private val asyncListDiffer: AsyncListDiffer<UIFolder> by lazy {
        AsyncListDiffer(this, DIFF_CALLBACK)
    }

    private var leftOffset: Float? = null
        set(value) {
            if (value == null) {
                field = value
            } else {
                if (field != value) {
                    field = value
//                    Logger.debug(TAG, "leftOffset: $value")
                    onLeftOffsetReadyListener?.invoke(value)
                }
            }
        }

    fun submitList(data: List<UIFolder>) {
//        Logger.debug(TAG, "submitList() -> ${data.size}")
        asyncListDiffer.submitList(data)
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    private fun getItem(position: Int): UIFolder = asyncListDiffer.currentList[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (type) {
            Type.SQUARE ->
                SquareViewHolder(parent.inflate(R.layout.bazaar_cell_folder_square))
            Type.RECTANGLE ->
                RectangleViewHolder(parent.inflate(R.layout.bazaar_cell_folder_rectangle))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SquareViewHolder) {
            holder.bind(getItem(position))
        } else if (holder is RectangleViewHolder) {
            holder.bind(getItem(position))
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is SquareViewHolder) {
            holder.unbind()
        } else if (holder is RectangleViewHolder) {
            holder.unbind()
        }
    }

    private inner class SquareViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: SquareImageView = view.findViewById(R.id.imageView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val subtitleView = view.findViewById<MaterialTextView>(R.id.subtitleView)

        fun bind(uiFolder: UIFolder) {
            if (leftOffset == null) {
                titleView.post {
                    leftOffset = titleView.x + titleView.marginLeft
                }
            }

            if (isCoverEnabled) {
                val cover = uiFolder.folder.cover
                if (cover == null) {
                    imageView.setImageResource(R.drawable.bazaar_bg_black)
                } else {
                    imageView.load(cover) {
                        setErrorDrawable(R.drawable.bazaar_bg_black)
                        setPlaceholderDrawable(R.drawable.bazaar_bg_black)
                        setSize(300, 300)
                    }
                }

                if (imageView.visibility != View.VISIBLE) {
                    imageView.visibility = View.VISIBLE
                }
            } else {
                if (imageView.visibility != View.GONE) {
                    imageView.visibility = View.GONE
                }
            }

            titleView.text = uiFolder.getDisplayName(itemView.context)

            subtitleView.text = if (uiFolder.folder.itemsCount == 0) {
                itemView.context.getString(R.string.bazaar_nothing_found)
            } else {
                itemView.context.resources.getQuantityString(
                    R.plurals.bazaar_folder_items,
                    uiFolder.folder.itemsCount,
                    uiFolder.folder.itemsCount
                )
            }

            itemView.setOnClickListener { onFolderClicked(uiFolder) }
        }

        fun unbind() {
            imageView.dispose()
        }

    }

    private inner class RectangleViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ShapeableImageView = view.findViewById(R.id.imageView)
        private val contentView = view.findViewById<LinearLayout>(R.id.contentView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val subtitleView = view.findViewById<MaterialTextView>(R.id.subtitleView)

        fun bind(uiFolder: UIFolder) {
            if (leftOffset == null) {
                contentView.post {
                    leftOffset = contentView.x
                }
            }

            titleView.text = uiFolder.getDisplayName(itemView.context)

            subtitleView.text = if (uiFolder.folder.itemsCount == 0) {
                itemView.context.getString(R.string.bazaar_nothing_found)
            } else {
                itemView.context.resources.getQuantityString(
                    R.plurals.bazaar_folder_items,
                    uiFolder.folder.itemsCount,
                    uiFolder.folder.itemsCount
                )
            }

            itemView.setOnClickListener { onFolderClicked(uiFolder) }
        }

        fun unbind() {
            imageView.dispose()
        }

    }

}