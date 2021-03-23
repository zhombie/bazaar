package kz.zhombie.bazaar.ui.media.audible

import android.os.Bundle
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
import kz.zhombie.bazaar.core.exception.ViewHolderException
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.components.view.CheckBoxButton
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
        const val TOGGLE_PLAYING = "toggle_playing"
    }

    private val asyncListDiffer: AsyncListDiffer<UIMultimedia> by lazy {
        AsyncListDiffer(this, diffCallback).also { asyncListDiffer ->
            asyncListDiffer.addListListener { previousList, currentList ->
                if (currentPlayingAudioPosition < 0) return@addListListener
                if (previousList.isNullOrEmpty()) return@addListListener
                if (currentPlayingAudioPosition > previousList.size) return@addListListener
                if (currentList.isNullOrEmpty()) return@addListListener

                val uiMultimedia = previousList[currentPlayingAudioPosition]
                val index = currentList.indexOfFirst { it.multimedia.id == uiMultimedia.multimedia.id }
                currentPlayingAudioPosition = index
            }
        }
    }

    private var leftOffset: Float? = null
        set(value) {
            if (value == null) {
                field = value
            } else {
                if (field != value) {
                    field = value
                    Logger.d(TAG, "leftOffset: $value")
                    onLeftOffsetReadyListener?.invoke(value)
                }
            }
        }

    private var currentPlayingAudioPosition: Int = -1

    fun getList(): List<UIMultimedia> = asyncListDiffer.currentList

    fun submitList(uiMultimedia: List<UIMultimedia>) {
        Logger.d(TAG, "submitList() -> ${uiMultimedia.size}")
        asyncListDiffer.submitList(uiMultimedia)
    }

    fun setPlaying(uiMultimedia: UIMultimedia, isPlaying: Boolean) {
        Logger.d(TAG, "setPlaying() -> uiMultimedia: $uiMultimedia, isPlaying: $isPlaying")

        if (uiMultimedia.isAudio()) {
            val index = asyncListDiffer.currentList.indexOfFirst { it.multimedia.id == uiMultimedia.multimedia.id }

            currentPlayingAudioPosition = if (isPlaying) {
                index
            } else {
                -1
            }

            notifyItemChanged(index, Bundle().apply {
                putBoolean(PayloadKey.TOGGLE_PLAYING, isPlaying)
            })
        }
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
                ViewHolder(parent.inflate(R.layout.bazaar_cell_audio))
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
                if (it is Bundle) {
                    if (!isProcessed) isProcessed = true
                    holder.togglePlaying(item, it.getBoolean(PayloadKey.TOGGLE_PLAYING))
                } else {
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
            }
            if (!isProcessed) {
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val playOrPauseButton = view.findViewById<MaterialButton>(R.id.playOrPauseButton)
        private val checkBoxButton = view.findViewById<CheckBoxButton>(R.id.checkBoxButton)
        private val contentView = view.findViewById<LinearLayout>(R.id.contentView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val subtitleView = view.findViewById<MaterialTextView>(R.id.subtitleView)
        private val durationView = view.findViewById<MaterialTextView>(R.id.durationView)

        fun bind(uiMultimedia: UIMultimedia) {
//            Logger.d(TAG, "uiMultimedia: $uiMultimedia")

            if (leftOffset == null) {
                contentView.post {
                    leftOffset = contentView.x
                }
            }

            toggleSelectionAbility(uiMultimedia)

            if (currentPlayingAudioPosition == bindingAdapterPosition) {
                playOrPauseButton.setIconResource(R.drawable.exo_icon_pause)
            } else {
                playOrPauseButton.setIconResource(R.drawable.exo_icon_play)
            }

            if (uiMultimedia.isSelected) {
                checkBoxButton.setShownState()
                checkBoxButton.show(false)
            } else {
                checkBoxButton.setHiddenState()
                checkBoxButton.hide(false)
            }

            titleView.text = uiMultimedia.getDisplayTitle()

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

            playOrPauseButton.setOnClickListener {
                if (uiMultimedia.isAudio()) {
                    callback.onAudioPlayOrPauseClicked(uiMultimedia)
                }
            }

            itemView.setOnClickListener {
                if (uiMultimedia.isAudio()) {
                    callback.onAudioClicked(uiMultimedia)
                }
            }
        }

        fun toggleSelectionAbility(uiMultimedia: UIMultimedia) {
            if (uiMultimedia.isSelectable) {
                itemView.isEnabled = true
                itemView.foreground = null
            } else {
                itemView.isEnabled = false
                itemView.foreground = AppCompatResources.getDrawable(itemView.context, R.drawable.bazaar_bg_alpha_black)
            }
        }

        fun toggleSelection(uiMultimedia: UIMultimedia) {
            if (uiMultimedia.isSelected) {
                checkBoxButton.show(true)
            } else {
                checkBoxButton.hide(true)
            }
        }

        fun toggleVisibility(uiMultimedia: UIMultimedia) {
            // Ignored
        }

        fun togglePlaying(uiMultimedia: UIMultimedia, isPlaying: Boolean) {
            Logger.d(TAG, "togglePlaying() -> uiMultimedia: $uiMultimedia, isPlaying: $isPlaying")
            if (isPlaying) {
                playOrPauseButton.setIconResource(R.drawable.exo_icon_pause)
            } else {
                playOrPauseButton.setIconResource(R.drawable.exo_icon_play)
            }
        }

    }

    interface Callback {
        fun onAudioPlayOrPauseClicked(uiMultimedia: UIMultimedia)
        fun onAudioClicked(uiMultimedia: UIMultimedia)
    }

}