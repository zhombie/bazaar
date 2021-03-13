package kz.zhombie.bazaar.ui.media.audible

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.core.exception.ViewHolderException
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.model.UIMultimedia

internal class AudiosAdapter constructor(
    private val imageLoader: ImageLoader
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
            ViewType.AUDIO -> {
                ViewHolder(
                    view = LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.cell_audio, parent, false)
                )
            }
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

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val slider = view.findViewById<Slider>(R.id.slider)
        private val durationView = view.findViewById<MaterialTextView>(R.id.durationView)

        fun bind(uiMultimedia: UIMultimedia) {
            titleView.text = uiMultimedia.multimedia.displayName
            durationView.text = uiMultimedia.getDisplayDuration()
        }

    }

}