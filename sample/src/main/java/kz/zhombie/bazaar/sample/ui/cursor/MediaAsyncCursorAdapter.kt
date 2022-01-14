package kz.zhombie.bazaar.sample.ui.cursor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kz.garage.multimedia.store.model.Content
import kz.zhombie.bazaar.dispose
import kz.zhombie.bazaar.load
import kz.zhombie.bazaar.sample.R
import kz.zhombie.museum.PaintingLoader

class MediaAsyncCursorAdapter : RecyclerView.Adapter<MediaAsyncCursorAdapter.ViewHolder>() {

    var contents: List<Content> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int): Content = contents[position]

    override fun getItemCount(): Int = contents.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.bazaar_cell_image, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        holder.unbind()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)

        fun bind(image: Content) {
            imageView.load(image.localFile?.uri ?: image.uri) {
                setCrossfade(PaintingLoader.Request.Crossfade(100, true))
                setSize(275, 275)
                setScale(PaintingLoader.Request.Scale.FIT)
            }
        }

        fun unbind() {
            imageView.dispose()
        }

    }

}