package kz.zhombie.bazaar.core.cache

import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Multimedia
import kz.zhombie.bazaar.core.logging.Logger

internal class Cache private constructor() {

    companion object {
        private val TAG = Cache::class.java.simpleName

        @Volatile
        private var INSTANCE: Cache? = null

        fun getInstance(): Cache =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Cache().also { INSTANCE = it }
            }
    }

    private var multimedia: List<Multimedia>? = null
    private var media: List<Media>? = null

    suspend fun getMultimedia(): List<Multimedia>? {
        Logger.d(TAG, "getMultimedia() -> multimedia: ${media?.size}")
        return multimedia
    }

    suspend fun setMultimedia(multimedia: List<Multimedia>) {
        this.multimedia = multimedia
    }

    suspend fun getMedia(): List<Media>? {
        Logger.d(TAG, "getMedia() -> media: ${media?.size}")
        return media
    }

    suspend fun setMedia(media: List<Media>) {
        this.media = media
    }

    suspend fun clear() {
        media = null
        multimedia = null
    }

    suspend fun destroy() {
        clear()
        INSTANCE = null
    }

}