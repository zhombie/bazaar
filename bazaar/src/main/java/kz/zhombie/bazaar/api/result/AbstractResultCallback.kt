package kz.zhombie.bazaar.api.result

import kz.zhombie.bazaar.api.model.Media

/**
 * [ResultCallback] SAM method
 */
fun interface AbstractResultCallback : ResultCallback {
    override fun onCameraResult(media: Media) = onMediaGalleryResult(listOf(media))
    override fun onLocalMediaGalleryResult(media: Media) = onMediaGalleryResult(listOf(media))
    override fun onLocalMediaGalleryResult(media: List<Media>) = onMediaGalleryResult(media)
    override fun onMediaGallerySelectResult(media: List<Media>) = onMediaGalleryResult(media)

    fun onMediaGalleryResult(media: List<Media>)
}