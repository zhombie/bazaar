package kz.zhombie.bazaar.core.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.model.*
import kz.zhombie.bazaar.core.cache.Cache
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.core.media.model.ImageBitmap
import kz.zhombie.bazaar.core.media.utils.*
import kz.zhombie.bazaar.utils.*
import java.io.*
import java.util.*
import kotlin.math.min

internal class MediaScanManager constructor(private val context: Context) {

    companion object {
        private val TAG: String = MediaScanManager::class.java.simpleName

        private const val DEFAULT_LOCAL_LOAD_LIMIT = 500

        private const val DEFAULT_MIME_TYPE_IMAGE = "image/jpeg"
        private const val DEFAULT_MIME_TYPE_VIDEO = "video/mp4"

        private const val REQUIRED_IMAGE_WIDTH = 512

        suspend fun preload(context: Context, mode: Mode) {
            Logger.d(TAG, "preload()")
            val mediaScanManager = MediaScanManager(context)
            if (mode == Mode.AUDIO || mode == Mode.DOCUMENT) {
                val multimedia = when (mode) {
                    Mode.AUDIO -> mediaScanManager.loadLocalMediaAudios()
                    Mode.DOCUMENT -> null
                    else -> null
                }
                Logger.d(TAG, "preload() -> mode: $mode, multimedia: ${multimedia?.size}")
                if (!multimedia.isNullOrEmpty()) {
                    Cache.getInstance().setMultimedia(multimedia)
                }
            } else {
                val media = when (mode) {
                    Mode.IMAGE -> mediaScanManager.loadLocalMediaImages()
                    Mode.VIDEO -> mediaScanManager.loadLocalMediaVideos()
                    Mode.IMAGE_AND_VIDEO -> mediaScanManager.loadLocalMediaImagesAndVideos()
                    else -> null
                }
                Logger.d(TAG, "preload() -> mode: $mode, media: ${media?.size}")
                if (!media.isNullOrEmpty()) {
                    Cache.getInstance().setMedia(media)
                }
            }
        }

        suspend fun clearCache() {
            Logger.d(TAG, "clearCache()")
            Cache.getInstance().clear()
        }

        suspend fun destroyCache() {
            Logger.d(TAG, "destroyCache()")
            Cache.getInstance().destroy()
        }
    }

    suspend fun createCameraPictureInputTempFile(): Image? = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "createCameraPictureInputTempFile()")

            // Must be same as <cache-path name="*" path="*" />
            val folder = "camera"
            val directory = File(context.cacheDir, folder).apply { mkdirs() }

            val filename = createImageFilename()
            val extension = "jpg"
            val file = runCatching {
                File.createTempFile("${filename}_", ".$extension", directory)
            }.getOrNull() ?: return@withContext null
            file.deleteOnExit()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val timestamp = System.currentTimeMillis()

            Logger.d(TAG, "createCameraPictureInputTempFile() -> ${file.absolutePath}")

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

    suspend fun createCameraVideoInputTempFile(): Video? = withContext(Dispatchers.IO) {
        try {
            // Must be same as <cache-path name="*" path="*" />
            val folder = "camera"
            val directory = File(context.cacheDir, folder).apply { mkdirs() }

            val filename = createVideoFilename()
            val extension = "mp4"
            val file = runCatching {
                File.createTempFile("${filename}_", ".$extension", directory)
            }.getOrNull() ?: return@withContext null
            file.deleteOnExit()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

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

    suspend fun loadLocalMediaImages(): List<Media>? = withContext(Dispatchers.IO) {
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection: Array<String> = ContentResolverCompat.getProjection(ContentResolverCompat.Type.IMAGE)
        val selection = "(${MediaStore.Images.ImageColumns.MIME_TYPE}=? OR ${MediaStore.Images.ImageColumns.MIME_TYPE}=?) AND ${MediaStore.Images.ImageColumns.SIZE}>=?"
        val selectionArgs: Array<String> = arrayOf("image/jpeg", "image/png", "102400")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bundle = Bundle()

            // Selection
            bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)

            // Limit & offset
            bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, DEFAULT_LOCAL_LOAD_LIMIT)
            bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, 0)

            // Sort
            bundle.putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.ImageColumns.DATE_ADDED)
            )
            bundle.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )

            context.contentResolver
                ?.query(uri, projection, bundle, null)
                ?.use { cursor ->
                    return@withContext cursor.mapTo<Image>()
                }
        } else {
            val sortOrder = "${MediaStore.Images.ImageColumns.DATE_ADDED} DESC LIMIT $DEFAULT_LOCAL_LOAD_LIMIT"

            context.contentResolver
                ?.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    return@withContext cursor.mapTo<Image>()
                }
        }

        return@withContext null
    }

    suspend fun loadLocalMediaVideos(): List<Media>? = withContext(Dispatchers.IO) {
        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection: Array<String> = ContentResolverCompat.getProjection(ContentResolverCompat.Type.VIDEO)
        val selection = "${MediaStore.Video.VideoColumns.SIZE}>=?"
        val selectionArgs: Array<String> = arrayOf("102400")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bundle = Bundle()

            // Selection
            bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)

            // Limit & offset
            bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, DEFAULT_LOCAL_LOAD_LIMIT)
            bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, 0)

            // Sort
            bundle.putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Video.VideoColumns.DATE_ADDED)
            )
            bundle.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )

            context.contentResolver
                ?.query(uri, projection, bundle, null)
                ?.use { cursor ->
                    return@withContext cursor.mapTo<Video>()
                }
        } else {
            val sortOrder = "${MediaStore.Video.VideoColumns.DATE_ADDED} DESC LIMIT $DEFAULT_LOCAL_LOAD_LIMIT"

            context.contentResolver
                ?.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    return@withContext cursor.mapTo<Video>()
                }
        }

        return@withContext null
    }

    suspend fun loadLocalMediaImagesAndVideos(): List<Media>? = withContext(Dispatchers.IO) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection: Array<String> = ContentResolverCompat.getProjection(ContentResolverCompat.Type.FILE)

        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?) AND ${MediaStore.Files.FileColumns.SIZE}>=?"
        val selectionArgs: Array<String> = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "102400"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bundle = Bundle()

            // Selection
            bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)

            // Limit & offset
            bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, DEFAULT_LOCAL_LOAD_LIMIT)
            bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, 0)

            // Sort
            bundle.putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
            )
            bundle.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )

            context.contentResolver
                ?.query(uri, projection, bundle, null)
                ?.use { cursor ->
                    return@withContext cursor.mapTo<Media>()
                }
        } else {
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC LIMIT $DEFAULT_LOCAL_LOAD_LIMIT"

            context.contentResolver
                ?.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    return@withContext cursor.mapTo<Media>()
                }
        }

        return@withContext null
    }

    suspend fun loadLocalMediaAudios(): List<Audio>? = withContext(Dispatchers.IO) {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection: Array<String> = ContentResolverCompat.getProjection(ContentResolverCompat.Type.AUDIO)
        val selection = "${MediaStore.Audio.AudioColumns.SIZE}>=?"
        val selectionArgs: Array<String> = arrayOf("102400")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bundle = Bundle()

            // Selection
            bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)

            // Limit & offset
            bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, DEFAULT_LOCAL_LOAD_LIMIT)
            bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, 0)

            // Sort
            bundle.putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Audio.AudioColumns.DATE_ADDED)
            )
            bundle.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )

            context.contentResolver
                ?.query(uri, projection, bundle, null)
                ?.use { cursor ->
                    return@withContext cursor.mapTo<Audio>()
                }
        } else {
            val sortOrder = "${MediaStore.Audio.AudioColumns.DATE_ADDED} DESC LIMIT $DEFAULT_LOCAL_LOAD_LIMIT"

            context.contentResolver
                ?.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    return@withContext cursor.mapTo<Audio>()
                }
        }

        return@withContext null
    }

    private suspend inline fun <reified T> Cursor.mapTo(): List<T> = withContext(Dispatchers.IO) {
        val list = mutableListOf<T>()
        if (moveToFirst()) {
            while (moveToNext()) {
                val item = when (T::class.java) {
                    Image::class.java -> this@mapTo.readImage(context)
                    Video::class.java -> this@mapTo.readVideo(context)
                    Media::class.java -> this@mapTo.readFile(context)
                    Audio::class.java -> this@mapTo.readAudio()
                    Document::class.java -> null
                    else -> null
                }
                if (item is T) {
                    list.add(item)
                }
            }
        }
        return@withContext list
    }

    suspend fun loadSelectedLocalMediaImages(uris: List<Uri>): List<Image> = withContext(Dispatchers.IO) {
        return@withContext uris.mapNotNull { uri ->
            loadSelectedLocalMediaImage(uri)
        }
    }

    suspend fun loadSelectedLocalMediaImage(uri: Uri): Image? = withContext(Dispatchers.IO) {
        try {
            check(uri.scheme == ContentResolver.SCHEME_CONTENT) { "Unsupported uri.scheme!" }

            var image: Image? = null

            // Create new file from uri (content://...)
            val filename = createImageFilename()

            // Retrieve local info from MediaStore
            val projection = ContentResolverCompat.getOpenableContentProjection()

            context.contentResolver
                ?.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    image = cursor.readOpenableImage(uri, filename.removeSuffix(".") + ".jpg", "jpg")
                }

            Logger.d(TAG, "Scanned by MediaStore: $image")

            if (image == null) return@withContext null

            val file = (uri.transformLocalContentToFile(Environment.DIRECTORY_PICTURES, requireNotNull(image).title) ?: return@withContext null)

            Logger.d(TAG, "Created local file: $file")

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
                    runCatching {
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
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun loadSelectedLocalMediaVideos(uris: List<Uri>): List<Video> = withContext(Dispatchers.IO) {
        return@withContext uris.mapNotNull { uri ->
            loadSelectedLocalMediaVideo(uri)
        }
    }

    suspend fun loadSelectedLocalMediaVideo(uri: Uri): Video? = withContext(Dispatchers.IO) {
        try {
            check(uri.scheme == ContentResolver.SCHEME_CONTENT) { "Unsupported uri.scheme!" }

            var video: Video? = null

            // Create new file from uri (content://...)
            val filename = createVideoFilename()

            // Retrieve local info from MediaStore
            val projection = ContentResolverCompat.getOpenableContentProjection()

            context.contentResolver
                ?.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    video = cursor.readOpenableVideo(uri, filename.removeSuffix(".") + ".mp4", "mp4")
                }

            Logger.d(TAG, "Scanned by MediaStore: $video")

            if (video == null) return@withContext null

            val file = (uri.transformLocalContentToFile(Environment.DIRECTORY_MOVIES, requireNotNull(video).title) ?: return@withContext null)

            Logger.d(TAG, "Created local file: $file")

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
            val metadata = video?.uri.retrieveVideoMetadata(context)
            if (metadata != null) {
                video = video?.copy(
                    width = metadata.width ?: 0,
                    height = metadata.height ?: 0,
                    duration = metadata.duration,
                    thumbnail = metadata.thumbnail
                )
            }

            Logger.d(TAG, "video: $video")

            return@withContext video
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun loadSelectedLocalMediaAudios(uris: List<Uri>): List<Audio> = withContext(Dispatchers.IO) {
        return@withContext uris.mapNotNull { uri ->
            loadSelectedLocalMediaAudio(uri)
        }
    }

    suspend fun loadSelectedLocalMediaAudio(uri: Uri): Audio? = withContext(Dispatchers.IO) {
        try {
            check(uri.scheme == ContentResolver.SCHEME_CONTENT) { "Unsupported uri.scheme!" }

            var audio: Audio? = null

            // Create new file from uri (content://...)
            val filename = createAudioFilename()

            // Retrieve local info from MediaStore
            val projection = ContentResolverCompat.getOpenableContentProjection()

            context.contentResolver
                ?.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    audio = cursor.readOpenableAudio(uri, filename.removeSuffix(".") + ".mp3", "mp3")
                }

            Logger.d(TAG, "Scanned by MediaStore: $audio")

            if (audio == null) return@withContext null

            val file = (uri.transformLocalContentToFile(Environment.DIRECTORY_MUSIC, requireNotNull(audio).title) ?: return@withContext null)

            Logger.d(TAG, "Created local file: $file")

            // Set file path
            audio = audio?.copy(path = file.absolutePath)

            // Set MimeType
            audio = audio?.copy(mimeType = uri.gainMimeType(null))

            // Set extension
            val extension = file.getExtension(mimeType = audio?.mimeType)
            if (!extension.isNullOrBlank()) {
                audio = audio?.copy(extension = extension)
            }

            // Retrieve additional metadata from uri
            val metadata = audio?.uri.retrieveAudioMetadata(context)
            if (metadata != null) {
                audio = audio?.copy(
                    duration = metadata.duration
                )
            }

            Logger.d(TAG, "audio: $audio")

            return@withContext audio
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun loadSelectedLocalMediaImagesAndVideos(uris: List<Uri>): List<Media> = withContext(Dispatchers.IO) {
        return@withContext uris.mapNotNull { uri ->
            loadSelectedLocalMediaImageOrVideo(uri)
        }
    }

    suspend fun loadSelectedLocalMediaImageOrVideo(uri: Uri): Media? = withContext(Dispatchers.IO) {
        val mimeType = uri.getMimeType(context)?.toLowerCase(Locale.ROOT)
        Logger.d(TAG, "loadSelectedLocalMediaImageOrVideo() -> mimeType: $mimeType")
        if (mimeType.isNullOrBlank()) {
            return@withContext null
        }
        return@withContext when {
            mimeType.startsWith("image") ->
                loadSelectedLocalMediaImage(uri)
            mimeType.startsWith("video") ->
                loadSelectedLocalMediaVideo(uri)
            else ->
                null
        }
    }

    suspend fun loadSelectedLocalDocument(uri: Uri): Document? = withContext(Dispatchers.IO) {
        try {
            check(uri.scheme == ContentResolver.SCHEME_CONTENT) { "Unsupported uri.scheme!" }

            var document: Document? = null

            // Create new file from uri (content://...)
            val filename = createDocumentFilename()

            // Retrieve local info from MediaStore
            val projection = ContentResolverCompat.getOpenableContentProjection()

            context.contentResolver
                ?.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    document = cursor.readOpenableDocument(uri, filename)
                }

            if (document == null) return@withContext null

            val file = (uri.transformLocalContentToFile(Environment.DIRECTORY_DOCUMENTS, requireNotNull(document).title) ?: return@withContext null)

            Logger.d(TAG, "Created local file: $file")

            // Set file path
            document = document?.copy(path = file.absolutePath)

            // Set MimeType
            document = document?.copy(mimeType = uri.gainMimeType(null))

            // Set extension
            val extension = file.getExtension(mimeType = document?.mimeType)
            if (!extension.isNullOrBlank()) {
                document = document?.copy(extension = extension)
            }

            // Retrieve additional metadata from bitmap
            runCatching {
                context.contentResolver
                    ?.openFileDescriptor(uri, "r")
                    ?.use {
                        Logger.d(TAG, "openFileDescriptor() -> $it")
                    }
            }

            return@withContext document
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun loadSelectedLocalDocuments(uris: List<Uri>): List<Document> = withContext(Dispatchers.IO) {
        return@withContext uris.mapNotNull { uri ->
            loadSelectedLocalDocument(uri)
        }
    }

    private suspend fun Uri.transformLocalContentToFile(
        type: String,
        filename: String
    ): File? = withContext(Dispatchers.IO) {
        Logger.d(TAG, "transformLocalContentToFile() -> filename: $filename")

        // If file could not be created, then there is no need to continue code flow
        var file: File? = null

        runCatching {
            file = File(context.getExternalFilesDir(type), filename)
//            file = File(context.cacheDir, filename)

            Logger.d(TAG, "Create local file [$file] with name $filename")

            if (file?.exists() == false) {
                file?.createNewFile()
            }
            val outputStream = FileOutputStream(file)
            val inputStream = context.contentResolver?.openInputStream(this@transformLocalContentToFile)

            inputStream?.use {
                copy(inputStream, outputStream)
            }

            outputStream.flush()
        }
            .onSuccess { return@withContext file }
            .onFailure { return@withContext null }

        return@withContext null
    }

    @Throws(IOException::class)
    private suspend fun copy(source: InputStream, target: OutputStream) {
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(8192)
            var length: Int
            runCatching {
                while (source.read(buffer).also { length = it } > 0) {
                    target.write(buffer, 0, length)
                }
            }
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

    private fun createAudioFilename(): String {
        val timestamp = System.currentTimeMillis()
        return "AUDIO_${timestamp}"
    }

    private fun createDocumentFilename(): String {
        val timestamp = System.currentTimeMillis()
        return "DOC_${timestamp}"
    }

    private fun Uri.gainMimeType(default: String?): String? {
        return context.contentResolver?.getType(this) ?: default
    }

    suspend fun decodeImageFile(image: Image): Image = withContext(Dispatchers.IO) {
        val bitmap: Bitmap? = BitmapFactory.decodeFile(image.path)
        Logger.d(TAG, "takenImage: $bitmap, ${bitmap?.width} x ${bitmap?.height}")
        val size = if (!image.path.isNullOrBlank()) {
            File(image.path).length()
        } else {
            bitmap?.allocationByteCount?.toLong()
        } ?: 0
        val width = bitmap?.width ?: 0
        val height = bitmap?.height ?: 0
        val bytes = ByteArrayOutputStream()
        var thumbnail = bitmap
        if (bitmap?.compress(Bitmap.CompressFormat.JPEG, 60, bytes) == true) {
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

    suspend fun decodeVideoFile(video: Video): Video = withContext(Dispatchers.IO) {
        val metadata = video.uri.retrieveVideoMetadata(context)
        if (metadata != null) {
            return@withContext video.copy(
                size = if (!video.path.isNullOrBlank()) File(video.path).length() else 0,
                width = metadata.width ?: 0,
                height = metadata.height ?: 0,
                duration = metadata.duration,
                thumbnail = metadata.thumbnail
            )
        }
        return@withContext video
    }

    suspend fun deleteFile(image: Image): Boolean = withContext(Dispatchers.IO) {
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