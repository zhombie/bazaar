package kz.zhombie.bazaar.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kz.garage.multimedia.store.model.Audio
import kz.garage.multimedia.store.model.Content
import kz.garage.multimedia.store.model.Document
import kz.garage.multimedia.store.model.Media
import java.util.concurrent.TimeUnit

@Parcelize
internal open class UIContent constructor(
    val content: Content,
    override val isSelectable: Boolean,
    override val isSelected: Boolean,
    override val isVisible: Boolean
) : Selectable, Visibility, Parcelable {

    fun getDisplayTitle(): String {
        var title = content.label
        if (content is Audio) {
            if (!content.album?.artist.isNullOrBlank()) {
                title = if (title.isNullOrBlank()) {
                    content.album?.artist
                } else {
                    content.album?.artist + " - " + title
                }
            }
        }
        return title ?: "undefined"
    }

    fun getDisplayDuration(): String? {
        return if (content is Media.Playable) {
            val duration = content.duration
            if (duration == null || duration == -1L) return null
            try {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                val seconds = duration % minutes

                "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
            } catch (exception: ArithmeticException) {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)

                if (seconds == 0L) return null

                String.format("00:%02d", seconds)
            }
        } else {
            null
        }
    }

    fun copy(
        content: Content = this@UIContent.content,
        isSelectable: Boolean = this@UIContent.isSelectable,
        isSelected: Boolean = this@UIContent.isSelected,
        isVisible: Boolean = this@UIContent.isVisible
    ): UIContent {
        return if (this is UIMedia) {
            copy(
                media = media,
                isSelectable = isSelectable,
                isSelected = isSelected,
                isVisible = isVisible
            )
        } else {
            UIContent(
                content = content,
                isSelectable = isSelectable,
                isSelected = isSelected,
                isVisible = isVisible
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is UIContent) return false
        return if ((content is Media && other.content is Media) || (content is Document && other.content is Document)) {
            content == other.content && isSelectable == other.isSelectable && isSelected == other.isSelected && isVisible == other.isVisible
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "${UIContent::class.java.simpleName}(" +
                "content=$content, " +
                "isSelectable=$isSelectable, " +
                "isSelected=$isSelected, " +
                "isVisible=$isVisible)"
    }

}