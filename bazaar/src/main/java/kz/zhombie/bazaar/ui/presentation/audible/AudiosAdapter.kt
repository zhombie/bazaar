package kz.zhombie.bazaar.ui.presentation.audible

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
import kz.garage.multimedia.store.model.Audio
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.core.exception.ViewHolderException
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.components.view.CheckBoxButton
import kz.zhombie.bazaar.ui.model.UIContent
import kz.zhombie.bazaar.utils.inflate

internal class AudiosAdapter constructor(
    private val callback: Callback,
    private val onLeftOffsetReadyListener: ((leftOffset: Float) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = AudiosAdapter::class.java.simpleName

        private val diffCallback = object : DiffUtil.ItemCallback<UIContent>() {
            override fun areItemsTheSame(oldItem: UIContent, newItem: UIContent): Boolean =
                oldItem.content.id == newItem.content.id && oldItem.content.uri == newItem.content.uri

            override fun areContentsTheSame(oldItem: UIContent, newItem: UIContent): Boolean =
                oldItem == newItem

            override fun getChangePayload(oldItem: UIContent, newItem: UIContent): Any? = when {
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

    private val asyncListDiffer: AsyncListDiffer<UIContent> by lazy {
        AsyncListDiffer(this, diffCallback).also { asyncListDiffer ->
            asyncListDiffer.addListListener { previousList, currentList ->
                if (currentPlayingAudioPosition < 0) return@addListListener
                if (previousList.isNullOrEmpty()) return@addListListener
                if (currentPlayingAudioPosition > previousList.size) return@addListListener
                if (currentList.isNullOrEmpty()) return@addListListener

                val uiContent = previousList[currentPlayingAudioPosition]
                val index = currentList.indexOfFirst { it.content.id == uiContent.content.id }
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
//                    Logger.debug(TAG, "leftOffset: $value")
                    onLeftOffsetReadyListener?.invoke(value)
                }
            }
        }

    private var currentPlayingAudioPosition: Int = -1

    fun getList(): List<UIContent> = asyncListDiffer.currentList

    fun submitList(uiContents: List<UIContent>) {
//        Logger.debug(TAG, "submitList() -> ${uiContents.size}")
        asyncListDiffer.submitList(uiContents)
    }

    fun setPlaying(uiContent: UIContent, isPlaying: Boolean) {
//        Logger.debug(TAG, "setPlaying() -> uiContent: $uiContent, isPlaying: $isPlaying")

        if (uiContent.content is Audio) {
            val index = asyncListDiffer.currentList.indexOfFirst { it.content.id == uiContent.content.id }

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

    private fun getItem(position: Int): UIContent = asyncListDiffer.currentList[position]

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item.content) {
            is Audio -> ViewType.AUDIO
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
        when (item.content) {
            is Audio -> if (holder is ViewHolder) holder.bind(item)
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
            if (item.content !is Audio) {
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

        init {
            checkBoxButton.setCheckedDrawable()
        }

        fun bind(uiContent: UIContent) {
//            Logger.d(TAG, "uiContent: uiContent")

            if (leftOffset == null) {
                contentView.post {
                    leftOffset = contentView.x
                }
            }

            toggleSelectionAbility(uiContent)

            if (currentPlayingAudioPosition == bindingAdapterPosition) {
                playOrPauseButton.setIconResource(R.drawable.bazaar_ic_pause)
            } else {
                playOrPauseButton.setIconResource(R.drawable.bazaar_ic_play)
            }

            if (uiContent.isSelected) {
                checkBoxButton.setShownState()
                checkBoxButton.show(false)
            } else {
                checkBoxButton.setHiddenState()
                checkBoxButton.hide(false)
            }

            titleView.text = uiContent.getDisplayTitle()

            val folder = uiContent.content.folder?.displayName
            if (folder.isNullOrBlank()) {
                subtitleView.text = null
                subtitleView.visibility = View.GONE
            } else {
                subtitleView.text = folder
                subtitleView.visibility = View.VISIBLE
            }

            val displayDuration = uiContent.getDisplayDuration()
            if (displayDuration.isNullOrBlank()) {
                durationView.text = null
                durationView.visibility = View.GONE
            } else {
                durationView.text = displayDuration
                durationView.visibility = View.VISIBLE
            }

            playOrPauseButton.setOnClickListener {
                if (uiContent.content is Audio) {
                    callback.onAudioPlayOrPauseClicked(uiContent)
                }
            }

            itemView.setOnClickListener {
                if (uiContent.content is Audio) {
                    callback.onAudioClicked(uiContent)
                }
            }
        }

        fun toggleSelectionAbility(uiContent: UIContent) {
            if (uiContent.isSelectable) {
                itemView.isEnabled = true

                itemView.foreground = null
            } else {
                itemView.isEnabled = false

                itemView.foreground = AppCompatResources.getDrawable(itemView.context, R.drawable.bazaar_bg_alpha_black)
            }
        }

        fun toggleSelection(uiContent: UIContent) {
            if (uiContent.isSelected) {
                checkBoxButton.show(true)
            } else {
                checkBoxButton.hide(true)
            }
        }

        fun toggleVisibility(uiContent: UIContent) {
            Logger.debug(TAG, "toggleVisibility() -> uiContent: $uiContent")
        }

        fun togglePlaying(uiContent: UIContent, isPlaying: Boolean) {
            Logger.debug(TAG, "togglePlaying() -> uiContent: $uiContent, isPlaying: $isPlaying")
            if (isPlaying) {
                playOrPauseButton.setIconResource(R.drawable.bazaar_ic_pause)
            } else {
                playOrPauseButton.setIconResource(R.drawable.bazaar_ic_play)
            }
        }

    }

    interface Callback {
        fun onAudioPlayOrPauseClicked(uiContent: UIContent)
        fun onAudioClicked(uiContent: UIContent)
    }

}