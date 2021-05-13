package kz.zhombie.bazaar.utils.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper

/**
 * An [ActivityResultContract] to
 * [take a video][MediaStore.ACTION_VIDEO_CAPTURE] saving it into the provided
 * content-[Uri].
 *
 * Returns a thumbnail.
 *
 * This can be extended to override [createIntent] if you wish to pass additional
 * extras to the Intent created by `super.createIntent()`.
 */
class TakeVideoContract : ActivityResultContract<Uri, Uri?>() {

    @CallSuper
    override fun createIntent(context: Context, input: Uri?): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
    }

    override fun getSynchronousResult(context: Context, input: Uri?): SynchronousResult<Uri?>? {
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) {
            null
        } else {
            intent.data
        }
    }

}