package kz.zhombie.bazaar.ui.media.visual

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.core.exception.ViewHolderException

internal class VisualMediaHeaderAdapter constructor(
    isCameraEnabled: Boolean,
    isExplorerEnabled: Boolean,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = VisualMediaHeaderAdapter::class.java.simpleName
    }

    private object ViewType {
        const val CAMERA = 100
        const val EXPLORER = 101
    }

    class FunctionalButton private constructor(
        val type: Type,

        @DrawableRes
        val icon: Int,

        @StringRes
        val title: Int
    ) {

        companion object {
            fun camera(): FunctionalButton {
                return FunctionalButton(Type.CAMERA, R.drawable.ic_camera, R.string.camera)
            }

            fun explorer(): FunctionalButton {
                return FunctionalButton(Type.EXPLORER, R.drawable.ic_folder, R.string.explorer)
            }
        }

        enum class Type {
            CAMERA,
            EXPLORER
        }

    }

    var isCameraEnabled: Boolean = isCameraEnabled
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isExplorerEnabled: Boolean = isExplorerEnabled
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int): FunctionalButton? {
        return if (isCameraEnabled && isExplorerEnabled) {
            when (position) {
                0 -> FunctionalButton.camera()
                1 -> FunctionalButton.explorer()
                else -> null
            }
        } else {
            when {
                isCameraEnabled -> FunctionalButton.camera()
                isExplorerEnabled -> FunctionalButton.explorer()
                else -> null
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item?.type) {
            FunctionalButton.Type.CAMERA -> ViewType.CAMERA
            FunctionalButton.Type.EXPLORER -> ViewType.EXPLORER
            else -> super.getItemViewType(position)
        }
    }

    override fun getItemCount(): Int = if (isCameraEnabled && isExplorerEnabled) {
        2
    } else {
        if (isCameraEnabled || isExplorerEnabled) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CAMERA -> {
                ViewHolder(
                    LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.cell_functional_button, parent, false)
                )
            }
            ViewType.EXPLORER -> {
                ViewHolder(
                    LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.cell_functional_button, parent, false)
                )
            }
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
                } else if (functionalButton.type == FunctionalButton.Type.EXPLORER) {
                    callback.onExplorerClicked()
                }
            }
        }
    }

    interface Callback {
        fun onCameraClicked()
        fun onExplorerClicked()
    }

}