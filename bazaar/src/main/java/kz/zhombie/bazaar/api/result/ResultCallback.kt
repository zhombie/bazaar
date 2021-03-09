package kz.zhombie.bazaar.api.result

import kz.zhombie.bazaar.api.model.Media

interface ResultCallback {
    // Media captured by camera
    fun onCameraResult(media: Media)

    // Media selected from local media gallery
    fun onLocalMediaGalleryResult(media: Media)
    fun onLocalMediaGalleryResult(media: List<Media>)

    // Media selected from offered custom media gallery
    fun onMediaGallerySelectResult(media: List<Media>)
}