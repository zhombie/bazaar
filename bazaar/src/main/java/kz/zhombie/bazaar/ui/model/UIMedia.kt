package kz.zhombie.bazaar.ui.model

import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import java.io.Serializable
import java.util.concurrent.TimeUnit

internal data class UIMedia constructor(
    val media: Media,
    override val isSelectable: Boolean,
    override val isSelected: Boolean,
    override val isVisible: Boolean
) : Controllable, Serializable {

    fun isImage(): Boolean {
        return media is Image
    }

    fun isVideo(): Boolean {
        return media is Video
    }

    fun getDisplayDuration(): String? {
        return if (media is Video) {
            if (media.duration == null) return null
            try {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(media.duration)
                val seconds = media.duration % minutes.toInt()

                "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
            } catch (exception: ArithmeticException) {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(media.duration)

                String.format("00:%02d", seconds)
            }
        } else {
            null
        }
    }

}