package kz.zhombie.bazaar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video

class MediaResultAdapter constructor(
    var imageLoader: ImageLoader
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var media: List<Media> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = media.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_media_result, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(media[position])
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)

        fun bind(media: Media) {
            imageLoader.loadGridItemImage(itemView.context, imageView, media.uri)
            val type = when (media) {
                is Image -> "Фото"
                is Video -> "Видео"
                else -> "Неизвестно"
            }
            textView.text = """
Название файла: ${media.displayName}
Тип файла: $type
Размер файла: ${media.size}
Папка: ${media.folderDisplayName}
            """.trim()
        }

    }

}