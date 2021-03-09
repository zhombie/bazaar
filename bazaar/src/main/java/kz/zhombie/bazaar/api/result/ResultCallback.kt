package kz.zhombie.bazaar.api.result

import kz.zhombie.bazaar.api.model.Media

interface ResultCallback {
    // Media captured by camera
    fun onCameraResult(media: Media)

    // Media selected from local gallery
    fun onGalleryResult(media: Media)
    fun onGalleryResult(media: List<Media>)

    // Media selected from offered custom gallery
    fun onMediaSelectResult(media: List<Media>)
}