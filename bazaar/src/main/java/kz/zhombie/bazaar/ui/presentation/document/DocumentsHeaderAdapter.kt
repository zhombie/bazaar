package kz.zhombie.bazaar.ui.presentation.document

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.ui.model.FunctionalButton
import kz.zhombie.bazaar.utils.inflate

internal class DocumentsHeaderAdapter constructor(
    isExplorerEnabled: Boolean,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = DocumentsHeaderAdapter::class.java.simpleName
    }

    var isExplorerEnabled: Boolean = isExplorerEnabled
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int): FunctionalButton? {
        return if (isExplorerEnabled) {
            when (position) {
                0 -> FunctionalButton.explorer()
                else -> null
            }
        } else {
            null
        }
    }

    override fun getItemCount(): Int = if (isExplorerEnabled) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.bazaar_cell_functional_button_horizontal))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is ViewHolder) {
            if (item != null) {
                holder.bind(item)
            }
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)

        fun bind(functionalButton: FunctionalButton) {
            imageView.setImageResource(functionalButton.icon)
            titleView.setText(functionalButton.title)

            itemView.setOnClickListener {
                if (functionalButton.type == FunctionalButton.Type.EXPLORER) {
                    callback.onExplorerClicked()
                }
            }
        }
    }

    interface Callback {
        fun onExplorerClicked()
    }

}