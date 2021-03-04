package kz.zhombie.bazaar.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kz.zhombie.bazaar.R

internal class HeaderAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG: String = HeaderAdapter::class.java.simpleName
    }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.cell_header, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind()
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val titleButton = view.findViewById<MaterialButton>(R.id.titleButton)
        private val closeButton = view.findViewById<MaterialButton>(R.id.closeButton)

        fun bind() {
            titleButton.text = "Название альбома"
        }
    }

}