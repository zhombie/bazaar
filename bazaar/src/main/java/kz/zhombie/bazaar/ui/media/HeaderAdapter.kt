package kz.zhombie.bazaar.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.core.ViewHolderException

internal class HeaderAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = HeaderAdapter::class.java.simpleName
    }

    private object ViewType {
        const val CAMERA = 100
        const val EXPLORER = 101
    }

    data class FunctionalButton constructor(
        @DrawableRes
        val icon: Int,

        @StringRes
        val title: Int
    )

    private fun getItem(position: Int): FunctionalButton? {
        return when (position) {
            0 -> FunctionalButton(R.drawable.ic_camera, R.string.camera)
            1 -> FunctionalButton(R.drawable.ic_folder, R.string.explorer)
            else -> null
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> ViewType.CAMERA
            1 -> ViewType.EXPLORER
            else -> super.getItemViewType(position)
        }
    }

    override fun getItemCount(): Int = 2

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
        }
    }

}