package kz.zhombie.bazaar.api

import kz.zhombie.bazaar.api.model.Media

interface ResultCallback {
    fun onCameraResult(media: Media)
    fun onMediaSelected(media: List<Media>)
}