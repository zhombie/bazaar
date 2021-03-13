package kz.zhombie.bazaar.api.result

import kz.zhombie.bazaar.api.model.Multimedia
import kz.zhombie.bazaar.api.model.Media

/**
 * [ResultCallback] implementation
 */
interface AbstractResultCallback : ResultCallback {
    override fun onCameraResult(media: Media) = onMediaSelectResult(listOf(media))

    override fun onLocalMediaStoreResult(media: Media) = onMediaSelectResult(listOf(media))
    override fun onLocalMediaStoreResult(media: List<Media>) = onMediaSelectResult(media)
    override fun onLocalMediaStoreResult(multimedia: Multimedia) = onMultimediaSelectResult(listOf(multimedia))

    override fun onMediaGallerySelectResult(media: List<Media>) = onMediaSelectResult(media)
    override fun onMultimediaGallerySelectResult(multimedia: List<Multimedia>) = onMultimediaSelectResult(multimedia)

    // ------------------------------------------------------

    fun onMultimediaSelectResult(multimedia: List<Multimedia>)
    fun onMediaSelectResult(media: List<Media>)
}