package kz.zhombie.bazaar.api.model

import kz.garage.multimedia.store.model.Image

internal fun Image?.complete(image: Image?): Image? {
    if (this == null) return null
    if (image == null) return null
    return Image(
        id = id,
        uri = uri,
        title = if (!title.isNullOrBlank()) title else image.title,
        displayName = if (!displayName.isNullOrBlank()) displayName else image.displayName,
        folder = folder ?: image.folder,
        history = history ?: image.history,
        resolution = resolution ?: image.resolution,
        properties = properties ?: image.properties,
        localFile = localFile ?: image.localFile
    )
}