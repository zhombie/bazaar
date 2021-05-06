package kz.zhombie.bazaar.ui.model

import kz.zhombie.bazaar.api.model.Folder
import kz.zhombie.bazaar.api.model.Multimedia

internal fun List<UIMultimedia>.convert(multimedia: List<Multimedia>): List<UIFolder> {
    return multimedia.mapNotNull { media ->
        val folderId = media.folderId ?: return@mapNotNull null
        val folderDisplayName = media.folderDisplayName ?: return@mapNotNull null
        val items = mapNotNull { if (it.multimedia.folderId == folderId) it.multimedia else null }
        UIFolder(Folder(id = folderId, displayName = folderDisplayName, items = items))
    }
}

internal fun List<UIMultimedia>.onlyImages(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it is UIMedia && it.isImage()) it.media else null }
        ),
        UIFolder.DisplayType.IMAGES
    )
}

internal fun List<UIMultimedia>.onlyVideos(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it is UIMedia && it.isVideo()) it.media else null }
        ),
        UIFolder.DisplayType.VIDEOS
    )
}

internal fun List<UIMultimedia>.onlyMedia(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it is UIMedia && it.isImageOrVideo()) it.media else null }
        ),
        UIFolder.DisplayType.MEDIA
    )
}

internal fun List<UIMultimedia>.onlyAudios(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it.isAudio()) it.multimedia else null }
        ),
        UIFolder.DisplayType.AUDIOS
    )
}

internal fun List<UIMultimedia>.onlyDocuments(): UIFolder {
    return UIFolder(
        Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it.isDocument()) it.multimedia else null }
        ),
        UIFolder.DisplayType.DOCUMENTS
    )
}