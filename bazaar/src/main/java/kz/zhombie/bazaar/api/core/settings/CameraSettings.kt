package kz.zhombie.bazaar.api.core.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CameraSettings constructor(
    val isPhotoShootEnabled: Boolean,
    val isVideoCaptureEnabled: Boolean
) : Parcelable