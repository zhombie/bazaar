package kz.zhombie.bazaar.api.core.settings

import java.io.Serializable

data class CameraSettings constructor(
    val isPhotoShootEnabled: Boolean,
    val isVideoCaptureEnabled: Boolean
) : Serializable {

    val isAnyCameraActionEnabled: Boolean
        get() = isPhotoShootEnabled || isVideoCaptureEnabled

}