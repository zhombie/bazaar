package kz.zhombie.bazaar.model

import android.net.Uri

data class Image constructor(
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
    override val height: Int
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
    height = height
)