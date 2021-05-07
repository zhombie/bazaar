package kz.zhombie.bazaar.core.cache

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Multimedia
import kz.zhombie.bazaar.core.logging.Logger
import java.util.*

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

    private var multimedia: MutableList<Multimedia>? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private fun ensureMultimediaExistence() {
        if (multimedia == null) {
            multimedia = mutableListOf()
        }
    }

    fun isEmpty(): Boolean {
        return multimedia.isNullOrEmpty()
    }

    suspend fun getMultimedia(): List<Multimedia>? = withContext(Dispatchers.IO + exceptionHandler) {
        Logger.d(TAG, "getMultimedia() -> multimedia: ${multimedia?.size}")
        return@withContext multimedia
    }

    suspend fun setMultimedia(multimedia: List<Multimedia>) = withContext(Dispatchers.IO + exceptionHandler) {
        Logger.d(TAG, "setMultimedia() -> multimedia: ${multimedia.size}")
        ensureMultimediaExistence()
        multimedia.forEach {
            this@Cache.multimedia?.addIfDoesNotContain(it)
        }
    }

    suspend fun getMedia(): List<Media>? = withContext(Dispatchers.IO + exceptionHandler) {
        val media = multimedia?.filterIsInstance<Media>()
        Logger.d(TAG, "getMedia() -> media: ${media?.size}")
        return@withContext media
    }

    suspend fun setMedia(media: List<Media>) = withContext(Dispatchers.IO + exceptionHandler) {
        Logger.d(TAG, "setMedia() -> media: ${media.size}")
        setMultimedia(media)
    }

    suspend fun clear(): Boolean = withContext(Dispatchers.IO + exceptionHandler) {
        multimedia?.clear()
        multimedia = null

        return@withContext multimedia.isNullOrEmpty()
    }

    suspend fun destroy() {
        clear()
        INSTANCE = null
    }

    private fun <E> MutableCollection<E>.addIfDoesNotContain(e: E): Boolean {
        if (e !in this) {
            return add(e)
        }
        return false
    }

}