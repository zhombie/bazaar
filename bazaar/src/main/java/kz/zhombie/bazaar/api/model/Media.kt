package kz.zhombie.bazaar.api.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * [width] - The width of the [Media], in pixels.
 * [height] - The height of the [Media], in pixels.
 */
open class Media constructor(
    override val id: Long,
    override val uri: Uri,
    override val path: String?,
    override val title: String,
    override val displayName: String,
    override val mimeType: String? = null,
    override val extension: String? = null,
    override val size: Long,
    override val dateAdded: Long,
    override val dateModified: Long,
    override val dateCreated: Long?,
    override val thumbnail: Bitmap? = null,
    override val folderId: Long? = null,
    override val folderDisplayName: String? = null,

    open val width: Int,
    open val height: Int
) : Entity(
    id = id,
    uri = uri,
    path = path,
    title = title,
    displayName = displayName,
    mimeType = mimeType,
    extension = extension,
    size = size,
    dateAdded = dateAdded,
    dateModified = dateModified,
    dateCreated = dateCreated,
    thumbnail = thumbnail,
    folderId = folderId,
    folderDisplayName = folderDisplayName
)