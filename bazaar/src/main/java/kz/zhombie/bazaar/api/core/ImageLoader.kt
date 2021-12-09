package kz.zhombie.bazaar.api.core

import kz.zhombie.museum.PaintingLoader

interface ImageLoader : PaintingLoader {
    fun interface Factory {
        fun getImageLoader(): ImageLoader
    }
}

typealias ImageRequest = PaintingLoader.Request
typealias ImageRequestBuilder = PaintingLoader.Request.Builder