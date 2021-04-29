package kz.zhombie.bazaar.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kz.zhombie.bazaar.R

internal class FunctionalButton private constructor(
    val type: Type,

    @DrawableRes
    val icon: Int,

    @StringRes
    val title: Int
) {

    companion object {
        fun camera(): FunctionalButton {
            return FunctionalButton(Type.CAMERA, R.drawable.bazaar_ic_camera, R.string.bazaar_camera)
        }

        fun chooseFromLibrary(): FunctionalButton {
            return FunctionalButton(Type.CHOOSE_FROM_LIBRARY, R.drawable.bazaar_ic_folder_yellow, R.string.bazaar_choose_from_library)
        }
    }

    enum class Type {
        CAMERA,
        CHOOSE_FROM_LIBRARY
    }

}