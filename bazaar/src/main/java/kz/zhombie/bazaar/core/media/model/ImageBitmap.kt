package kz.zhombie.bazaar.core.media.model

import android.graphics.Bitmap

internal data class ImageBitmap constructor(
    val source: Source,
    val processed: Processed
) {

    data class Source constructor(
        val bitmap: Bitmap,
        val size: Size
    )

    data class Processed constructor(
        val bitmap: Bitmap
    )

    data class Size constructor(
        val width: Int,
        val height: Int
    )

}