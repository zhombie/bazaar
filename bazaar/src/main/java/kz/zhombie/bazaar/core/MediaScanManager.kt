@file:Suppress("BlockingMethodInNonBlockingContext")

package kz.zhombie.bazaar.core

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.utils.*
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

        private const val DEFAULT_MIME_TYPE_IMAGE = "image/jpeg"
        private const val DEFAULT_MIME_TYPE_VIDEO = "video/mp4"
    }

    fun createCameraPictureInputTempFile(): Image? = try {
        // Must be same as <cache-path name="*" path="*" />
        val folder = "camera"
        val directory = File(context.cacheDir, folder).apply { mkdirs() }

        val filename = createImageFilename()
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
            mimeType = uri.gainMimeType(DEFAULT_MIME_TYPE_IMAGE),
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

    fun createCameraVideoInputTempFile(): Video? = try {
        // Must be same as <cache-path name="*" path="*" />
        val folder = "camera"
        val directory = File(context.cacheDir, folder).apply { mkdirs() }

        val filename = createVideoFilename()
        val file = File.createTempFile("${filename}_", ".mp4", directory)
        file.deleteOnExit()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val timestamp = System.currentTimeMillis()

        Video(
            id = timestamp,
            uri = uri,
            title = file.name,
            displayName = file.name,
            size = file.length(),
            dateAdded = timestamp,
            dateModified = timestamp,
            dateCreated = timestamp,
            mimeType = uri.gainMimeType(DEFAULT_MIME_TYPE_IMAGE),
            width = 0,
            height = 0,
            thumbnail = null,
            folderId = null,
            folderDisplayName = folder,
            duration = null,
            cover = null
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
        val projection: Array<String> = ContentResolverCompat.getProjection(ContentResolverCompat.Type.IMAGE)
        val selection: String? = null
        val selectionArgs: MutableList<String>? = null
        val sortOrder = "${MediaStore.Images.ImageColumns.DATE_ADDED} DESC LIMIT $DEFAULT_LOCAL_LOAD_LIMIT"

        context.contentResolver
            ?.query(uri, projection, selection, selectionArgs?.toTypedArray(), sortOrder)
            ?.use { cursor ->
                val data = cursor.mapTo(dispatcher, Image::class.java)
                callback(data)
            }
    }

    suspend fun loadLocalVideos(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        callback: (media: List<Media>) -> Unit
    ) = withContext(dispatcher) {
        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection: Array<String> = ContentResolverCompat.getProjection(ContentResolverCompat.Type.VIDEO)
        val selection: String? = null
        val selectionArgs: MutableList<String>? = null
        val sortOrder = "${MediaStore.Video.VideoColumns.DATE_ADDED} DESC LIMIT $DEFAULT_LOCAL_LOAD_LIMIT"

        context.contentResolver
            ?.query(uri, projection, selection, selectionArgs?.toTypedArray(), sortOrder)
            ?.use { cursor ->
                val data = cursor.mapTo(dispatcher, Video::class.java)
                callback(data)
            }
    }

    suspend fun loadLocalImagesAndVideos(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        callback: (media: List<Media>) -> Unit
    ) = withContext(dispatcher) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val projection: Array<String> = ContentResolverCompat.getProjection(ContentResolverCompat.Type.FILE)
        val selection = (
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            + " OR " +
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        )
        val selectionArgs: MutableList<String>? = null
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC LIMIT $DEFAULT_LOCAL_LOAD_LIMIT"

        context.contentResolver
            ?.query(uri, projection, selection, selectionArgs?.toTypedArray(), sortOrder)
            ?.use { cursor ->
                val data = cursor.mapTo(dispatcher, Media::class.java)
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
                        Media::class.java -> this@mapTo.readFile()
                        else -> null
                    }
                }
        )
        return@withContext array
    }

    suspend fun loadLocalSelectedMediaGalleryImages(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uris: List<Uri>,
        callback: (images: List<Image>) -> Unit
    ) = withContext(dispatcher) {
        val images = mutableListOf<Image>()
        uris.forEach { uri ->
            loadLocalSelectedMediaGalleryImage(dispatcher, uri) { image ->
                images.add(image)
            }
        }
        if (!images.isNullOrEmpty()) {
            callback(images)
        }
    }

    suspend fun loadLocalSelectedMediaGalleryImage(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uri: Uri,
        callback: (image: Image) -> Unit
    ) = withContext(dispatcher) {
        try {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                var image: Image? = null

                // Create new file from uri (content://...)
                val filename = createImageFilename()
                val file = uri.transformLocalMediaGalleryItemToFile(dispatcher, filename) ?: return@withContext

                // Retrieve local info from MediaStore
                val projection = ContentResolverCompat.getOpenableContentProjection()

                context.contentResolver
                    ?.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        image = cursor.readOpenableImage(uri, file)
                    }

                // Set MimeType
                image = image?.copy(mimeType = uri.gainMimeType(DEFAULT_MIME_TYPE_IMAGE))

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

    suspend fun loadLocalSelectedMediaGalleryVideos(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uris: List<Uri>,
        callback: (videos: List<Video>) -> Unit
    ) = withContext(dispatcher) {
        val videos = mutableListOf<Video>()
        uris.forEach { uri ->
            loadLocalSelectedMediaGalleryVideo(dispatcher, uri) { video ->
                videos.add(video)
            }
        }
        if (!videos.isNullOrEmpty()) {
            callback(videos)
        }
    }

    suspend fun loadLocalSelectedMediaGalleryVideo(
        dispatcher: CoroutineDispatcher,
        uri: Uri,
        callback: (video: Video) -> Unit
    ) = withContext(dispatcher) {
        try {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                var video: Video? = null

                // Create new file from uri (content://...)
                val filename = createVideoFilename()
                val file = uri.transformLocalMediaGalleryItemToFile(dispatcher, filename) ?: return@withContext

                Logger.d(TAG, "Created local file: $file")

                // Retrieve local info from MediaStore
                val projection = ContentResolverCompat.getOpenableContentProjection()

                context.contentResolver
                    ?.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        video = cursor.readOpenableVideo(uri, file)
                    }

                Logger.d(TAG, "Scanned by MediaStore: $video")

                // Set MimeType
                video = video?.copy(mimeType = uri.gainMimeType(DEFAULT_MIME_TYPE_VIDEO))

                // Retrieve additional metadata from bitmap
                try {
                    context.contentResolver
                        ?.openFileDescriptor(uri, "r")
                        ?.use {
                            val bitmap: Bitmap? = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                            if (bitmap != null) {
                                video = video?.copy(width = bitmap.width, height = bitmap.height)
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Logger.d(TAG, "video: $video")

                video?.let {
                    callback(it)
                }
            } else {
                throw UnsupportedOperationException("Unsupported uri.scheme!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun Uri.transformLocalMediaGalleryItemToFile(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        filename: String
    ): File? = withContext(dispatcher) {
        Logger.d(TAG, "transformLocalMediaGalleryItemToFile() -> filename: $filename")

        val file = File(context.cacheDir, filename)

        Logger.d(TAG, "Created local file [$file] with name $filename")

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
            val inputStream = context.contentResolver?.openInputStream(this@transformLocalMediaGalleryItemToFile)

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

    private fun createImageFilename(): String {
        val timestamp = System.currentTimeMillis()
        return "IMG_${timestamp}"
    }

    private fun createVideoFilename(): String {
        val timestamp = System.currentTimeMillis()
        return "VIDEO_${timestamp}"
    }

    private fun Uri.gainMimeType(default: String): String {
        return context.contentResolver?.getType(this) ?: default
    }

}