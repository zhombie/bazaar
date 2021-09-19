package kz.zhombie.bazaar.api.model

import kz.zhombie.multimedia.model.Document

internal fun Document?.complete(document: Document?): Document? {
    if (this == null) return null
    if (document == null) return null
    return Document(
        id = id,
        uri = uri,
        title = if (!title.isNullOrBlank()) title else document.title,
        displayName = if (!displayName.isNullOrBlank()) displayName else document.displayName,
        folder = folder ?: document.folder,
        history = history ?: document.history,
        properties = properties ?: document.properties,
        localFile = localFile ?: document.localFile
    )
}