package kz.zhombie.bazaar

import kz.zhombie.bazaar.api.ImageLoader

internal object Settings {

    private var imageLoader: ImageLoader? = null

    fun getImageLoader(): ImageLoader {
        return requireNotNull(imageLoader) {
            "${ImageLoader::class.java.simpleName} not initialized at ${Settings::class.java.simpleName}"
        }
    }

    fun setImageLoader(imageLoader: ImageLoader) {
        this.imageLoader = imageLoader
    }

}