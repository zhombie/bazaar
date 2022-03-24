package kz.zhombie.bazaar.api.model

import kz.garage.multimedia.store.model.Audio

internal fun Audio.complete(audio: Audio?): Audio? {
    if (audio == null) return null
    return Audio(
        id = id,
        uri = uri,
        title = if (title.isNullOrBlank()) audio.title else title,
        displayName = if (displayName.isNullOrBlank()) audio.displayName else displayName,
        folder = folder ?: audio.folder,
        history = history ?: audio.history,
        duration = if (duration == null || duration == -1L) audio.duration else duration,
        properties = properties ?: audio.properties,
        album = album ?: audio.album,
        publicFile = publicFile ?: audio.publicFile,
        remoteFile = remoteFile ?: audio.remoteFile
    )
}