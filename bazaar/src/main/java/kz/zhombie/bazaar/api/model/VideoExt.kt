package kz.zhombie.bazaar.api.model

import kz.garage.multimedia.store.model.Video

internal fun Video.complete(video: Video?): Video? {
    if (video == null) return null
    return Video(
        id = id,
        uri = uri,
        title = if (title.isNullOrBlank()) video.title else title,
        displayName = if (displayName.isNullOrBlank()) video.displayName else displayName,
        folder = folder ?: video.folder,
        history = history ?: video.history,
        duration = if (duration == null || duration == -1L) video.duration else duration,
        resolution = resolution ?: video.resolution,
        properties = properties ?: video.properties,
        publicFile = publicFile ?: video.publicFile,
        remoteFile = remoteFile ?: video.remoteFile
    )
}