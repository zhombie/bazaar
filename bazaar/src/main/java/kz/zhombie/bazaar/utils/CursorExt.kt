package kz.zhombie.bazaar.utils

import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import kz.zhombie.bazaar.model.Image
import kz.zhombie.bazaar.model.Video
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
            height = height
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
            duration = duration,
            cover = null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}