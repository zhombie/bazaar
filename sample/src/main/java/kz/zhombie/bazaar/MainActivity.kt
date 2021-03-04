package kz.zhombie.bazaar

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private object RequestCode {
        const val READ_EXTERNAL_STORAGE = 100
    }

    private var button: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)

        button?.setOnClickListener {
            if (checkPermissions()) {
                Bazaar.Builder(supportFragmentManager)
                    .setTag(Bazaar.TAG)
                    .setImageLoader(CoilImageLoader())
                    .show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), RequestCode.READ_EXTERNAL_STORAGE)
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RequestCode.READ_EXTERNAL_STORAGE) {
            checkPermissions()
        }
    }

}