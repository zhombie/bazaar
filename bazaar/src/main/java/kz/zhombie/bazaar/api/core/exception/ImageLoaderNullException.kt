package kz.zhombie.bazaar.api.core.exception

import kz.zhombie.bazaar.Bazaar
import kz.zhombie.bazaar.api.core.ImageLoader

class ImageLoaderNullException : IllegalStateException() {

    override val message: String
        get() = "${ImageLoader::class.java.simpleName} not initialized at ${Bazaar.TAG}"

}