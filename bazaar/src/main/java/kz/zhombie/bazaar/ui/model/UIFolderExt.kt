package kz.zhombie.bazaar.ui.model

import kz.garage.multimedia.store.model.*

internal fun List<UIContent>.convert(contents: List<Content>): List<UIFolder> {
    return contents.mapNotNull { media ->
        val folderId = media.folder?.id ?: return@mapNotNull null
        val folderDisplayName = media.folder?.displayName ?: return@mapNotNull null
        val items = mapNotNull { if (it.content.folder?.id == folderId) it.content else null }
        UIFolder(Folder(id = folderId, displayName = folderDisplayName, items = items))
    }
}

internal fun List<UIContent>.onlyImages(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it is UIMedia && it.media is Image) it.media else null }
        ),
        UIFolder.DisplayType.IMAGES
    )
}

internal fun List<UIContent>.onlyVideos(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it is UIMedia && it.media is Video) it.media else null }
        ),
        UIFolder.DisplayType.VIDEOS
    )
}

internal fun List<UIContent>.onlyImagesAndVideos(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull {
                if (it is UIMedia && (it.media is Image || it.media is Video)) {
                    it.media
                } else {
                    null
                }
            }
        ),
        UIFolder.DisplayType.MEDIA
    )
}

internal fun List<UIContent>.onlyAudios(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it.content is Audio) it.content else null }
        ),
        UIFolder.DisplayType.AUDIOS
    )
}

internal fun List<UIContent>.onlyDocuments(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it.content is Document) it.content else null }
        ),
        UIFolder.DisplayType.DOCUMENTS
    )
}