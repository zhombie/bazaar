package kz.zhombie.bazaar.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.model.*
import java.util.concurrent.TimeUnit

@SuppressLint("SetTextI18n")
class MultimediaResultAdapter constructor(
    var imageLoader: ImageLoader,
    private val callback: (multimedia: Multimedia) -> Unit
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
                is Image -> bindImage(multimedia)
                is Video -> bindVideo(multimedia)
                is Audio -> bindAudio(multimedia)
                is Document -> bindDocument(multimedia)
                else -> {
                    imageView.setImageDrawable(null)
                    textView.text = null
                }
            }

            itemView.setOnClickListener { callback(multimedia) }
        }

        private fun bindImage(image: Image) {
            val thumbnail = image.thumbnail ?: image.source
            if (thumbnail == null) {
                imageLoader.loadSmallImage(itemView.context, imageView, image.uri)
            } else {
                imageLoader.loadSmallImage(itemView.context, imageView, thumbnail)
            }

            textView.text = """
id: ${image.id}
Name: ${image.displayName}
Type: ${Image::class.java.simpleName}
File size: ${image.size}
Folder: ${image.folderDisplayName}
Extension: ${image.extension}
Width x Height: ${image.width}x${image.height} 
Link: ${image.uri}
Path: ${image.path}
MIME type: ${image.mimeType},
Source image: ${if (image.source != null) "Exists" else "Does not exist"}
Thumbnail: ${if (image.thumbnail != null) "Exists" else "Does not exist"}
            """.trim()
        }

        private fun bindVideo(video: Video) {
            val thumbnail = video.thumbnail
            if (thumbnail == null) {
                imageLoader.loadSmallImage(itemView.context, imageView, video.uri)
            } else {
                imageLoader.loadSmallImage(itemView.context, imageView, thumbnail)
            }

            textView.text = """
id: ${video.id}
Name: ${video.displayName}
Type: ${Video::class.java.simpleName}
File size: ${video.size}
Duration: ${getDuration(video)}, ${getDisplayDuration(video)}
Folder: ${video.folderDisplayName}
Extension: ${video.extension}
Width x Height: ${video.width}x${video.height} 
Link: ${video.uri}
Path: ${video.path}
MIME type: ${video.mimeType},
Cover image: ${if (video.thumbnail != null) "Exists" else "Does not exist"}
            """.trim()
        }

        private fun bindAudio(audio: Audio) {
            val thumbnail = audio.thumbnail
            if (thumbnail == null) {
                imageLoader.loadSmallImage(itemView.context, imageView, audio.uri)
            } else {
                imageLoader.loadSmallImage(itemView.context, imageView, thumbnail)
            }

            textView.text = """
id: ${audio.id}
Name: ${audio.displayName}
Type: ${Audio::class.java.simpleName}
File size: ${audio.size}
Duration: ${getDuration(audio)}, ${getDisplayDuration(audio)}
Folder: ${audio.folderDisplayName}
Extension: ${audio.extension}
Ссылка: ${audio.uri}
Путь: ${audio.path}
MIME type: ${audio.mimeType},
Обложка: ${if (audio.thumbnail != null) "Exists" else "Does not exist"}
            """.trim()
        }

        private fun bindDocument(document: Document) {
            imageView.setImageDrawable(null)

            textView.text = """
id: ${document.id}
Name: ${document.displayName}
Type: ${Document::class.java.simpleName}
File size: ${document.size}
Folder: ${document.folderDisplayName}
Extension: ${document.extension}
Ссылка: ${document.uri}
Путь: ${document.path}
MIME type: ${document.mimeType}
            """.trim()
        }

        private fun getDuration(multimedia: Multimedia): Long? {
            return when (multimedia) {
                is Video -> multimedia.duration
                is Audio -> multimedia.duration
                else -> null
            }
        }

        private fun getDisplayDuration(multimedia: Multimedia): String? {
            fun format(duration: Long): String {
                return try {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                    val seconds = duration % minutes.toInt()

                    "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
                } catch (exception: ArithmeticException) {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)

                    String.format("00:%02d", seconds)
                }
            }

            val duration = getDuration(multimedia)
            return if (duration == null) null else format(duration)
        }
    }

}