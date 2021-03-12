package kz.zhombie.bazaar.api.result

import kz.zhombie.bazaar.api.model.Entity
import kz.zhombie.bazaar.api.model.Media

/**
 * [ResultCallback] implementation
 */
interface AbstractResultCallback : ResultCallback {
    override fun onCameraResult(media: Media) = onMediaGalleryResult(listOf(media))
    override fun onLocalMediaGalleryResult(media: Media) = onMediaGalleryResult(listOf(media))
    override fun onLocalMediaGalleryResult(media: List<Media>) = onMediaGalleryResult(media)
    override fun onLocalEntityResult(entity: Entity) = onLocalEntityResult(listOf(entity))
    override fun onMediaGallerySelectResult(media: List<Media>) = onMediaGalleryResult(media)

    fun onLocalEntityResult(entity: List<Entity>)
    fun onMediaGalleryResult(media: List<Media>)
}