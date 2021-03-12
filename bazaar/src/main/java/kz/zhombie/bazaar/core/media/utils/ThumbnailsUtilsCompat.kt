package kz.zhombie.bazaar.core.media.utils

import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import java.io.File

internal class ThumbnailsUtilsCompat {

    companion object {
        fun createVideoThumbnail(file: File, size: Size, kind: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(file, size, null)
            } else {
                @Suppress("DEPRECATION")
                ThumbnailUtils.createVideoThumbnail(file.absolutePath, kind)
            }
        }
    }

}