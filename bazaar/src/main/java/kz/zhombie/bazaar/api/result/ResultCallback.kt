package kz.zhombie.bazaar.api.result

import kz.zhombie.multimedia.model.Content
import kz.zhombie.multimedia.model.Media

interface ResultCallback {
    // Media captured by camera
    fun onCameraResult(media: Media)

    // Media selected from local media store
    fun onMediaResult(media: Media)
    fun onMediaResult(media: List<Media>)

    // Other types
    fun onContentResult(content: Content)
    fun onContentsResult(contents: List<Content>)

    // Media selected from offered custom media gallery
    fun onGalleryMediaResult(media: List<Media>)
    fun onGalleryContentsResult(contents: List<Content>)
}