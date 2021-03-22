package kz.zhombie.bazaar

import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.exception.ImageLoaderNullException

internal object Settings {

    private var imageLoader: ImageLoader? = null
    private var isLoggingEnabled: Boolean = false

    fun hasImageLoader(): Boolean {
        return imageLoader != null
    }

    fun getImageLoader(): ImageLoader {
        return requireNotNull(imageLoader) { ImageLoaderNullException() }
    }

    fun setImageLoader(imageLoader: ImageLoader) {
        this.imageLoader = imageLoader
    }

    fun isLoggingEnabled(): Boolean {
        return isLoggingEnabled
    }

    fun setLoggingEnabled(isLoggingEnabled: Boolean) {
        this.isLoggingEnabled = isLoggingEnabled
    }

}