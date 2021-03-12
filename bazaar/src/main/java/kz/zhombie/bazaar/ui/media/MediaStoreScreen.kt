package kz.zhombie.bazaar.ui.media

import android.net.Uri
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import java.io.Serializable

internal object MediaStoreScreen {

    data class Settings constructor(
        val mode: Mode,
        val maxSelectionCount: Int,
        val cameraSettings: CameraSettings,
        val isLocalMediaSearchAndSelectEnabled: Boolean,
        val isFolderBasedInterfaceEnabled: Boolean
    ) : Serializable

    enum class State {
        LOADING,
        CONTENT
    }

    sealed class Action {
        data class SubmitSelectedMedia(val media: List<Media>) : Action()

        data class TakePicture constructor(val input: Uri) : Action()
        data class TakenPictureResult constructor(val image: Image): Action()

        data class TakeVideo constructor(val input: Uri) : Action()
        data class TakenVideoResult constructor(val video: Video): Action()

        object SelectLocalMediaGalleryImage : Action()
        data class SelectedLocalMediaGalleryImageResult constructor(val image: Image) : Action()

        object SelectLocalMediaGalleryImages : Action()
        data class SelectedLocalMediaGalleryImagesResult constructor(val images: List<Image>) : Action()

        object SelectLocalMediaGalleryVideo : Action()
        data class SelectedLocalMediaGalleryVideoResult constructor(val video: Video) : Action()

        object SelectLocalMediaGalleryVideos : Action()
        data class SelectedLocalMediaGalleryVideosResult constructor(val videos: List<Video>) : Action()

        object SelectLocalMediaGalleryImageOrVideo : Action()
        data class SelectedLocalMediaGalleryImageOrVideoResult constructor(val media: Media) : Action()

        object SelectLocalMediaGalleryImagesOrVideos : Action()
        data class SelectedLocalMediaGalleryImagesOrVideosResult constructor(val media: List<Media>) : Action()
    }

}