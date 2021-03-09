package kz.zhombie.bazaar.ui.media

import android.net.Uri
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Video
import java.io.Serializable

object MediaStoreScreen {

    data class Settings constructor(
        val mode: Mode,
        val maxSelectionCount: Int,
        val cameraSettings: CameraSettings,
        val isLocalMediaSearchAndSelectEnabled: Boolean,
        val isAlbumBasedInterfaceEnabled: Boolean
    ) : Serializable

    enum class State {
        LOADING,
        CONTENT
    }

    sealed class Action {
        data class TakePicture constructor(val input: Uri) : Action()
        data class TakenPictureResult constructor(val image: Image): Action()

        object SelectLocalMediaGalleryImage : Action()
        data class SelectedLocalMediaGalleryImageResult constructor(val image: Image) : Action()

        object SelectLocalMediaGalleryImages : Action()
        data class SelectedLocalMediaGalleryImagesResult constructor(val images: List<Image>) : Action()

        object SelectLocalMediaGalleryVideo : Action()
        data class SelectedLocalMediaGalleryVideoResult constructor(val video: Video) : Action()

        object SelectLocalMediaGalleryVideos : Action()
        data class SelectedLocalMediaGalleryVideosResult constructor(val videos: List<Video>) : Action()
    }

}