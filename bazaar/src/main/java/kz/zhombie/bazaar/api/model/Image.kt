package kz.zhombie.bazaar.api.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * [source] - The original [Bitmap] of the [Image] (its size is [width]x[height])
 */
data class Image constructor(
    override val id: Long,
    override val uri: Uri,
    override val path: String?,
    override val title: String,
    override val displayName: String,
    override val mimeType: String?,
    override val extension: String?,
    override val size: Long,
    override val dateAdded: Long,
    override val dateModified: Long,
    override val dateCreated: Long?,
    override val thumbnail: Bitmap? = null,
    override val folderId: Long? = null,
    override val folderDisplayName: String? = null,
    override val width: Int,
    override val height: Int,

    val source: Bitmap? = null
) : Media(
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
    folderDisplayName = folderDisplayName,
    width = width,
    height = height
)