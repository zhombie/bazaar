package kz.zhombie.bazaar.api

import kz.zhombie.bazaar.api.model.Media

interface ResultCallback {
    // Media captured by camera
    fun onCameraResult(media: Media)

    // Media selected from local gallery
    fun onGalleryResult(media: Media)

    // Media selected from offered custom gallery
    fun onMediaSelectResult(media: List<Media>)
}