package kz.zhombie.bazaar.api.core.worker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kz.zhombie.bazaar.Bazaar
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.utils.isPermissionGranted

@Suppress("USELESS_IS_CHECK")
class MediaSyncWorker constructor(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = MediaSyncWorker::class.java.simpleName

        const val UNIQUE_WORK_NAME = "BazaarMediaSyncWork"

        suspend fun startWork(context: Context): Boolean =
            try {
                val success = WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        UNIQUE_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<MediaSyncWorker>()
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiresBatteryNotLow(true)
                                    .setRequiresCharging(false)
                                    .setRequiresStorageNotLow(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                    .build()
                            )
                            .build()
                    )
                    .await()
                success is Operation.State.SUCCESS
            } catch (e: Exception) {
                if (e !is CancellationException) e.printStackTrace()
                false
            }

        suspend fun cancelWork(context: Context): Boolean =
            try {
                val success = WorkManager.getInstance(context)
                    .cancelUniqueWork(UNIQUE_WORK_NAME)
                    .await()
                success is Operation.State.SUCCESS
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                }
                false
            }
    }

    override suspend fun doWork(): Result =
        try {
            @SuppressLint("MissingPermission")
            if (context.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Bazaar.preload(context, Mode.IMAGE_AND_VIDEO)
                Bazaar.preload(context, Mode.AUDIO)
                Bazaar.preload(context, Mode.DOCUMENT)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            if (e !is CancellationException) e.printStackTrace()
            Result.failure()
        }

}