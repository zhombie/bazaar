package kz.zhombie.bazaar.api.model

internal fun Image.complete(image: Image?): Image? {
    if (image == null) return null
    return Image(
        id = id,
        uri = uri,
        path = path ?: image.path,
        title = if (title.isNotBlank()) title else image.title,
        displayName = if (displayName.isNotBlank()) displayName else image.displayName,
        mimeType = mimeType ?: image.mimeType,
        extension = extension ?: image.extension,
        size = image.size,
        dateAdded = dateAdded,
        dateModified = dateModified,
        dateCreated = dateCreated,
        thumbnail = thumbnail ?: image.thumbnail,
        folderId = folderId ?: image.folderId,
        folderDisplayName = folderDisplayName ?: image.folderDisplayName,
        width = if (width > 0) width else image.width,
        height = if (height > 0) height else image.height,
        source = source ?: image.source
    )
}