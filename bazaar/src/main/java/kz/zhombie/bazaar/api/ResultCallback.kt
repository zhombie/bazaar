package kz.zhombie.bazaar.api

import kz.zhombie.bazaar.api.model.Media

interface ResultCallback {
    fun onMediaSelected(media: List<Media>)
}