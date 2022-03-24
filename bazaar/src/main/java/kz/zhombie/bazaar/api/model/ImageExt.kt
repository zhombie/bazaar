package kz.zhombie.bazaar.api.model

import kz.garage.multimedia.store.model.Image

internal fun Image.complete(image: Image?): Image? {
    if (image == null) return null
    return Image(
        id = id,
        uri = uri,
        title = if (title.isNullOrBlank()) image.title else title,
        displayName = if (displayName.isNullOrBlank()) image.displayName else displayName,
        folder = folder ?: image.folder,
        history = history ?: image.history,
        resolution = resolution ?: image.resolution,
        properties = properties ?: image.properties,
        publicFile = publicFile ?: image.publicFile,
        remoteFile = remoteFile ?: image.remoteFile
    )
}