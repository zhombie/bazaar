package kz.zhombie.bazaar.api.result

import kz.zhombie.bazaar.api.model.Multimedia
import kz.zhombie.bazaar.api.model.Media

interface ResultCallback {
    // Media captured by camera
    fun onCameraResult(media: Media)

    // Media selected from local media store
    fun onLocalMediaStoreResult(media: Media)
    fun onLocalMediaStoreResult(media: List<Media>)

    // Other types, not visual media. For example, Audio
    fun onLocalMediaStoreResult(multimedia: Multimedia)

    // Media selected from offered custom media gallery
    fun onMediaGallerySelectResult(media: List<Media>)
}