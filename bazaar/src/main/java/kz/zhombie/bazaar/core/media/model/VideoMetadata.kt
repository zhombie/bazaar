package kz.zhombie.bazaar.core.media.model

import android.graphics.Bitmap

internal data class VideoMetadata constructor(
    val width: Int,
    val height: Int,
    val duration: Long,
    val thumbnail: Bitmap?
)