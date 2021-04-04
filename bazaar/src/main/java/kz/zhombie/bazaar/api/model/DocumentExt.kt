package kz.zhombie.bazaar.api.model

internal fun Document.complete(document: Document?): Document? {
    if (document == null) return null
    return Document(
        id = id,
        uri = uri,
        path = path ?: document.path,
        title = if (title.isNotBlank()) title else document.title,
        displayName = if (displayName.isNotBlank()) displayName else document.displayName,
        mimeType = mimeType ?: document.mimeType,
        extension = extension ?: document.extension,
        size = document.size,
        dateAdded = dateAdded,
        dateModified = dateModified,
        dateCreated = dateCreated,
        thumbnail = thumbnail ?: document.thumbnail,
        folderId = folderId ?: document.folderId,
        folderDisplayName = folderDisplayName ?: document.folderDisplayName,
    )
}