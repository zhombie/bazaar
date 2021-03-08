package kz.zhombie.bazaar.ui.media

import android.net.Uri
import kz.zhombie.bazaar.api.model.Image

object MediaStoreScreen {

    enum class State {
        LOADING,
        CONTENT
    }

    sealed class Action {
        data class TakePicture constructor(val input: Uri) : Action()
        data class TakenPictureResult constructor(val image: Image): Action()
    }

}