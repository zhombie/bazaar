package kz.zhombie.bazaar.core.media.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.core.media.model.AudioMetadata
import kz.zhombie.bazaar.core.media.model.VideoMetadata

internal suspend fun Uri?.retrieveAudioMetadata(context: Context): AudioMetadata? = withContext(Dispatchers.IO) {
    if (this@retrieveAudioMetadata == null) return@withContext null
    return@withContext runCatching {
        context.contentResolver
            ?.openFileDescriptor(this@retrieveAudioMetadata, "r")
            ?.use {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(it.fileDescriptor)

                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

                retriever.release()

                return@withContext AudioMetadata(
                    duration = duration
                )
            }
    }.getOrNull()
}


internal suspend fun Uri?.retrieveVideoMetadata(context: Context): VideoMetadata? = withContext(Dispatchers.IO) {
    if (this@retrieveVideoMetadata == null) return@withContext null
    return@withContext runCatching {
        context.contentResolver
            ?.openFileDescriptor(this@retrieveVideoMetadata, "r")
            ?.use {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(it.fileDescriptor)

                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()

//                val embeddedPicture = retriever.embeddedPicture
//                val thumbnail = if (embeddedPicture?.isNotEmpty() == true) {
//                    BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
//                } else {
//                    val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//                        retriever.getScaledFrameAtTime(
//                            -1,
//                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
//                            300,
//                            300
//                        )
//                    } else {
//                        retriever.frameAtTime ?: retriever.getFrameAtTime(-1)
//                    }
//                    frame
//                }

                retriever.release()

                return@withContext VideoMetadata(
                    duration = duration,
                    width = width,
                    height = height,
                    thumbnail = null
                )
            }
    }.getOrNull()
}