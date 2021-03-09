package kz.zhombie.bazaar.utils.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper

internal class GetContentContract : ActivityResultContract<Array<String>, Uri?>() {

    @CallSuper
    override fun createIntent(context: Context, input: Array<String>?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, input)
    }

    override fun getSynchronousResult(context: Context, input: Array<String>?): SynchronousResult<Uri?>? {
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
    }

}