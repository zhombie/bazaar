package kz.zhombie.bazaar.core.cache

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.garage.multimedia.store.model.Content
import kz.garage.multimedia.store.model.Media
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

        fun destroyInstance() {
            INSTANCE = null
        }
    }

    private var contents: MutableList<Content>? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private val io = Dispatchers.Default + exceptionHandler

    private fun ensureContentsExistence() {
        if (contents == null) {
            contents = mutableListOf()
        }
    }

    fun isEmpty(): Boolean {
        return contents.isNullOrEmpty()
    }

    suspend fun getContents(): List<Content>? = withContext(io) {
        Logger.debug(TAG, "getContents() -> contents: ${contents?.size}")
        return@withContext contents
    }

    suspend fun setContents(content: List<Content>) = withContext(io) {
        Logger.debug(TAG, "setContents() -> contents: ${content.size}")
        ensureContentsExistence()
        content.forEach {
            this@Cache.contents?.addIfDoesNotContain(it)
        }
    }

    suspend fun getMedia(): List<Media>? = withContext(io) {
        val media = contents?.filterIsInstance<Media>()
        Logger.debug(TAG, "getMedia() -> media: ${media?.size}")
        return@withContext media
    }

    suspend fun setMedia(media: List<Media>) = withContext(io) {
        Logger.debug(TAG, "setMedia() -> media: ${media.size}")
        setContents(media)
    }

    suspend fun clear(): Boolean = withContext(io) {
        contents?.clear()
        contents = null

        return@withContext contents.isNullOrEmpty()
    }

    suspend fun destroy() {
        clear()
        destroyInstance()
    }

    private fun <E> MutableCollection<E>.addIfDoesNotContain(e: E): Boolean {
        if (e !in this) {
            return add(e)
        }
        return false
    }

}