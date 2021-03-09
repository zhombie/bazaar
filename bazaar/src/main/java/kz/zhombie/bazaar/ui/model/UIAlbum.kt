package kz.zhombie.bazaar.ui.model

import kz.zhombie.bazaar.api.model.Album

internal data class UIAlbum constructor(
    val album: Album
) {

    companion object {
        const val ALL_MEDIA_ID = 0L
    }

}