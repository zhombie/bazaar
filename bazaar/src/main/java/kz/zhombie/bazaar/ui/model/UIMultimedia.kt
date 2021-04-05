package kz.zhombie.bazaar.ui.model

import kz.zhombie.bazaar.api.model.Audio
import kz.zhombie.bazaar.api.model.Document
import kz.zhombie.bazaar.api.model.Multimedia
import kz.zhombie.bazaar.api.model.Media
import java.io.Serializable
import java.util.concurrent.TimeUnit

internal open class UIMultimedia constructor(
    val multimedia: Multimedia,
    override val isSelectable: Boolean,
    override val isSelected: Boolean,
    override val isVisible: Boolean
) : Controllable, Serializable {

    fun isAudio(): Boolean = multimedia is Audio

    fun isDocument(): Boolean = multimedia is Document

    fun getDisplayTitle(): String {
        var title = multimedia.displayName
        if (multimedia is Audio) {
            if (!multimedia.album?.artist.isNullOrBlank()) {
                title = multimedia.album?.artist + " - " + multimedia.displayName
            }
        }
        return title
    }

    open fun getDisplayDuration(): String? {
        return if (multimedia is Audio) {
            if (multimedia.duration == null) return null
            try {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(multimedia.duration)
                val seconds = multimedia.duration % minutes.toInt()

                "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
            } catch (exception: ArithmeticException) {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(multimedia.duration)

                if (seconds == 0L) return null

                String.format("00:%02d", seconds)
            }
        } else {
            null
        }
    }

    fun copy(
        multimedia: Multimedia = this.multimedia,
        isSelectable: Boolean = this.isSelectable,
        isSelected: Boolean = this.isSelected,
        isVisible: Boolean = this.isVisible
    ): UIMultimedia {
        return if (this is UIMedia) {
            copy(
                media = media,
                isSelectable = isSelectable,
                isSelected = isSelected,
                isVisible = isVisible
            )
        } else {
            UIMultimedia(
                multimedia = multimedia,
                isSelectable = isSelectable,
                isSelected = isSelected,
                isVisible = isVisible
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is UIMultimedia) return false
        return if ((multimedia is Media && other.multimedia is Media) || (multimedia is Audio && other.multimedia is Audio)) {
            multimedia == other.multimedia && isSelectable == other.isSelectable && isSelected == other.isSelected && isVisible == other.isVisible
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "${UIMultimedia::class.java.simpleName}(multimedia=$multimedia, isSelectable=$isSelectable, isSelected=$isSelected, isVisible=$isVisible)"
    }

}