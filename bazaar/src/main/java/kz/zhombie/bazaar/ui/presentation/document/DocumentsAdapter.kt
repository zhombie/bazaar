package kz.zhombie.bazaar.ui.presentation.document

import android.os.Build
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
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.components.view.CheckBoxButton
import kz.zhombie.bazaar.ui.model.UIContent
import kz.zhombie.bazaar.utils.inflate
import kz.zhombie.multimedia.model.Audio
import kz.zhombie.multimedia.model.Document

internal class DocumentsAdapter constructor(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = DocumentsAdapter::class.java.simpleName

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

    private object PayloadKey {
        const val TOGGLE_SELECTION_ABILITY = "toggle_selection_ability"
        const val TOGGLE_SELECTION = "toggle_selection"
        const val TOGGLE_VISIBILITY = "toggle_visibility"
    }

    private val asyncListDiffer: AsyncListDiffer<UIContent> by lazy {
        AsyncListDiffer(this, diffCallback)
    }

    fun getList(): List<UIContent> = asyncListDiffer.currentList

    fun submitList(uiContents: List<UIContent>) {
        Logger.d(TAG, "submitList() -> ${uiContents.size}")
        asyncListDiffer.submitList(uiContents)
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    private fun getItem(position: Int): UIContent = asyncListDiffer.currentList[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.bazaar_cell_document))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is ViewHolder) holder.bind(item)
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
            if (item.content !is Audio) {
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
        private val iconButton = view.findViewById<MaterialButton>(R.id.iconButton)
        private val checkBoxButton = view.findViewById<CheckBoxButton>(R.id.checkBoxButton)
        private val contentView = view.findViewById<LinearLayout>(R.id.contentView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val subtitleView = view.findViewById<MaterialTextView>(R.id.subtitleView)
        private val sizeView = view.findViewById<MaterialTextView>(R.id.sizeView)

        init {
            checkBoxButton.setCheckedDrawable()
        }

        fun bind(uiContent: UIContent) {
            toggleSelectionAbility(uiContent)

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
            
            iconButton.setOnClickListener {
                if (uiContent.content is Document) {
                    callback.onDocumentIconClicked(uiContent)
                }
            }

            itemView.setOnClickListener {
                if (uiContent.content is Document) {
                    callback.onDocumentClicked(uiContent)
                }
            }
        }

        fun toggleSelectionAbility(uiContent: UIContent) {
            if (uiContent.isSelectable) {
                itemView.isEnabled = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    itemView.foreground = null
                }
            } else {
                itemView.isEnabled = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    itemView.foreground = AppCompatResources.getDrawable(itemView.context, R.drawable.bazaar_bg_alpha_black)
                }
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
            Logger.d(TAG, "toggleVisibility() -> uiContent: $uiContent")
        }
    }

    interface Callback {
        fun onDocumentIconClicked(uiContent: UIContent)
        fun onDocumentClicked(uiContent: UIContent)
    }

}