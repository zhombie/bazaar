package kz.zhombie.bazaar.ui.presentation.visual

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.garage.multimedia.store.model.Image
import kz.garage.multimedia.store.model.Video
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.core.exception.ViewHolderException
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.dispose
import kz.zhombie.bazaar.load
import kz.zhombie.bazaar.ui.components.view.CheckBoxButton
import kz.zhombie.bazaar.ui.components.view.SquareImageView
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.utils.inflate

internal class VisualMediaAdapter constructor(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = VisualMediaAdapter::class.java.simpleName

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UIMedia>() {
            override fun areItemsTheSame(oldItem: UIMedia, newItem: UIMedia): Boolean =
                oldItem.media.id == newItem.media.id && oldItem.media.uri == newItem.media.uri

            override fun areContentsTheSame(oldItem: UIMedia, newItem: UIMedia): Boolean =
                oldItem == newItem

            override fun getChangePayload(oldItem: UIMedia, newItem: UIMedia): Any? = when {
                oldItem.isSelectable != newItem.isSelectable -> PayloadKey.TOGGLE_SELECTION_ABILITY
                oldItem.isSelected != newItem.isSelected -> PayloadKey.TOGGLE_SELECTION
                oldItem.isVisible != newItem.isVisible -> PayloadKey.TOGGLE_VISIBILITY
                else -> null
            }
        }
    }

    private object ViewType {
        const val IMAGE = 100
        const val VIDEO = 101
    }

    private object PayloadKey {
        const val TOGGLE_SELECTION_ABILITY = "toggle_selection_ability"
        const val TOGGLE_SELECTION = "toggle_selection"
        const val TOGGLE_VISIBILITY = "toggle_visibility"
    }

    private val asyncListDiffer: AsyncListDiffer<UIMedia> by lazy {
        AsyncListDiffer(this, DIFF_CALLBACK)
    }

    fun submitList(uiMedia: List<UIMedia>) {
        Logger.debug(TAG, "submitList() -> ${uiMedia.size}")
        asyncListDiffer.submitList(uiMedia)
    }

    fun setListListener(callback: () -> Unit) {
        asyncListDiffer.addListListener { _, _ -> callback() }
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    private fun getItem(position: Int): UIMedia = asyncListDiffer.currentList[position]

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item.media) {
            is Image -> ViewType.IMAGE
            is Video -> ViewType.VIDEO
            else -> super.getItemViewType(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.IMAGE ->
                ImageViewHolder(view = parent.inflate(R.layout.bazaar_cell_image))
            ViewType.VIDEO ->
                VideoViewHolder(view = parent.inflate(R.layout.bazaar_cell_video))
            else ->
                throw ViewHolderException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ImageViewHolder -> if (item.media is Image) holder.bind(item)
            is VideoViewHolder -> if (item.media is Video) holder.bind(item)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        Logger.debug(TAG, "payloads: $payloads")
        if (payloads.isNullOrEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            var isProcessed = false
            val item = getItem(position)
            if (item.media !is Image || item.media !is Video) {
                super.onBindViewHolder(holder, position, payloads)
                return
            }
            payloads.forEach {
                when (it) {
                    PayloadKey.TOGGLE_SELECTION_ABILITY -> {
                        if (holder is ImageViewHolder) {
                            if (!isProcessed) isProcessed = true
                            holder.toggleSelectionAbility(item)
                        } else if (holder is VideoViewHolder) {
                            if (!isProcessed) isProcessed = true
                            holder.toggleSelectionAbility(item)
                        }
                    }
                    PayloadKey.TOGGLE_SELECTION -> {
                        if (holder is ImageViewHolder) {
                            if (!isProcessed) isProcessed = true
                            holder.toggleSelection(item, animate = true)
                        } else if (holder is VideoViewHolder) {
                            if (!isProcessed) isProcessed = true
                            holder.toggleSelection(item, animate = true)
                        }
                    }
                    PayloadKey.TOGGLE_VISIBILITY -> {
                        if (holder is ImageViewHolder) {
                            if (!isProcessed) isProcessed = true
//                            holder.toggleVisibility(item)
                        } else if (holder is VideoViewHolder) {
                            if (!isProcessed) isProcessed = true
//                            holder.toggleVisibility(item)
                        }
                    }
                }
            }
            if (!isProcessed) {
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ImageViewHolder) {
            holder.unbind()
        } else if (holder is VideoViewHolder) {
            holder.unbind()
        }
    }

    private inner class ImageViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: SquareImageView = view.findViewById(R.id.imageView)
        private val checkBoxButton: CheckBoxButton = view.findViewById(R.id.checkBoxButton)

        fun bind(uiMedia: UIMedia) {
            val image: Image = if (uiMedia.media is Image) uiMedia.media else return

            imageView.load(image.availableFileUri) {
                setErrorDrawable(R.drawable.bazaar_bg_black)
                setPlaceholderDrawable(R.drawable.bazaar_bg_black)
                setSize(300, 300)
            }

            toggleSelectionAbility(uiMedia)

            toggleSelection(uiMedia, animate = false)

//            toggleVisibility(uiMedia)

            imageView.setOnClickListener {
                callback.onImageClicked(imageView, uiMedia)
            }

            checkBoxButton.setOnClickListener {
                callback.onImageCheckboxClicked(uiMedia)
            }
        }

        fun unbind() {
            imageView.dispose()
        }

        fun toggleSelectionAbility(uiMedia: UIMedia) {
            if (uiMedia.isSelectable) {
                if (checkBoxButton.visibility != View.VISIBLE) {
                    checkBoxButton.visibility = View.VISIBLE
                }
                imageView.foreground = null
            } else {
                if (checkBoxButton.visibility != View.GONE) {
                    checkBoxButton.visibility = View.GONE
                }
                imageView.foreground = AppCompatResources.getDrawable(itemView.context, R.drawable.bazaar_bg_rounded_alpha_black)
            }
        }

        fun toggleSelection(uiMedia: UIMedia, animate: Boolean) {
            if (animate) {
                if (uiMedia.isSelected) {
                    imageView.animate()
                        .setDuration(100L)
                        .scaleX(0.9F)
                        .scaleY(0.9F)
                        .withStartAction {
                            checkBoxButton.setCheckedDrawable()
                        }
                        .start()
                } else {
                    imageView.animate()
                        .setDuration(100L)
                        .scaleX(1.0F)
                        .scaleY(1.0F)
                        .withStartAction {
                            checkBoxButton.setUncheckedDrawable()
                        }
                        .start()
                }
            } else {
                if (uiMedia.isSelected) {
                    imageView.scaleX = 0.9F
                    imageView.scaleY = 0.9F

                    checkBoxButton.setCheckedDrawable()
                } else {
                    imageView.scaleX = 1.0F
                    imageView.scaleY = 1.0F

                    checkBoxButton.setUncheckedDrawable()
                }
            }
        }

//        fun toggleVisibility(uiMedia: UIMedia) {
//            if (uiMedia.isVisible) {
//                if (imageView.visibility != View.VISIBLE) {
//                    imageView.visibility = View.VISIBLE
//                }
//            } else {
//                if (imageView.visibility != View.INVISIBLE) {
//                    imageView.visibility = View.INVISIBLE
//                }
//            }
//        }
    }

    private inner class VideoViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: SquareImageView = view.findViewById(R.id.imageView)
        private val checkBoxButton: CheckBoxButton = view.findViewById(R.id.checkBoxButton)
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)

        fun bind(uiMedia: UIMedia) {
            val video: Video = if (uiMedia.media is Video) uiMedia.media else return

            imageView.load(video.availableFileUri) {
                setErrorDrawable(R.drawable.bazaar_bg_black)
                setPlaceholderDrawable(R.drawable.bazaar_bg_black)
                setSize(300, 300)
            }

            toggleSelectionAbility(uiMedia)

            toggleSelection(uiMedia, animate = true)

//            toggleVisibility(uiMedia)

            val displayDuration = uiMedia.getDisplayDuration()
            textView?.text = if (displayDuration.isNullOrBlank()) {
                itemView.context.getString(R.string.bazaar_video)
            } else {
                displayDuration
            }

            imageView.setOnClickListener {
                callback.onVideoClicked(imageView, uiMedia)
            }

            checkBoxButton.setOnClickListener {
                callback.onVideoCheckboxClicked(uiMedia)
            }
        }

        fun unbind() {
            imageView.dispose()
        }

        fun toggleSelectionAbility(uiMedia: UIMedia) {
            if (uiMedia.isSelectable) {
                if (checkBoxButton.visibility != View.VISIBLE) {
                    checkBoxButton.visibility = View.VISIBLE
                }
                imageView.foreground = null
            } else {
                if (checkBoxButton.visibility != View.GONE) {
                    checkBoxButton.visibility = View.GONE
                }
                imageView.foreground = AppCompatResources.getDrawable(itemView.context, R.drawable.bazaar_bg_rounded_alpha_black)
            }
        }

        fun toggleSelection(uiMedia: UIMedia, animate: Boolean) {
            if (animate) {
                if (uiMedia.isSelected) {
                    imageView.animate()
                        .setDuration(100L)
                        .scaleX(0.9F)
                        .scaleY(0.9F)
                        .withStartAction {
                            checkBoxButton.setCheckedDrawable()
                        }
                        .start()
                } else {
                    imageView.animate()
                        .setDuration(100L)
                        .scaleX(1.0F)
                        .scaleY(1.0F)
                        .withStartAction {
                            checkBoxButton.setUncheckedDrawable()
                        }
                        .start()
                }
            } else {
                if (uiMedia.isSelected) {
                    imageView.scaleX = 0.9F
                    imageView.scaleY = 0.9F

                    checkBoxButton.setCheckedDrawable()
                } else {
                    imageView.scaleX = 1.0F
                    imageView.scaleY = 1.0F

                    checkBoxButton.setUncheckedDrawable()
                }
            }
        }

//        fun toggleVisibility(uiMedia: UIMedia) {
//            if (uiMedia.isVisible) {
//                if (imageView.visibility != View.VISIBLE) {
//                    imageView.visibility = View.VISIBLE
//                }
//            } else {
//                if (imageView.visibility != View.INVISIBLE) {
//                    imageView.visibility = View.INVISIBLE
//                }
//            }
//        }
    }

    interface Callback {
        fun onImageClicked(imageView: ShapeableImageView, uiMedia: UIMedia)
        fun onImageCheckboxClicked(uiMedia: UIMedia)

        fun onVideoClicked(imageView: ShapeableImageView, uiMedia: UIMedia)
        fun onVideoCheckboxClicked(uiMedia: UIMedia)
    }

}