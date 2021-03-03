package kz.zhombie.bazaar.utils

import android.os.Build
import android.provider.MediaStore

internal class ContentResolverCompat {

    companion object {
        private val TAG: String = ContentResolverCompat::class.java.simpleName

        fun getProjection(type: Type): Array<String> {
            val projection: MutableList<String> = when (type) {
                Type.IMAGE -> mutableListOf(
                    MediaStore.Images.ImageColumns._ID,
                    MediaStore.Images.ImageColumns.DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.TITLE,
                    MediaStore.Images.ImageColumns.SIZE,
                    MediaStore.Images.ImageColumns.DATE_ADDED,
                    MediaStore.Images.ImageColumns.DATE_MODIFIED,
                    MediaStore.Images.ImageColumns.MIME_TYPE,
                    MediaStore.Images.ImageColumns.WIDTH,
                    MediaStore.Images.ImageColumns.HEIGHT
                )
                Type.VIDEO -> mutableListOf(
                    MediaStore.Video.VideoColumns._ID,
                    MediaStore.Video.VideoColumns.TITLE,
                    MediaStore.Video.VideoColumns.DISPLAY_NAME,
                    MediaStore.Video.VideoColumns.DATE_ADDED,
                    MediaStore.Video.VideoColumns.DATE_MODIFIED,
                    MediaStore.Video.VideoColumns.MIME_TYPE,
                    MediaStore.Video.VideoColumns.SIZE,
                    MediaStore.Video.VideoColumns.WIDTH,
                    MediaStore.Video.VideoColumns.HEIGHT
                )
                Type.FILE -> mutableListOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.TITLE,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.WIDTH,
                    MediaStore.Files.FileColumns.HEIGHT
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                when (type) {
                    Type.IMAGE -> {
                        projection.add(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
                        projection.add(MediaStore.Images.ImageColumns.DATE_TAKEN)
                        projection.add(MediaStore.Images.ImageColumns.VOLUME_NAME)
                    }
                    Type.VIDEO -> {
                        projection.add(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME)
                        projection.add(MediaStore.Video.VideoColumns.DATE_TAKEN)
                        projection.add(MediaStore.Video.VideoColumns.DURATION)
                        projection.add(MediaStore.Video.VideoColumns.VOLUME_NAME)
                    }
                    Type.FILE -> {
                        projection.add(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                        projection.add(MediaStore.Files.FileColumns.DATE_TAKEN)
                        projection.add(MediaStore.Files.FileColumns.DURATION)
                        projection.add(MediaStore.Files.FileColumns.VOLUME_NAME)
                    }
                }
            }
            return projection.toTypedArray()
        }
    }

    enum class Type {
        IMAGE,
        VIDEO,
        FILE
    }

}