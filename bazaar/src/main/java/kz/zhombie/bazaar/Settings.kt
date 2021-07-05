package kz.zhombie.bazaar

import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.exception.ImageLoaderNullException

internal object Settings {

    private var permanentImageLoader: ImageLoader? = null
    private var temporaryImageLoader: ImageLoader? = null
    private var isLoggingEnabled: Boolean = false

    fun getImageLoader(): ImageLoader =
        getTemporaryImageLoader() ?: getPermanentImageLoader()

    fun hasPermanentImageLoader(): Boolean =
        permanentImageLoader != null

    fun getPermanentImageLoader(): ImageLoader =
        requireNotNull(permanentImageLoader) { ImageLoaderNullException() }

    fun setPermanentImageLoader(imageLoader: ImageLoader) {
        this.permanentImageLoader = imageLoader
    }

    fun hasTemporaryImageLoader(): Boolean =
        temporaryImageLoader != null

    fun getTemporaryImageLoader(): ImageLoader? =
        temporaryImageLoader

    fun setTemporaryImageLoader(imageLoader: ImageLoader) {
        this.temporaryImageLoader = imageLoader
    }

    fun isLoggingEnabled(): Boolean = isLoggingEnabled

    fun setLoggingEnabled(isLoggingEnabled: Boolean) {
        this.isLoggingEnabled = isLoggingEnabled
    }

    fun cleanupTemporarySettings() {
        temporaryImageLoader = null
    }

    fun cleanupSettings() {
        permanentImageLoader = null
        temporaryImageLoader = null
        isLoggingEnabled = false
    }

}