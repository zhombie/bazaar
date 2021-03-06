package kz.zhombie.bazaar.api.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * [duration] - The duration time of the [Video]
 * [cover] - The cover image of the [Video] (like thumbnail)
 */
data class Video constructor(
    override val id: Long,
    override val uri: Uri,
    override val title: String,
    override val displayName: String,
    override val size: Long,
    override val dateAdded: Long,
    override val dateModified: Long,
    override val dateCreated: Long?,
    override val mimeType: String,
    override val width: Int,
    override val height: Int,
    override val thumbnail: Bitmap? = null,

    val duration: Long?,
    val cover: Bitmap?,
) : Media(
    id = id,
    uri = uri,
    title = title,
    displayName = displayName,
    dateAdded = dateAdded,
    dateModified = dateModified,
    dateCreated = dateCreated,
    size = size,
    mimeType = mimeType,
    width = width,
    height = height,
    thumbnail = thumbnail
)