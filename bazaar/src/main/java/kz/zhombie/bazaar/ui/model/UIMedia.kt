package kz.zhombie.bazaar.ui.model

import kz.zhombie.bazaar.model.Media

data class UIMedia constructor(
    val media: Media,
    override val isSelected: Boolean
) : Selectable {

//    override fun equals(other: Any?): Boolean {
//        if (other == null) return false
//        if (other !is UIMedia) return false
//        if (media.id == other.media.id && isSelected == other.isSelected) return true
//        return false
//    }
//
//    override fun hashCode(): Int {
//        var result = media.hashCode()
//        result = 31 * result + isSelected.hashCode()
//        return result
//    }

}