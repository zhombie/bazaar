package kz.zhombie.bazaar.api.model

internal fun Audio.complete(audio: Audio?): Audio? {
    if (audio == null) return null
    return Audio(
        id = id,
        uri = uri,
        path = path ?: audio.path,
        title = if (title.isNotBlank()) title else audio.title,
        displayName = if (displayName.isNotBlank()) displayName else audio.displayName,
        mimeType = mimeType ?: audio.mimeType,
        extension = extension ?: audio.extension,
        size = audio.size,
        dateAdded = dateAdded,
        dateModified = dateModified,
        dateCreated = dateCreated,
        thumbnail = thumbnail ?: audio.thumbnail,
        folderId = folderId ?: audio.folderId,
        folderDisplayName = folderDisplayName ?: audio.folderDisplayName,
        duration = duration ?: audio.duration
    )
}