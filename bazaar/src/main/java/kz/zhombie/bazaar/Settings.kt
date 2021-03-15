package kz.zhombie.bazaar

import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.exception.ImageLoaderNullException

internal object Settings {

    private var imageLoader: ImageLoader? = null

    fun hasImageLoader(): Boolean {
        return imageLoader != null
    }

    fun getImageLoader(): ImageLoader {
        return requireNotNull(imageLoader) { ImageLoaderNullException() }
    }

    fun setImageLoader(imageLoader: ImageLoader) {
        this.imageLoader = imageLoader
    }

}