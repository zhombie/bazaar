package kz.zhombie.bazaar

import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.result.ResultCallback

abstract class AbstractResultCallback : ResultCallback {
    override fun onCameraResult(media: Media) {
    }

    override fun onGalleryResult(media: Media) {
    }

    override fun onGalleryResult(media: List<Media>) {
    }

    override fun onMediaSelectResult(media: List<Media>) {
    }
}