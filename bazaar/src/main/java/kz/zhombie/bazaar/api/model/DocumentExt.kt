package kz.zhombie.bazaar.api.model

import kz.garage.multimedia.store.model.Document

internal fun Document?.complete(document: Document?): Document? {
    if (this == null) return null
    if (document == null) return null
    return Document(
        id = id,
        uri = uri,
        title = if (title.isNullOrBlank()) document.title else title,
        displayName = if (displayName.isNullOrBlank()) document.displayName else displayName,
        folder = folder ?: document.folder,
        history = history ?: document.history,
        properties = properties ?: document.properties,
        publicFile = publicFile ?: document.publicFile,
        remoteFile = remoteFile ?: document.remoteFile
    )
}