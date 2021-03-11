package kz.zhombie.bazaar.ui.model

import android.graphics.Bitmap

internal data class VideoMetadata constructor(
    val width: Int,
    val height: Int,
    val duration: Long,
    val frame: Bitmap?
)