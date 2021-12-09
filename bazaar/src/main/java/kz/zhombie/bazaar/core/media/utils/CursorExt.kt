package kz.zhombie.bazaar.core.media.utils

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.garage.multimedia.store.model.*
import java.util.concurrent.TimeUnit

internal suspend fun Cursor.readImage(): Image? = withContext(Dispatchers.IO) {
    val id = getLongOrNull(getColumnIndex(MediaStore.Images.ImageColumns._ID))
        ?: return@withContext null

    val externalContentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
    val title = getStringOrNull(getColumnIndex(MediaStore.Images.ImageColumns.TITLE))
    val displayName = getStringOrNull(getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME))
    val size = getLongOrNull(getColumnIndex(MediaStore.Images.ImageColumns.SIZE))

    var dateAdded = getLongOrNull(getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED))
    dateAdded = dateAdded?.let { TimeUnit.SECONDS.toMillis(it) }

    var dateModified = getLongOrNull(getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED))
    dateModified = dateModified?.let { TimeUnit.SECONDS.toMillis(it) }

    val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getLongOrNull(getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN))
    } else {
        null
    }

    val history = if (dateAdded == null && dateModified == null && dateTaken == null) {
        null
    } else {
        Content.History(
            addedAt = dateAdded,
            modifiedAt = dateModified,
            createdAt = dateTaken,
        )
    }

    val mimeType = getStringOrNull(getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE))

    val width = getIntOrNull(getColumnIndex(MediaStore.Images.ImageColumns.WIDTH))
    val height = getIntOrNull(getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT))
    val resolution = if (width != null && height != null) {
        Resolution(width, height)
    } else {
        null
    }

    val bucketId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getLongOrNull(getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID))
    } else {
        null
    }
    val bucketDisplayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getStringOrNull(getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))
    } else {
        null
    }

    val folder = if (bucketId != null && bucketDisplayName != null) {
        Folder(bucketId, bucketDisplayName)
    } else {
        null
    }

    Image(
        id = id,
        uri = externalContentUri,
        title = title,
        displayName = displayName,
        folder = folder,
        history = history,
        resolution = resolution,
        properties = Content.Properties(
            size = size ?: Content.Properties.UNDEFINED_SIZE,
            mimeType = mimeType,
        ),
        localFile = null
    )
}


internal suspend fun Cursor.readVideo(): Video? = withContext(Dispatchers.IO) {
    val id = getLongOrNull(getColumnIndex(MediaStore.Video.VideoColumns._ID)) 
        ?: return@withContext null
    
    val externalContentUri =
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
    val title = getStringOrNull(getColumnIndex(MediaStore.Video.VideoColumns.TITLE))
    val displayName = getStringOrNull(getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME))
    val size = getLongOrNull(getColumnIndex(MediaStore.Video.VideoColumns.SIZE))

    var dateAdded = getLongOrNull(getColumnIndex(MediaStore.Video.VideoColumns.DATE_ADDED))
    dateAdded = dateAdded?.let { TimeUnit.SECONDS.toMillis(it) }

    var dateModified = getLongOrNull(getColumnIndex(MediaStore.Video.VideoColumns.DATE_MODIFIED))
    dateModified = dateModified?.let { TimeUnit.SECONDS.toMillis(it) }

    val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getLongOrNull(getColumnIndex(MediaStore.Video.VideoColumns.DATE_TAKEN))
    } else {
        null
    }

    val history = if (dateAdded == null && dateModified == null && dateTaken == null) {
        null
    } else {
        Content.History(
            addedAt = dateAdded,
            modifiedAt = dateModified,
            createdAt = dateTaken,
        )
    }

    val mimeType = getStringOrNull(getColumnIndex(MediaStore.Video.VideoColumns.MIME_TYPE))

    val width = getIntOrNull(getColumnIndex(MediaStore.Video.VideoColumns.WIDTH))
    val height = getIntOrNull(getColumnIndex(MediaStore.Video.VideoColumns.HEIGHT))
    val resolution = if (width != null && height != null) {
        Resolution(width, height)
    } else {
        null
    }

    val bucketId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getLongOrNull(getColumnIndex(MediaStore.Video.VideoColumns.BUCKET_ID))
    } else {
        null
    }

    val bucketDisplayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getStringOrNull(getColumnIndex(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME))
    } else {
        null
    }

    val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getLongOrNull(getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
    } else {
        null
    }

    val folder = if (bucketId != null && bucketDisplayName != null) {
        Folder(bucketId, bucketDisplayName)
    } else {
        null
    }

    Video(
        id = id,
        uri = externalContentUri,
        title = title,
        displayName = displayName,
        folder = folder,
        history = history,
        duration = duration ?: Media.Playable.UNDEFINED_DURATION,
        resolution = resolution,
        properties = Content.Properties(
            size = size ?: Content.Properties.UNDEFINED_SIZE,
            mimeType = mimeType,
        ),
        localFile = null
    )
}


internal suspend fun Cursor.readFile(): Media? {
    return when (getIntOrNull(getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))) {
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> readImage()
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> readVideo()
        else -> null
    }
}


internal suspend fun Cursor.readAudio(): Audio? = withContext(Dispatchers.IO) {
    val id = getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns._ID))
        ?: return@withContext null

    val externalContentUri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val title = getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.TITLE))
    val displayName = getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME))
    val size = getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.SIZE))

    var dateAdded = getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED))
    dateAdded = dateAdded?.let { TimeUnit.SECONDS.toMillis(it) }

    var dateModified = getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.DATE_MODIFIED))
    dateModified = dateModified?.let { TimeUnit.SECONDS.toMillis(it) }

    val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.DATE_TAKEN))
    } else {
        -1L
    }

    val mimeType = getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE))

    val bucketId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.BUCKET_ID))
    } else {
        null
    }
    val bucketDisplayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.BUCKET_DISPLAY_NAME))
    } else {
        null
    }

    val albumId = getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))
    val albumTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM))
    } else {
        null
    }
    val albumArtist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ARTIST))
    } else {
        null
    }

    val album = if (albumId == null) {
        null
    } else {
        Audio.Album(
            id = albumId,
            title = albumTitle,
            artist = albumArtist
        )
    }

    val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getLongOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.DURATION))
    } else {
        null
    }

    Audio(
        id = id,
        uri = externalContentUri,
        title = title,
        displayName = displayName,
        folder = Folder(
            id = bucketId ?: Content.generateId(),
            displayName = bucketDisplayName
        ),
        history = Content.History(
            addedAt = dateAdded,
            modifiedAt = dateModified,
            createdAt = dateTaken,
        ),
        duration = duration ?: Media.Playable.UNDEFINED_DURATION,
        properties = Content.Properties(
            size = size ?: Content.Properties.UNDEFINED_SIZE,
            mimeType = mimeType,
        ),
        album = album,
        localFile = null
    )
}


internal suspend fun Cursor.readOpenableImage(uri: Uri): Image? = withContext(Dispatchers.IO) {
    val displayName: String?
    val size: Long?
    if (moveToFirst()) {
        displayName = getStringOrNull(getColumnIndex(OpenableColumns.DISPLAY_NAME))
        size = getLongOrNull(getColumnIndex(OpenableColumns.SIZE))
    } else {
        return@withContext null
    }
    Image(
        id = Content.generateId(),
        uri = uri,
        title = null,
        displayName = displayName,
        folder = null,
        history = null,
        resolution = null,
        properties = Content.Properties(size = size ?: Content.Properties.UNDEFINED_SIZE),
        localFile = null
    )
}


internal suspend fun Cursor.readOpenableVideo(uri: Uri): Video? = withContext(Dispatchers.IO) {
    val displayName: String?
    val size: Long?
    if (moveToFirst()) {
        displayName = getStringOrNull(getColumnIndex(OpenableColumns.DISPLAY_NAME))
        size = getLongOrNull(getColumnIndex(OpenableColumns.SIZE))
    } else {
        return@withContext null
    }
    Video(
        id = Content.generateId(),
        uri = uri,
        title = null,
        displayName = displayName,
        folder = null,
        history = null,
        duration = Media.Playable.UNDEFINED_DURATION,
        resolution = null,
        properties = Content.Properties(
            size = size ?: Content.Properties.UNDEFINED_SIZE,
        ),
        localFile = null
    )
}


internal suspend fun Cursor.readOpenableAudio(uri: Uri): Audio? = withContext(Dispatchers.IO) {
    val displayName: String?
    val size: Long?
    if (moveToFirst()) {
        displayName = getStringOrNull(getColumnIndex(OpenableColumns.DISPLAY_NAME))
        size = getLongOrNull(getColumnIndex(OpenableColumns.SIZE))
    } else {
        return@withContext null
    }
    Audio(
        id = Content.generateId(),
        uri = uri,
        title = null,
        displayName = displayName,
        folder = null,
        history = null,
        duration = Media.Playable.UNDEFINED_DURATION,
        properties = Content.Properties(
            size = size ?: Content.Properties.UNDEFINED_SIZE,
        ),
        album = null,
        localFile = null
    )
}


internal suspend fun Cursor.readOpenableDocument(uri: Uri): Document? = withContext(Dispatchers.IO) {
    val displayName: String?
    val size: Long?
    if (moveToFirst()) {
        displayName = getStringOrNull(getColumnIndex(OpenableColumns.DISPLAY_NAME))
        size = getLongOrNull(getColumnIndex(OpenableColumns.SIZE))
    } else {
        return@withContext null
    }
    Document(
        id = Content.generateId(),
        uri = uri,
        title = null,
        displayName = displayName,
        folder = null,
        history = null,
        properties = Content.Properties(
            size = size ?: Content.Properties.UNDEFINED_SIZE,
        ),
        localFile = null
    )
}
