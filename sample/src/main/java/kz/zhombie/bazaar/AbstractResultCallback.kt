package kz.zhombie.bazaar

import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.result.ResultCallback

abstract class AbstractResultCallback : ResultCallback {
    override fun onCameraResult(media: Media) {
    }

    override fun onLocalMediaGalleryResult(media: Media) {
    }

    override fun onLocalMediaGalleryResult(media: List<Media>) {
    }

    override fun onMediaGallerySelectResult(media: List<Media>) {
    }
}