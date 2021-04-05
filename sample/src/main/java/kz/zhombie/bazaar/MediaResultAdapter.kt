package kz.zhombie.bazaar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.model.*
import java.util.concurrent.TimeUnit

class MediaResultAdapter constructor(
    var imageLoader: ImageLoader
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var multimedia: List<Multimedia> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = multimedia.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_media_result, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(multimedia[position])
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)

        fun bind(multimedia: Multimedia) {
            when (multimedia) {
                is Image -> {
                    val thumbnail = multimedia.thumbnail ?: multimedia.source
                    if (thumbnail == null) {
                        imageLoader.loadGridItemImage(itemView.context, imageView, multimedia.uri)
                    } else {
                        imageLoader.loadGridItemImage(itemView.context, imageView, thumbnail)
                    }

                    textView.text = """
id: ${multimedia.id}
Название: ${multimedia.displayName}
Тип: "Фото"
Размер: ${multimedia.size}
Папка: ${multimedia.folderDisplayName}
Расширение: ${multimedia.extension}
Ширина x Высота: ${multimedia.width}x${multimedia.height} 
Ссылка: ${multimedia.uri}
Путь: ${multimedia.path}
MIME type: ${multimedia.mimeType},
Оригинальное фото: ${if (multimedia.source != null) "Есть" else "Нет"}
Ярлык: ${if (multimedia.thumbnail != null) "Есть" else "Нет"}
                    """.trim()
                }
                is Video -> {
                    val thumbnail = multimedia.thumbnail
                    if (thumbnail == null) {
                        imageLoader.loadGridItemImage(itemView.context, imageView, multimedia.uri)
                    } else {
                        imageLoader.loadGridItemImage(itemView.context, imageView, thumbnail)
                    }

                    textView.text = """
id: ${multimedia.id}
Название: ${multimedia.displayName}
Тип: "Видео"
Размер: ${multimedia.size}
Продолжительность: ${getDuration(multimedia)}, ${getDisplayDuration(multimedia)}
Папка: ${multimedia.folderDisplayName}
Расширение: ${multimedia.extension}
Ширина x Высота: ${multimedia.width}x${multimedia.height} 
Ссылка: ${multimedia.uri}
Путь: ${multimedia.path}
MIME type: ${multimedia.mimeType},
Обложка: ${if (multimedia.thumbnail != null) "Есть" else "Нет"}
                    """.trim()
                }
                is Audio -> {
                    val thumbnail = multimedia.thumbnail
                    if (thumbnail == null) {
                        imageLoader.loadGridItemImage(itemView.context, imageView, multimedia.uri)
                    } else {
                        imageLoader.loadGridItemImage(itemView.context, imageView, thumbnail)
                    }

                    textView.text = """
id: ${multimedia.id}
Название: ${multimedia.displayName}
Тип: "Аудио"
Размер: ${multimedia.size}
Продолжительность: ${getDuration(multimedia)}, ${getDisplayDuration(multimedia)}
Папка: ${multimedia.folderDisplayName}
Расширение: ${multimedia.extension}
Ссылка: ${multimedia.uri}
Путь: ${multimedia.path}
MIME type: ${multimedia.mimeType},
Обложка: ${if (multimedia.thumbnail != null) "Есть" else "Нет"}
                    """.trim()
                }
                is Document -> {
                    textView.text = """
id: ${multimedia.id}
Название: ${multimedia.displayName}
Тип: "Документ"
Размер: ${multimedia.size}
Папка: ${multimedia.folderDisplayName}
Расширение: ${multimedia.extension}
Ссылка: ${multimedia.uri}
Путь: ${multimedia.path}
MIME type: ${multimedia.mimeType}
                    """.trim()
                }
                else -> {
                }
            }
        }

    }

    private fun getDuration(multimedia: Multimedia): Long? {
        return when (multimedia) {
            is Video -> {
                multimedia.duration
            }
            is Audio -> {
                multimedia.duration
            }
            else -> {
                null
            }
        }
    }

    private fun getDisplayDuration(multimedia: Multimedia): String? {
        return when (multimedia) {
            is Video -> {
                val duration = multimedia.duration ?: return null
                try {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                    val seconds = duration % minutes.toInt()

                    "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
                } catch (exception: ArithmeticException) {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)

                    String.format("00:%02d", seconds)
                }
            }
            is Audio -> {
                val duration = multimedia.duration ?: return null
                try {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                    val seconds = duration % minutes.toInt()

                    "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
                } catch (exception: ArithmeticException) {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)

                    String.format("00:%02d", seconds)
                }
            }
            else -> {
                null
            }
        }
    }

}