package kz.zhombie.bazaar.core

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.utils.ContentResolverCompat
import kz.zhombie.bazaar.utils.readImage
import kz.zhombie.bazaar.utils.readVideo
import java.io.File
import java.util.*

internal class MediaScanManager constructor(private val context: Context) {

    companion object {
        private val TAG: String = MediaScanManager::class.java.simpleName
    }

    fun createCameraInputTempFile(): Image? = try {
        val timestamp = System.currentTimeMillis()
        val folder = "camera"
        val directory = File(context.cacheDir, folder).apply { mkdirs() }
        val file = File.createTempFile("IMG_${timestamp}_", ".jpg", directory)
        file.deleteOnExit()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        Image(
            id = timestamp,
            uri = uri,
            title = file.name,
            displayName = file.name,
            size = file.length(),
            dateAdded = timestamp,
            dateModified = timestamp,
            dateCreated = timestamp,
            mimeType = context.contentResolver.getType(uri) ?: "image/jpeg",
            width = 0,
            height = 0,
            thumbnail = null,
            folderId = null,
            folderDisplayName = folder
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    suspend fun loadImages(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        callback: (media: List<Media>) -> Unit
    ) = withContext(dispatcher) {
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = ContentResolverCompat.getProjection(ContentResolverCompat.Type.IMAGE)
        val selection: String? = null
        val selectionArgs: MutableList<String>? = null
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 3000"

        context.contentResolver
            ?.query(uri, projection, selection, selectionArgs?.toTypedArray(), sortOrder)
            ?.use { cursor ->
                val data = cursor.mapTo(dispatcher, Image::class.java)
                callback(data)
            }
    }

    private suspend fun <T> Cursor.mapTo(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        clazz: Class<T>
    ): List<Media> = withContext(dispatcher) {
        Logger.d(TAG, "$count items to $clazz")
        val array = arrayListOf<Media>()
        array.addAll(
            generateSequence { if (moveToNext()) this else null }
                .mapNotNull {
                    when (clazz) {
                        Image::class.java -> this@mapTo.readImage()
                        Video::class.java -> this@mapTo.readVideo()
                        else -> null
                    }
                }
        )
        return@withContext array
    }

}