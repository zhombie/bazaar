package kz.zhombie.bazaar.ui.media.audible

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.model.Audio
import kz.zhombie.bazaar.core.exception.ViewHolderException
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.model.UIMultimedia
import kz.zhombie.bazaar.utils.inflate

internal class AudiosAdapter constructor(
    private val imageLoader: ImageLoader,
    private val callback: Callback,
    private val onLeftOffsetReadyListener: ((leftOffset: Float) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = AudiosAdapter::class.java.simpleName

        private val diffCallback = object : DiffUtil.ItemCallback<UIMultimedia>() {
            override fun areItemsTheSame(oldItem: UIMultimedia, newItem: UIMultimedia): Boolean =
                oldItem.multimedia.id == newItem.multimedia.id

            override fun areContentsTheSame(oldItem: UIMultimedia, newItem: UIMultimedia): Boolean =
                oldItem == newItem

            override fun getChangePayload(oldItem: UIMultimedia, newItem: UIMultimedia): Any? = when {
                oldItem.isSelectable != newItem.isSelectable -> PayloadKey.TOGGLE_SELECTION_ABILITY
                oldItem.isSelected != newItem.isSelected -> PayloadKey.TOGGLE_SELECTION
                oldItem.isVisible != newItem.isVisible -> PayloadKey.TOGGLE_VISIBILITY
                else -> null
            }
        }
    }

    private object ViewType {
        const val AUDIO = 100
    }

    private object PayloadKey {
        const val TOGGLE_SELECTION_ABILITY = "toggle_selection_ability"
        const val TOGGLE_SELECTION = "toggle_selection"
        const val TOGGLE_VISIBILITY = "toggle_visibility"
    }

    private val asyncListDiffer: AsyncListDiffer<UIMultimedia> by lazy {
        AsyncListDiffer(this, diffCallback)
    }

    private var leftOffset: Float? = null
        set(value) {
            field = value
            if (value != null) {
                onLeftOffsetReadyListener?.invoke(value)
            }
        }

    fun submitList(uiMultimedia: List<UIMultimedia>) {
        Logger.d(TAG, "submitList() -> ${uiMultimedia.size}")
        asyncListDiffer.submitList(uiMultimedia)
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    private fun getItem(position: Int): UIMultimedia = asyncListDiffer.currentList[position]

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.isAudio() -> ViewType.AUDIO
            else -> super.getItemViewType(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.AUDIO ->
                ViewHolder(parent.inflate(R.layout.cell_audio))
            else ->
                throw ViewHolderException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when {
            item.isAudio() -> if (holder is ViewHolder) holder.bind(item)
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
            val item = getItem(position)
            if (!item.isAudio()) {
                super.onBindViewHolder(holder, position, payloads)
                return
            }
            if (holder !is ViewHolder) {
                super.onBindViewHolder(holder, position, payloads)
                return
            }
            payloads.forEach {
                when (it) {
                    PayloadKey.TOGGLE_SELECTION_ABILITY -> {
                        if (!isProcessed) isProcessed = true
                        holder.toggleSelectionAbility(item)
                    }
                    PayloadKey.TOGGLE_SELECTION -> {
                        if (!isProcessed) isProcessed = true
                        holder.toggleSelection(item)
                    }
                    PayloadKey.TOGGLE_VISIBILITY -> {
                        if (!isProcessed) isProcessed = true
                        holder.toggleVisibility(item)
                    }
                }
            }
            if (!isProcessed) {
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val checkbox = view.findViewById<MaterialButton>(R.id.checkbox)
        private val contentView = view.findViewById<LinearLayout>(R.id.contentView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val subtitleView = view.findViewById<MaterialTextView>(R.id.subtitleView)
        private val durationView = view.findViewById<MaterialTextView>(R.id.durationView)

        fun bind(uiMultimedia: UIMultimedia) {
            Logger.d(TAG, "uiMultimedia: $uiMultimedia")

            if (leftOffset == null) {
                contentView.post {
                    leftOffset = contentView.x
                }
            }

            toggleSelectionAbility(uiMultimedia)

            if (uiMultimedia.isSelected) {
                checkbox.scaleX = 1.0F
                checkbox.scaleY = 1.0F

                checkbox.visibility = View.VISIBLE
            } else {
                checkbox.scaleX = 0.0F
                checkbox.scaleY = 0.0F

                checkbox.visibility = View.INVISIBLE
            }

            var title = uiMultimedia.multimedia.displayName
            if (uiMultimedia.multimedia is Audio) {
                if (!uiMultimedia.multimedia.album?.artist.isNullOrBlank()) {
                    title = uiMultimedia.multimedia.album?.artist + " - " + uiMultimedia.multimedia.displayName
                }
            }

            titleView.text = title

            val folderDisplayName = uiMultimedia.multimedia.folderDisplayName
            if (folderDisplayName.isNullOrBlank()) {
                subtitleView.text = null
                subtitleView.visibility = View.GONE
            } else {
                subtitleView.text = folderDisplayName
                subtitleView.visibility = View.VISIBLE
            }

            val displayDuration = uiMultimedia.getDisplayDuration()
            if (displayDuration.isNullOrBlank()) {
                durationView.text = null
                durationView.visibility = View.GONE
            } else {
                durationView.text = displayDuration
                durationView.visibility = View.VISIBLE
            }

            itemView.setOnClickListener { callback.onAudioClicked(uiMultimedia) }
        }

        fun toggleSelectionAbility(uiMultimedia: UIMultimedia) {
            if (uiMultimedia.isSelectable) {
                itemView.isEnabled = true
                itemView.foreground = null
            } else {
                itemView.isEnabled = false
                itemView.foreground = AppCompatResources.getDrawable(itemView.context, R.drawable.bg_alpha_black)
            }
        }

        fun toggleSelection(uiMultimedia: UIMultimedia) {
            if (uiMultimedia.isSelected) {
                checkbox.animate()
                    .setDuration(100L)
                    .scaleX(1.0F)
                    .scaleY(1.0F)
                    .withStartAction {
                        checkbox.visibility = View.VISIBLE
                    }
                    .start()
            } else {
                checkbox.animate()
                    .setDuration(100L)
                    .scaleX(0.0F)
                    .scaleY(0.0F)
                    .withEndAction {
                        checkbox.visibility = View.INVISIBLE
                    }
                    .start()
            }
        }

        fun toggleVisibility(uiMultimedia: UIMultimedia) {
        }

    }

    interface Callback {
        fun onAudioClicked(uiMultimedia: UIMultimedia)
    }

}