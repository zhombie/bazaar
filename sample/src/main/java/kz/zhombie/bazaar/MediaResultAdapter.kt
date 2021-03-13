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
import java.util.concurrent.TimeUnit

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
            when (media) {
                is Image -> {
                    val thumbnail = media.thumbnail ?: media.source
                    if (thumbnail == null) {
                        imageLoader.loadGridItemImage(itemView.context, imageView, media.uri)
                    } else {
                        imageLoader.loadGridItemImage(itemView.context, imageView, thumbnail)
                    }

                    textView.text = """
Название: ${media.displayName}
Тип: "Фото"
Размер: ${media.size}
Папка: ${media.folderDisplayName}
Расширение: ${media.extension}
Ширина x Высота: ${media.width}x${media.height} 
Ссылка: ${media.uri}
Путь: ${media.path}
MIME type: ${media.mimeType},
Оригинальное фото: ${if (media.source != null) "Есть" else "Нет"}
Ярлык: ${if (media.thumbnail != null) "Есть" else "Нет"}
                    """.trim()
                }
                is Video -> {
                    val thumbnail = media.thumbnail
                    if (thumbnail == null) {
                        imageLoader.loadGridItemImage(itemView.context, imageView, media.uri)
                    } else {
                        imageLoader.loadGridItemImage(itemView.context, imageView, thumbnail)
                    }

                    textView.text = """
Название: ${media.displayName}
Тип: "Видео"
Размер: ${media.size}
Продолжительность: ${getDuration(media)}, ${getDisplayDuration(media)}
Папка: ${media.folderDisplayName}
Расширение: ${media.extension}
Ширина x Высота: ${media.width}x${media.height} 
Ссылка: ${media.uri}
Путь: ${media.path}
MIME type: ${media.mimeType},
Обложка: ${if (media.thumbnail != null) "Есть" else "Нет"}
                    """.trim()
                }
                else -> {
                }
            }
        }

    }

    private fun getDuration(media: Media): Long? {
        return if (media is Video) {
            media.duration
        } else {
            null
        }
    }

    private fun getDisplayDuration(media: Media): String? {
        return if (media is Video) {
            val duration = media.duration ?: return null
            try {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                val seconds = duration % minutes.toInt()

                "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
            } catch (exception: ArithmeticException) {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)

                String.format("00:%02d", seconds)
            }
        } else {
            null
        }
    }

}