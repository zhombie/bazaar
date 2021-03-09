package kz.zhombie.bazaar.ui.media

import android.net.Uri
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.model.Image
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

        object SelectGalleryImage : Action()
        data class SelectedGalleryImageResult constructor(val image: Image) : Action()

        object SelectGalleryImages : Action()
        data class SelectedGalleryImagesResult constructor(val images: List<Image>) : Action()
    }

}