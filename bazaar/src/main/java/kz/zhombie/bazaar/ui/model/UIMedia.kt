package kz.zhombie.bazaar.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kz.garage.multimedia.store.model.Media

@Parcelize
internal data class UIMedia constructor(
    val media: Media,
    override val isSelectable: Boolean,
    override val isSelected: Boolean,
    override val isVisible: Boolean
) : UIContent(
    content = media,
    isSelectable = isSelectable,
    isSelected = isSelected,
    isVisible = isVisible
), Parcelable {

    override fun equals(other: Any?): Boolean {
        if (other is UIMedia) {
            return media.id == other.media.id &&
                media.uri == other.media.uri &&
                media.title == other.media.title &&
                media.displayName == other.media.displayName &&
                isSelectable == other.isSelectable &&
                isSelected == other.isSelected &&
                isVisible == other.isVisible
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

}