package kz.zhombie.bazaar.core.media.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.api.model.*
import kz.zhombie.bazaar.utils.getExtension
import java.util.concurrent.TimeUnit

internal suspend fun Cursor.readImage(context: Context): Image? = withContext(Dispatchers.IO) {
    return@withContext try {
        val id = getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID))
        val externalContentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        val title = getString(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.TITLE))
        val displayName = getString(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME))
        val size = getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SIZE))

        var dateAdded = getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_ADDED))
        dateAdded = TimeUnit.SECONDS.toMillis(dateAdded)

        var dateModified = getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_MODIFIED))
        dateModified = TimeUnit.SECONDS.toMillis(dateModified)

        val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN))
        } else {
            -1L
        }

        val mimeType = getString(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MIME_TYPE))
        val width = getInt(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.WIDTH))
        val height = getInt(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.HEIGHT))

        var bucketId: Long? = null
        var bucketDisplayName: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bucketId = getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID))
            bucketDisplayName = getString(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))
        }

        val thumbnail: Bitmap? = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val thumbnailSize = Size(125, 125)
                context.contentResolver.loadThumbnail(externalContentUri, thumbnailSize, null)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    context.contentResolver,
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    BitmapFactory.Options()
                )
            }
        }.getOrNull()

        Image(
            id = id,
            uri = externalContentUri,
            path = null,
            title = title,
            displayName = displayName,
            mimeType = mimeType,
            extension = getExtension(filename = displayName, mimeType = mimeType),
            size = size,
            dateAdded = dateAdded,
            dateModified = dateModified,
            dateCreated = dateTaken,
            thumbnail = thumbnail,
            folderId = bucketId,
            folderDisplayName = bucketDisplayName,
            width = width,
            height = height,
            source = null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


internal suspend fun Cursor.readVideo(context: Context): Video? = withContext(Dispatchers.IO) {
    return@withContext try {
        val id = getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns._ID))
        val externalContentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        val title = getString(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.TITLE))
        val displayName = getString(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DISPLAY_NAME))
        val size = getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.SIZE))

        var dateAdded = getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATE_ADDED))
        dateAdded = TimeUnit.SECONDS.toMillis(dateAdded)

        var dateModified = getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATE_MODIFIED))
        dateModified = TimeUnit.SECONDS.toMillis(dateModified)

        val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATE_TAKEN))
        } else {
            -1L
        }

        val mimeType = getString(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.MIME_TYPE))
        val width = getInt(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.WIDTH))
        val height = getInt(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.HEIGHT))

        var bucketId: Long? = null
        var bucketDisplayName: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bucketId = getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.BUCKET_ID))
            bucketDisplayName = getString(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME))
        }

        val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION))
        } else {
            null
        }

        val thumbnail: Bitmap? = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val thumbnailSize = Size(125, 125)
                context.contentResolver.loadThumbnail(externalContentUri, thumbnailSize, null)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver,
                    id,
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    BitmapFactory.Options()
                )
            }
        }.getOrNull()

        Video(
            id = id,
            uri = externalContentUri,
            path = null,
            title = title,
            displayName = displayName,
            mimeType = mimeType,
            extension = getExtension(filename = displayName, mimeType = mimeType),
            size = size,
            dateAdded = dateAdded,
            dateModified = dateModified,
            dateCreated = dateTaken,
            thumbnail = thumbnail,
            folderId = bucketId,
            folderDisplayName = bucketDisplayName,
            width = width,
            height = height,
            duration = duration
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


internal suspend fun Cursor.readFile(context: Context): Media? {
    return when (getInt(getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))) {
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> readImage(context)
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> readVideo(context)
        else -> null
    }
}


internal suspend fun Cursor.readAudio(): Audio? = withContext(Dispatchers.IO) {
    return@withContext try {
        val id = getLong(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID))
        val externalContentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        val title = getString(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE))
        val displayName = getString(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME))
        val size = getLong(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE))

        var dateAdded = getLong(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED))
        dateAdded = TimeUnit.SECONDS.toMillis(dateAdded)

        var dateModified = getLong(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED))
        dateModified = TimeUnit.SECONDS.toMillis(dateModified)

        val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLong(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_TAKEN))
        } else {
            -1L
        }

        val mimeType = getString(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE))

        var bucketId: Long? = null
        var bucketDisplayName: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bucketId = getLong(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.BUCKET_ID))
            bucketDisplayName = getString(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.BUCKET_DISPLAY_NAME))
        }

        val albumId = getLongOrNull(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID))
        val albumTitle = getStringOrNull(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM))
        val albumArtist = getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ARTIST))

        val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLong(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION))
        } else {
            null
        }

        Audio(
            id = id,
            uri = externalContentUri,
            path = null,
            title = title,
            displayName = displayName,
            mimeType = mimeType,
            extension = getExtension(filename = displayName, mimeType = mimeType),
            size = size,
            dateAdded = dateAdded,
            dateModified = dateModified,
            dateCreated = dateTaken,
            thumbnail = null,
            folderId = bucketId,
            folderDisplayName = bucketDisplayName,
            album = Audio.Album(id = albumId, title = albumTitle, artist = albumArtist),
            duration = duration
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


internal fun Cursor.readOpenableImage(uri: Uri, filename: String, extension: String? = null): Image? {
    return try {
        var displayName = filename
        var size: Long = 0
        if (moveToFirst()) {
            displayName = getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))
            size = getLong(getColumnIndex(OpenableColumns.SIZE))
        }
        Image(
            id = -1,
            uri = uri,
            path = null,
            title = displayName,
            displayName = displayName,
            mimeType = null,
            extension = extension,
            size = size,
            dateAdded = 0,
            dateModified = System.currentTimeMillis(),
            dateCreated = 0,
            thumbnail = null,
            folderId = null,
            folderDisplayName = null,
            width = 0,
            height = 0,
            source = null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


internal fun Cursor.readOpenableVideo(uri: Uri, filename: String, extension: String? = null): Video? {
    return try {
        var displayName = filename
        var size: Long = 0
        if (moveToFirst()) {
            displayName = getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))
            size = getLong(getColumnIndex(OpenableColumns.SIZE))
        }
        Video(
            id = -1,
            uri = uri,
            path = null,
            title = displayName,
            displayName = displayName,
            mimeType = null,
            extension = extension,
            size = size,
            dateAdded = 0,
            dateModified = System.currentTimeMillis(),
            dateCreated = 0,
            thumbnail = null,
            folderId = null,
            folderDisplayName = null,
            width = 0,
            height = 0,
            duration = null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


internal fun Cursor.readOpenableAudio(uri: Uri, filename: String, extension: String? = null): Audio? {
    return try {
        var displayName = filename
        var size: Long = 0
        if (moveToFirst()) {
            displayName = getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))
            size = getLong(getColumnIndex(OpenableColumns.SIZE))
        }
        Audio(
            id = -1,
            uri = uri,
            path = null,
            title = displayName,
            displayName = displayName,
            mimeType = null,
            extension = extension,
            size = size,
            dateAdded = 0,
            dateModified = System.currentTimeMillis(),
            dateCreated = 0,
            thumbnail = null,
            folderId = null,
            folderDisplayName = null,
            duration = null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


internal fun Cursor.readOpenableDocument(uri: Uri, filename: String, extension: String? = null): Document? {
    return try {
        var displayName = filename
        var size: Long = 0
        if (moveToFirst()) {
            displayName = getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))
            size = getLong(getColumnIndex(OpenableColumns.SIZE))
        }
        Document(
            id = -1,
            uri = uri,
            path = null,
            title = displayName,
            displayName = displayName,
            mimeType = null,
            extension = extension,
            size = size,
            dateAdded = 0,
            dateModified = System.currentTimeMillis(),
            dateCreated = 0,
            thumbnail = null,
            folderId = null,
            folderDisplayName = null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

