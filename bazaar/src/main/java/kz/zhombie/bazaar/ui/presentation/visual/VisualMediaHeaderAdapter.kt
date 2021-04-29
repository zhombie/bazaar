package kz.zhombie.bazaar.ui.presentation.visual

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.core.exception.ViewHolderException
import kz.zhombie.bazaar.ui.model.FunctionalButton
import kz.zhombie.bazaar.utils.inflate

internal class VisualMediaHeaderAdapter constructor(
    isCameraEnabled: Boolean,
    isChooseFromLibraryEnabled: Boolean,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = VisualMediaHeaderAdapter::class.java.simpleName
    }

    private object ViewType {
        const val CAMERA = 100
        const val CHOOSE_FROM_LIBRARY = 101
    }

    var isCameraEnabled: Boolean = isCameraEnabled
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isChooseFromLibraryEnabled: Boolean = isChooseFromLibraryEnabled
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int): FunctionalButton? {
        return if (isCameraEnabled && isChooseFromLibraryEnabled) {
            when (position) {
                0 -> FunctionalButton.camera()
                1 -> FunctionalButton.chooseFromLibrary()
                else -> null
            }
        } else {
            when {
                isCameraEnabled -> FunctionalButton.camera()
                isChooseFromLibraryEnabled -> FunctionalButton.chooseFromLibrary()
                else -> null
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item?.type) {
            FunctionalButton.Type.CAMERA -> ViewType.CAMERA
            FunctionalButton.Type.CHOOSE_FROM_LIBRARY -> ViewType.CHOOSE_FROM_LIBRARY
            else -> super.getItemViewType(position)
        }
    }

    override fun getItemCount(): Int = if (isCameraEnabled && isChooseFromLibraryEnabled) {
        2
    } else {
        if (isCameraEnabled || isChooseFromLibraryEnabled) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CAMERA ->
                ViewHolder(parent.inflate(R.layout.bazaar_cell_functional_button_square))
            ViewType.CHOOSE_FROM_LIBRARY ->
                ViewHolder(parent.inflate(R.layout.bazaar_cell_functional_button_square))
            else ->
                throw ViewHolderException(viewType)
        }
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
                if (functionalButton.type == FunctionalButton.Type.CAMERA) {
                    callback.onCameraClicked()
                } else if (functionalButton.type == FunctionalButton.Type.CHOOSE_FROM_LIBRARY) {
                    callback.onChooseFromLibraryClicked()
                }
            }
        }
    }

    interface Callback {
        fun onCameraClicked()
        fun onChooseFromLibraryClicked()
    }

}