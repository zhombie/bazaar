package kz.zhombie.bazaar.api.model

import kz.zhombie.multimedia.model.Audio
import kz.zhombie.multimedia.model.Media

internal fun Audio.complete(audio: Audio?): Audio? {
    if (audio == null) return null
    return Audio(
        id = id,
        uri = uri,
        title = if (!title.isNullOrBlank()) title else audio.title,
        displayName = if (!displayName.isNullOrBlank()) displayName else audio.displayName,
        folder = folder ?: audio.folder,
        history = history ?: audio.history,
        duration = if (duration == Media.Playable.UNDEFINED_DURATION) audio.duration else duration,
        properties = properties ?: audio.properties,
        album = album ?: audio.album,
        localFile = localFile ?: audio.localFile
    )
}