package kz.zhombie.bazaar.ui.model

import kz.zhombie.bazaar.api.model.Folder

internal data class UIFolder constructor(
    val folder: Folder
) {

    companion object {
        const val ALL_MEDIA_ID = 0L
    }

}