package kz.zhombie.bazaar.core.media.utils

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import kz.zhombie.bazaar.api.model.Audio
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import java.io.File
import java.util.concurrent.TimeUnit

internal fun Cursor.readImage(): Image? {
    return try {
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
            thumbnail = null,
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


internal fun Cursor.readVideo(): Video? {
    return try {
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
            thumbnail = null,
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


internal fun Cursor.readFile(): Media? {
    return when (getInt(getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))) {
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> readImage()
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> readVideo()
        else -> null
    }
}


internal fun Cursor.readAudio(): Audio? {
    return try {
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
            duration = duration
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


internal fun Cursor.readOpenableImage(uri: Uri, file: File): Image? {
    return try {
        var displayName = file.name
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
            extension = file.getExtension(),
            size = size,
            dateAdded = 0,
            dateModified = file.lastModified(),
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


internal fun Cursor.readOpenableVideo(uri: Uri, file: File): Video? {
    return try {
        var displayName = file.name
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
            extension = null,
            size = size,
            dateAdded = 0,
            dateModified = file.lastModified(),
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


internal fun Cursor.readOpenableAudio(uri: Uri, file: File): Audio? {
    return try {
        var displayName = file.name
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
            extension = null,
            size = size,
            dateAdded = 0,
            dateModified = file.lastModified(),
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