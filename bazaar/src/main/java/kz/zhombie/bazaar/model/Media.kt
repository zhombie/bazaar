package kz.zhombie.bazaar.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * [mimeType] - The MIME type of the [Media]
 * [width] - The width of the [Media], in pixels.
 * [height] - The height of the [Media], in pixels.
 */
open class Media constructor(
    override val id: Long,
    override val uri: Uri,
    override val title: String,
    override val displayName: String,
    override val size: Long,
    override val dateAdded: Long,
    override val dateModified: Long,
    override val dateCreated: Long?,
    override val thumbnail: Bitmap? = null,

    open val mimeType: String,
    open val width: Int,
    open val height: Int
) : Entity(
    id = id,
    uri = uri,
    title = title,
    displayName = displayName,
    size = size,
    dateAdded = dateAdded,
    dateModified = dateModified,
    dateCreated = dateCreated,
    thumbnail = thumbnail
)