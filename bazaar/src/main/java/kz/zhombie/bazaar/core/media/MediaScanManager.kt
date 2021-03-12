@file:Suppress("BlockingMethodInNonBlockingContext")

package kz.zhombie.bazaar.core.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.core.media.model.ImageBitmap
import kz.zhombie.bazaar.core.media.utils.*
import kz.zhombie.bazaar.core.media.utils.readFile
import kz.zhombie.bazaar.core.media.utils.readImage
import kz.zhombie.bazaar.core.media.utils.readOpenableImage
import kz.zhombie.bazaar.core.media.utils.readVideo
import kz.zhombie.bazaar.utils.*
import java.io.*
import java.util.*
import kotlin.math.min

internal class MediaScanManager constructor(private val context: Context) {

    companion object {
        private val TAG: String = MediaScanManager::class.java.simpleName

        private const val DEFAULT_LOCAL_LOAD_LIMIT = 3000

        private const val DEFAULT_MIME_TYPE_IMAGE = "image/jpeg"
        private const val DEFAULT_MIME_TYPE_VIDEO = "video/mp4"

        private const val REQUIRED_IMAGE_WIDTH = 512
    }

    suspend fun createCameraPictureInputTempFile(dispatcher: CoroutineDispatcher = Dispatchers.IO): Image? =
        withContext(dispatcher) {
            try {
                Logger.d(TAG, "createCameraPictureInputTempFile()")

                // Must be same as <cache-path name="*" path="*" />
                val folder = "camera"
                val directory = File(context.cacheDir, folder).apply { mkdirs() }

                val filename = createImageFilename()
                val extension = "jpg"
                val file = File.createTempFile("${filename}_", ".$extension", directory)
                file.deleteOnExit()
                val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

                val timestamp = System.currentTimeMillis()

                Logger.d(TAG, "createCameraPictureInputTempFile() -> file.absolutePath: ${file.absolutePath}")

                Image(
                    id = timestamp,
                    uri = uri,
                    path = file.absolutePath,
                    title = file.name,
                    displayName = file.name,
                    mimeType = uri.gainMimeType(DEFAULT_MIME_TYPE_IMAGE),
                    extension = extension,
                    size = file.length(),
                    dateAdded = timestamp,
                    dateModified = timestamp,
                    dateCreated = timestamp,
                    thumbnail = null,
                    folderId = null,
                    folderDisplayName = folder,
                    width = 0,
                    height = 0,
                    source = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    suspend fun createCameraVideoInputTempFile(dispatcher: CoroutineDispatcher = Dispatchers.IO): Video? =
        withContext(dispatcher) {
            try {
                // Must be same as <cache-path name="*" path="*" />
                val folder = "camera"
                val directory = File(context.cacheDir, folder).apply { mkdirs() }

                val filename = createVideoFilename()
                val extension = "mp4"
                val file = File.createTempFile("${filename}_", ".$extension", directory)
                file.deleteOnExit()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

                val timestamp = System.currentTimeMillis()

                Video(
                    id = timestamp,
                    uri = uri,
                    path = file.absolutePath,
                    title = file.name,
                    displayName = file.name,
                    mimeType = uri.gainMimeType(DEFAULT_MIME_TYPE_IMAGE),
                    extension = extension,
                    size = file.length(),
                    dateAdded = timestamp,
                    dateModified = timestamp,
                    dateCreated = timestamp,
                    thumbnail = null,
                    folderId = null,
                    folderDisplayName = folder,
                    width = 0,
                    height = 0,
                    duration = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
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
        uris: List<Uri>
    ): List<Image> = withContext(dispatcher) {
        return@withContext uris.mapNotNull { uri ->
            loadLocalSelectedMediaGalleryImage(dispatcher, uri)
        }
    }

    suspend fun loadLocalSelectedMediaGalleryImage(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uri: Uri
    ): Image? = withContext(dispatcher) {
        try {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                var image: Image? = null

                // Create new file from uri (content://...)
                val filename = createImageFilename()
                val file = (uri.transformLocalMediaGalleryItemToFile(dispatcher, filename) ?: return@withContext null)

                // Retrieve local info from MediaStore
                val projection = ContentResolverCompat.getOpenableContentProjection()

                context.contentResolver
                    ?.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        image = cursor.readOpenableImage(uri, file)
                    }

                // Set file path
                image = image?.copy(path = file.absolutePath)

                // Set MimeType
                image = image?.copy(mimeType = uri.gainMimeType(DEFAULT_MIME_TYPE_IMAGE))

                // Set extension
                val extension = file.getExtension(mimeType = image?.mimeType)
                if (!extension.isNullOrBlank()) {
                    image = image?.copy(extension = extension)
                }

                // Retrieve additional metadata from bitmap
                try {
                    val imageScale = decodeScaledBitmap(uri)
                    Logger.d(TAG, "decodeScaledBitmap() -> imageScale: $imageScale")
                    if (imageScale == null) {
                        context.contentResolver
                            ?.openFileDescriptor(uri, "r")
                            ?.use {
                                val bitmap: Bitmap? = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                                if (bitmap != null) {
                                    image = image?.copy(
                                        width = bitmap.width,
                                        height = bitmap.height,
                                        thumbnail = bitmap,
                                        source = bitmap
                                    )
                                }
                            }
                    } else {
                        image = image?.copy(
                            width = imageScale.source.size.width,
                            height = imageScale.source.size.height,
                            thumbnail = imageScale.processed.bitmap,
                            source = imageScale.source.bitmap
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return@withContext image
            } else {
                throw UnsupportedOperationException("Unsupported uri.scheme!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun loadLocalSelectedMediaGalleryVideos(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uris: List<Uri>
    ): List<Video> = withContext(dispatcher) {
        return@withContext uris.mapNotNull { uri ->
            loadLocalSelectedMediaGalleryVideo(dispatcher, uri)
        }
    }

    suspend fun loadLocalSelectedMediaGalleryVideo(
        dispatcher: CoroutineDispatcher,
        uri: Uri
    ): Video? = withContext(dispatcher) {
        try {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                var video: Video? = null

                // Create new file from uri (content://...)
                val filename = createVideoFilename()
                val file = (uri.transformLocalMediaGalleryItemToFile(dispatcher, filename) ?: return@withContext null)

                Logger.d(TAG, "Created local file: $file")

                // Retrieve local info from MediaStore
                val projection = ContentResolverCompat.getOpenableContentProjection()

                context.contentResolver
                    ?.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        video = cursor.readOpenableVideo(uri, file)
                    }

                Logger.d(TAG, "Scanned by MediaStore: $video")

                // Set file path
                video = video?.copy(path = file.absolutePath)

                // Set MimeType
                video = video?.copy(mimeType = uri.gainMimeType(DEFAULT_MIME_TYPE_VIDEO))

                // Set extension
                val extension = file.getExtension(mimeType = video?.mimeType)
                if (!extension.isNullOrBlank()) {
                    video = video?.copy(extension = extension)
                }

                // Retrieve additional metadata from uri
                val metadata = video?.uri.retrieveVideoMetadata(context, dispatcher)
                if (metadata != null) {
                    video = video?.copy(
                        width = metadata.width,
                        height = metadata.height,
                        duration = metadata.duration,
                        thumbnail = metadata.thumbnail
                    )
                }

                Logger.d(TAG, "video: $video")

                return@withContext video
            } else {
                throw UnsupportedOperationException("Unsupported uri.scheme!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun loadLocalSelectedMediaGalleryImagesOrVideos(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uris: List<Uri>
    ): List<Media> = withContext(dispatcher) {
        return@withContext uris.mapNotNull { uri ->
            loadLocalSelectedMediaGalleryImageOrVideo(dispatcher, uri)
        }
    }

    suspend fun loadLocalSelectedMediaGalleryImageOrVideo(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        uri: Uri
    ): Media? = withContext(dispatcher) {
        val mimeType = uri.getMimeType(context)?.toLowerCase(Locale.ROOT)
        Logger.d(TAG, "loadLocalSelectedMediaGalleryImageOrVideo() -> mimeType: $mimeType")
        if (mimeType.isNullOrBlank()) {
            return@withContext null
        }
        return@withContext when {
            mimeType.startsWith("image") ->
                loadLocalSelectedMediaGalleryImage(dispatcher, uri)
            mimeType.startsWith("video") ->
                loadLocalSelectedMediaGalleryVideo(dispatcher, uri)
            else ->
                null
        }
    }

    private suspend fun Uri.transformLocalMediaGalleryItemToFile(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        filename: String
    ): File? = withContext(dispatcher) {
        Logger.d(TAG, "transformLocalMediaGalleryItemToFile() -> filename: $filename")

        val file = File(context.cacheDir, filename)

        Logger.d(TAG, "Create local file [$file] with name $filename")

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

    suspend fun decodeFile(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        image: Image
    ): Image = withContext(dispatcher) {
        val bitmap = BitmapFactory.decodeFile(image.path)
        Logger.d(TAG, "takenImage: $bitmap, ${bitmap.width} x ${bitmap.height}")
        val size = if (!image.path.isNullOrBlank()) {
            File(image.path).length()
        } else {
            bitmap.allocationByteCount.toLong()
        }
        val width = bitmap.width
        val height = bitmap.height
        val bytes = ByteArrayOutputStream()
        var thumbnail = bitmap
        if (bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bytes)) {
            thumbnail = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())
        }
        return@withContext image.copy(
            size = size,
            thumbnail = thumbnail,
            width = width,
            height = height,
            source = bitmap
        )
    }

    suspend fun deleteFile(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        image: Image
    ): Boolean = withContext(dispatcher) {
        if (!image.path.isNullOrBlank()) {
            val file = File(image.path)
            return@withContext if (file.exists()) file.delete() else false
        }
        return@withContext false
    }

    @Throws(FileNotFoundException::class)
    private fun getImageSize(inputStream: InputStream): ImageBitmap.Size {
        Logger.d(TAG, "getImageSize() -> inputStream: $inputStream")
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)
        return ImageBitmap.Size(options.outWidth, options.outHeight)
    }

    private fun calculateSampleSize(currentWidth: Int, requiredWidth: Int): Int {
        var inSampleSize = 1
        if (currentWidth > requiredWidth) {
            val halfWidth = currentWidth / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps
            // width larger than the requested width
            while (halfWidth / inSampleSize >= requiredWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @Throws(IllegalArgumentException::class)
    private fun decodeScaledBitmap(uri: Uri): ImageBitmap? {
        Logger.d(TAG, "decodeScaledBitmap() -> uri: $uri")

        val inputStream = context.contentResolver?.openInputStream(uri) ?: return null

        val size = getImageSize(inputStream)
        val sourceBitmap = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options())
        val source = ImageBitmap.Source(sourceBitmap, size)

        val requiredWidth = min(REQUIRED_IMAGE_WIDTH, size.width)
        val sourceWidth = size.width
        val sampleSize = calculateSampleSize(sourceWidth, requiredWidth)

        val options = BitmapFactory.Options()
        options.inSampleSize = sampleSize
        options.inDensity = sourceWidth
        options.inTargetDensity = requiredWidth * sampleSize

        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        // reset density to display bitmap correctly
        bitmap?.density = context.resources.displayMetrics.densityDpi

        val imageBitmap = ImageBitmap(source, ImageBitmap.Processed(bitmap))
        return if (imageBitmap.source.bitmap == null && imageBitmap.processed.bitmap == null) {
            null
        } else {
            imageBitmap
        }
    }

}