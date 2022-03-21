package kz.zhombie.bazaar.ui.model

import kz.garage.multimedia.store.model.*

internal fun List<UIContent>.convert(contents: List<Content>): List<UIFolder> =
    contents.mapNotNull { media ->
        val folderId = media.folder?.id ?: return@mapNotNull null
        val items = mapNotNull { if (it.content.folder?.id == folderId) it.content else null }
        UIFolder(
            folder = Folder(
                id = folderId,
                displayName = media.folder?.displayName ?: return@mapNotNull null,
                items = items
            )
        )
    }

internal fun List<UIContent>.onlyImages(): UIFolder =
    UIFolder(
        folder = Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it is UIMedia && it.media is Image) it.media else null }
        ),
        displayType = UIFolder.DisplayType.IMAGES
    )

internal fun List<UIContent>.onlyVideos(): UIFolder =
    UIFolder(
        folder = Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it is UIMedia && it.media is Video) it.media else null }
        ),
        displayType = UIFolder.DisplayType.VIDEOS
    )

internal fun List<UIContent>.onlyImagesAndVideos(): UIFolder =
    UIFolder(
        folder = Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull {
                if (it is UIMedia && (it.media is Image || it.media is Video)) {
                    it.media
                } else {
                    null
                }
            }
        ),
        displayType = UIFolder.DisplayType.MEDIA
    )

internal fun List<UIContent>.onlyAudios(): UIFolder =
    UIFolder(
        folder = Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it.content is Audio) it.content else null }
        ),
        displayType = UIFolder.DisplayType.AUDIOS
    )

internal fun List<UIContent>.onlyDocuments(): UIFolder =
    UIFolder(
        folder = Folder(
            id = UIFolder.ALL_MEDIA_ID,
            items = mapNotNull { if (it.content is Document) it.content else null }
        ),
        displayType = UIFolder.DisplayType.DOCUMENTS
    )
