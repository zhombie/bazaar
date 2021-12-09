package kz.zhombie.bazaar.ui.model

import kz.garage.multimedia.store.model.Media
import java.io.Serializable

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
), Serializable