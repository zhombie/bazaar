package kz.zhombie.bazaar.ui.model

import kz.zhombie.bazaar.api.model.Media
import java.io.Serializable

internal data class UIMedia constructor(
    val media: Media,
    override val isSelectable: Boolean,
    override val isSelected: Boolean,
    override val isVisible: Boolean
) : Controllable, Serializable