package kz.zhombie.bazaar.ui.model

import kz.zhombie.bazaar.model.Media
import java.io.Serializable

data class UIMedia constructor(
    val media: Media,
    override val isSelected: Boolean,
    override val isVisible: Boolean
) : Controllable, Serializable