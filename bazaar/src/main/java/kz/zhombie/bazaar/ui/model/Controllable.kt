package kz.zhombie.bazaar.ui.model

internal interface Controllable {
    val isSelectable: Boolean
    val isSelected: Boolean
    val isVisible: Boolean
}