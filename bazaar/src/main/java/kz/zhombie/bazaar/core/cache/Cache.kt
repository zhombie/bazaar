package kz.zhombie.bazaar.core.cache

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.multimedia.model.Content
import kz.zhombie.multimedia.model.Media
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

    private var contents: MutableList<Content>? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private fun ensureContentsExistence() {
        if (contents == null) {
            contents = mutableListOf()
        }
    }

    fun isEmpty(): Boolean {
        return contents.isNullOrEmpty()
    }

    suspend fun getContents(): List<Content>? = withContext(Dispatchers.IO + exceptionHandler) {
        Logger.d(TAG, "getContents() -> contents: ${contents?.size}")
        return@withContext contents
    }

    suspend fun setContents(content: List<Content>) = withContext(Dispatchers.IO + exceptionHandler) {
        Logger.d(TAG, "setContents() -> contents: ${content.size}")
        ensureContentsExistence()
        content.forEach {
            this@Cache.contents?.addIfDoesNotContain(it)
        }
    }

    suspend fun getMedia(): List<Media>? = withContext(Dispatchers.IO + exceptionHandler) {
        val media = contents?.filterIsInstance<Media>()
        Logger.d(TAG, "getMedia() -> media: ${media?.size}")
        return@withContext media
    }

    suspend fun setMedia(media: List<Media>) = withContext(Dispatchers.IO + exceptionHandler) {
        Logger.d(TAG, "setMedia() -> media: ${media.size}")
        setContents(media)
    }

    suspend fun clear(): Boolean = withContext(Dispatchers.IO + exceptionHandler) {
        contents?.clear()
        contents = null

        return@withContext contents.isNullOrEmpty()
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