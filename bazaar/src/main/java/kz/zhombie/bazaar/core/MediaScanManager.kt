@file:Suppress("BlockingMethodInNonBlockingContext")

package kz.zhombie.bazaar.core

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kz.zhombie.bazaar.utils.readOpenableImage
import kz.zhombie.bazaar.utils.readVideo
import java.io.*
import java.util.*

internal class MediaScanManager constructor(private val context: Context) {

    companion object {
        private val TAG: String = MediaScanManager::class.java.simpleName

        private const val DEFAULT_LOCAL_LOAD_LIMIT = 3000
    }

    fun createCameraInputTempFile(): Image? = try {
        // Must be same as <cache-path name="*" path="*" />
        val folder = "camera"
        val directory = File(context.cacheDir, folder).apply { mkdirs() }

        val filename = createFilename()
        val file = File.createTempFile("${filename}_", ".jpg", directory)
        file.deleteOnExit()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val timestamp = System.currentTimeMillis()

        Image(
            id = timestamp,
            uri = uri,
            title = file.name,
            displayName = file.name,
            size = file.length(),
            dateAdded = timestamp,
            dateModified = timestamp,
            dateCreated = timestamp,
            mimeType = context.contentResolver?.getType(uri) ?: "image/jpeg",
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

    suspend fun loadLocalImages(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        callback: (media: List<Media>) -> Unit
    ) = withContext(dispatcher) {
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = ContentResolverCompat.getProjection(ContentResolverCompat.Type.IMAGE)
        val selection: String? = null
        val selectionArgs: MutableList<String>? = null
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $DEFAULT_LOCAL_LOAD_LIMIT"

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

    suspend fun loadSelectedGalleryImages(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uris: List<Uri>,
        callback: (images: List<Image>) -> Unit
    ) = withContext(dispatcher) {
        val images = mutableListOf<Image>()
        uris.forEach { uri ->
            loadSelectedGalleryImage(dispatcher, uri) { image ->
                images.add(image)
            }
        }
        if (!images.isNullOrEmpty()) {
            callback(images)
        }
    }

    suspend fun loadSelectedGalleryImage(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uri: Uri,
        callback: (image: Image) -> Unit
    ) = withContext(dispatcher) {
        try {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                var image: Image? = null

                // Create new file from uri (content://...)
                val file = uri.transformGalleryImageToLocalFile(dispatcher) ?: return@withContext

                // Retrieve local info from MediaStore
                val projection = ContentResolverCompat.getOpenableContentProjection()

                context.contentResolver
                    ?.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        image = cursor.readOpenableImage(uri, file)
                    }

                // Set MimeType
                image = image?.copy(mimeType = context.contentResolver?.getType(uri) ?: "image/jpeg")

                // Retrieve additional metadata from bitmap
                try {
                    context.contentResolver
                        ?.openFileDescriptor(uri, "r")
                        ?.use {
                            val bitmap: Bitmap? = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                            if (bitmap != null) {
                                image = image?.copy(width = bitmap.width, height = bitmap.height)
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                image?.let {
                    callback(it)
                }
            } else {
                throw UnsupportedOperationException("Unsupported uri.scheme!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun Uri.transformGalleryImageToLocalFile(
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): File? = withContext(dispatcher) {
        val filename = createFilename()
        val file = File(context.cacheDir, filename)

        // If file could not be created, then there is no need to continue code flow
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        try {
            val outputStream = FileOutputStream(file)
            val inputStream = context.contentResolver?.openInputStream(this@transformGalleryImageToLocalFile)

            inputStream?.use {
                copy(dispatcher, inputStream, outputStream)
            }

            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext file
    }

    @Throws(IOException::class)
    private suspend fun copy(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        source: InputStream,
        target: OutputStream
    ) = withContext(dispatcher) {
        val buffer = ByteArray(8192)
        var length: Int
        while (source.read(buffer).also { length = it } > 0) {
            target.write(buffer, 0, length)
        }
    }

    private fun createFilename(): String {
        val timestamp = System.currentTimeMillis()
        return "IMG_${timestamp}"
    }

}