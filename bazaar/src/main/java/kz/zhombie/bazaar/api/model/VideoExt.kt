package kz.zhombie.bazaar.api.model

import kz.garage.multimedia.store.model.Media
import kz.garage.multimedia.store.model.Video

internal fun Video.complete(video: Video?): Video? {
    if (video == null) return null
    return Video(
        id = id,
        uri = uri,
        title = if (!title.isNullOrBlank()) title else video.title,
        displayName = if (!displayName.isNullOrBlank()) displayName else video.displayName,
        folder = folder ?: video.folder,
        history = history ?: video.history,
        duration = if (duration == Media.Playable.UNDEFINED_DURATION) video.duration else duration,
        resolution = resolution ?: video.resolution,
        properties = properties ?: video.properties,
        localFile = localFile ?: video.localFile
    )
}