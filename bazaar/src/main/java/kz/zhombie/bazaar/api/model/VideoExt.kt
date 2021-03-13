package kz.zhombie.bazaar.api.model

internal fun Video.complete(video: Video?): Video? {
    if (video == null) return null
    return Video(
        id = id,
        uri = uri,
        path = path ?: video.path,
        title = if (title.isNotBlank()) title else video.title,
        displayName = if (displayName.isNotBlank()) displayName else video.displayName,
        mimeType = mimeType ?: video.mimeType,
        extension = extension ?: video.extension,
        size = video.size,
        dateAdded = dateAdded,
        dateModified = dateModified,
        dateCreated = dateCreated,
        thumbnail = thumbnail ?: video.thumbnail,
        folderId = folderId ?: video.folderId,
        folderDisplayName = folderDisplayName ?: video.folderDisplayName,
        width = if (width > 0) width else video.width,
        height = if (height > 0) height else video.height,
        duration = duration ?: video.duration
    )
}