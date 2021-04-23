package kz.zhombie.bazaar.ui.presentation

import android.net.Uri
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.model.*
import java.io.Serializable

internal object MediaStoreScreen {

    data class Settings constructor(
        val mode: Mode,
        val maxSelectionCount: Int,
        val cameraSettings: CameraSettings,
        val isLocalMediaSearchAndSelectEnabled: Boolean,
        val isFolderBasedInterfaceEnabled: Boolean
    ) : Serializable {

        fun isCameraShouldBeAvailable(): Boolean {
            return if (isVisualMediaMode()) {
                cameraSettings.isAnyCameraActionEnabled
            } else {
                false
            }
        }

        fun isVisualMediaMode(): Boolean {
            return mode == Mode.IMAGE || mode == Mode.VIDEO || mode == Mode.IMAGE_AND_VIDEO
        }

        fun isAudibleMediaMode(): Boolean {
            return mode == Mode.AUDIO
        }

        fun isDocumentMode(): Boolean {
            return mode == Mode.DOCUMENT
        }

    }

    enum class State {
        LOADING,
        CONTENT
    }

    sealed class Action {
        data class SubmitSelectedMedia(val media: List<Media>) : Action()
        data class SubmitSelectedMultimedia(val multimedia: List<Multimedia>) : Action()

        object ChooseBetweenTakePictureOrVideo : Action()

        data class TakePicture constructor(val input: Uri) : Action()
        data class TakenPictureResult constructor(val image: Image): Action()

        data class TakeVideo constructor(val input: Uri) : Action()
        data class TakenVideoResult constructor(val video: Video): Action()

        object SelectLocalMediaImage : Action()
        data class SelectedLocalMediaImageResult constructor(val image: Image) : Action()

        object SelectLocalMediaImages : Action()
        data class SelectedLocalMediaImagesResult constructor(val images: List<Image>) : Action()

        object SelectLocalMediaVideo : Action()
        data class SelectedLocalMediaVideoResult constructor(val video: Video) : Action()

        object SelectLocalMediaVideos : Action()
        data class SelectedLocalMediaVideosResult constructor(val videos: List<Video>) : Action()

        object SelectLocalMediaImageOrVideo : Action()
        data class SelectedLocalMediaImageOrVideoResult constructor(val media: Media) : Action()

        object SelectLocalMediaImagesAndVideos : Action()
        data class SelectedLocalMediaImagesAndVideosResult constructor(val media: List<Media>) : Action()

        object SelectLocalMediaAudio : Action()
        data class SelectedLocalMediaAudio constructor(val audio: Audio) : Action()

        object SelectLocalMediaAudios : Action()
        data class SelectedLocalMediaAudios constructor(val audios: List<Audio>) : Action()

        object SelectLocalDocument : Action()
        data class SelectedLocalDocument constructor(val document: Document) : Action()

        object SelectLocalDocuments : Action()
        data class SelectedLocalDocuments constructor(val documents: List<Document>) : Action()

        object Empty : Action()
    }

}