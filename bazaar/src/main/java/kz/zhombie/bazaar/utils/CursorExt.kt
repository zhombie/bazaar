package kz.zhombie.bazaar.utils

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
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
            title = title,
            displayName = displayName,
            size = size,
            dateAdded = dateAdded,
            dateModified = dateModified,
            dateCreated = dateTaken,
            mimeType = mimeType,
            width = width,
            height = height,
            thumbnail = null,
            folderId = bucketId,
            folderDisplayName = bucketDisplayName
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

        var dateAdded = getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_ADDED))
        dateAdded = TimeUnit.SECONDS.toMillis(dateAdded)

        var dateModified = getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_MODIFIED))
        dateModified = TimeUnit.SECONDS.toMillis(dateModified)

        val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN))
        } else {
            -1L
        }

        val mimeType = getString(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.MIME_TYPE))
        val width = getInt(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.WIDTH))
        val height = getInt(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.HEIGHT))

        var bucketId: Long? = null
        var bucketDisplayName: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bucketId = getLong(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID))
            bucketDisplayName = getString(getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))
        }

        val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION))
        } else {
            null
        }

        Video(
            id = id,
            uri = externalContentUri,
            title = title,
            displayName = displayName,
            size = size,
            dateAdded = dateAdded,
            dateModified = dateModified,
            dateCreated = dateTaken,
            mimeType = mimeType,
            width = width,
            height = height,
            thumbnail = null,
            folderId = bucketId,
            folderDisplayName = bucketDisplayName,
            duration = duration,
            cover = null
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
            title = displayName,
            displayName = displayName,
            size = size,
            dateAdded = 0,
            dateModified = file.lastModified(),
            dateCreated = 0,
            mimeType = "",
            width = 0,
            height = 0,
            thumbnail = null,
            folderId = null,
            folderDisplayName = null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}