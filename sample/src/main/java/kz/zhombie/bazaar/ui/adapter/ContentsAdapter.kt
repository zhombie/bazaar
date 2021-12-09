package kz.zhombie.bazaar.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.garage.multimedia.store.model.*
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.dispose
import kz.zhombie.bazaar.load
import java.util.concurrent.TimeUnit

@SuppressLint("SetTextI18n")
class ContentsAdapter constructor(
    private val callback: (content: Content) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var contents: List<Content> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = contents.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_content, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(contents[position])
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        if (holder is ViewHolder) {
            holder.unbind()
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)

        fun bind(content: Content) {
            when (content) {
                is Image -> bindImage(content)
                is Video -> bindVideo(content)
                is Audio -> bindAudio(content)
                is Document -> bindDocument(content)
                else -> {
                    imageView.setImageDrawable(null)
                    textView.text = null
                }
            }

            itemView.setOnClickListener { callback(content) }
        }

        fun unbind() {
            imageView.dispose()
        }

        private fun bindImage(image: Image) {
            val uri = image.localFile?.uri ?: image.uri

            imageView.load(uri) {
                setErrorDrawable(R.drawable.bazaar_bg_black)
                setPlaceholderDrawable(R.drawable.bazaar_bg_black)
                setSize(300, 300)
            }

            textView.text = """
id: ${image.id}
Name: ${image.displayName}
Type: ${Image::class.java.simpleName}
File size: ${image.properties?.size}
Folder: ${image.folder?.displayName}
Extension: ${image.localFile?.file?.extension}
Width x Height: ${image.resolution?.width}x${image.resolution?.height} 
Link: ${image.uri}
Path: ${image.localFile?.file?.absolutePath}
MIME type: ${image.properties?.mimeType}
            """.trim()
        }

        private fun bindVideo(video: Video) {
            val uri = video.localFile?.uri ?: video.uri

            imageView.load(uri) {
                setErrorDrawable(R.drawable.bazaar_bg_black)
                setPlaceholderDrawable(R.drawable.bazaar_bg_black)
                setSize(300, 300)
            }

            textView.text = """
id: ${video.id}
Name: ${video.displayName}
Type: ${Video::class.java.simpleName}
File size: ${video.properties?.size}
Duration: ${getDuration(video)}, ${getDisplayDuration(video)}
Folder: ${video.folder?.displayName}
Extension: ${video.localFile?.file?.extension}
Width x Height: ${video.resolution?.width}x${video.resolution?.height} 
Link: ${video.uri}
Path: ${video.localFile?.file?.absolutePath}
MIME type: ${video.properties?.mimeType}
            """.trim()
        }

        private fun bindAudio(audio: Audio) {
            val uri = audio.localFile?.uri ?: audio.uri

            imageView.load(uri) {
                setErrorDrawable(R.drawable.bazaar_bg_black)
                setPlaceholderDrawable(R.drawable.bazaar_bg_black)
                setSize(300, 300)
            }

            textView.text = """
id: ${audio.id}
Name: ${audio.displayName}
Type: ${Audio::class.java.simpleName}
File size: ${audio.properties?.size}
Duration: ${getDuration(audio)}, ${getDisplayDuration(audio)}
Folder: ${audio.folder?.displayName}
Extension: ${audio.localFile?.file?.extension}
Ссылка: ${audio.uri}
Path: ${audio.localFile?.file?.absolutePath}
MIME type: ${audio.properties?.mimeType}
            """.trim()
        }

        private fun bindDocument(document: Document) {
            imageView.setImageDrawable(null)

            textView.text = """
id: ${document.id}
Name: ${document.displayName}
Type: ${Document::class.java.simpleName}
File size: ${document.properties?.size}
Folder: ${document.folder?.displayName}
Extension: ${document.localFile?.file?.extension}
Ссылка: ${document.uri}
Path: ${document.localFile?.file?.absolutePath}
MIME type: ${document.properties?.mimeType}
            """.trim()
        }

        private fun getDuration(content: Content): Long? {
            return when (content) {
                is Video -> content.duration
                is Audio -> content.duration
                else -> null
            }
        }

        private fun getDisplayDuration(content: Content): String? {
            fun format(duration: Long): String {
                return try {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                    val seconds = duration % minutes

                    "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
                } catch (exception: ArithmeticException) {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)

                    String.format("00:%02d", seconds)
                }
            }

            val duration = getDuration(content)
            return if (duration == null) null else format(duration)
        }
    }

}