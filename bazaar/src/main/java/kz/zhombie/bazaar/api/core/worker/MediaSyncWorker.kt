package kz.zhombie.bazaar.api.core.worker

import android.content.Context
import androidx.work.*
import kz.zhombie.bazaar.Bazaar
import kz.zhombie.bazaar.api.core.settings.Mode
import java.util.concurrent.CancellationException

class MediaSyncWorker constructor(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = MediaSyncWorker::class.java.simpleName

        const val UNIQUE_WORK_NAME = "BazaarMediaSyncWork"

        suspend fun startWork(context: Context): Boolean {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(false)
                .setRequiresStorageNotLow(false)
                .setRequiresDeviceIdle(false)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val operation = WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<MediaSyncWorker>()
                        .setConstraints(constraints)
                        .build()
                )
            return try {
                val success = operation.await()
                @Suppress("USELESS_IS_CHECK")
                return success is Operation.State.SUCCESS
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                }
                false
            }
        }

        suspend fun cancelWork(context: Context): Boolean {
            val operation = WorkManager.getInstance(context)
                .cancelUniqueWork(UNIQUE_WORK_NAME)

            return try {
                val success = operation.await()
                @Suppress("USELESS_IS_CHECK")
                return success is Operation.State.SUCCESS
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                }
                false
            }
        }
    }

    override suspend fun doWork(): Result {
        Bazaar.preload(context, Mode.IMAGE_AND_VIDEO)
        Bazaar.preload(context, Mode.AUDIO)
        Bazaar.preload(context, Mode.DOCUMENT)
        return Result.success()
    }

}