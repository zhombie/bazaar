package kz.zhombie.bazaar.api.result

import kz.zhombie.multimedia.model.Content
import kz.zhombie.multimedia.model.Media

/**
 * [ResultCallback] implementation
 */
interface AbstractResultCallback : ResultCallback {
    override fun onCameraResult(media: Media) =
        onSelectResult(listOf(media))

    override fun onMediaResult(media: Media) =
        onSelectResult(listOf(media))

    override fun onMediaResult(media: List<Media>) =
        onSelectResult(media)

    override fun onContentResult(content: Content) =
        onSelectResult(listOf(content))

    override fun onContentsResult(contents: List<Content>) =
        onSelectResult(contents)

    override fun onGalleryMediaResult(media: List<Media>) =
        onSelectResult(media)

    override fun onGalleryContentsResult(contents: List<Content>) =
        onSelectResult(contents)

    // ------------------------------------------------------

    fun onSelectResult(contents: List<Content>)
}