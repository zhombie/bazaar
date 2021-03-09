package kz.zhombie.bazaar

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.result.AbstractResultCallback

class MainActivity : AppCompatActivity(), ResultCallback {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName

        private val DEFAULT_IMAGE_LOAD = "Glide" to GlideImageLoader()
    }

    private object RequestCode {
        const val READ_EXTERNAL_STORAGE = 100
    }

    private lateinit var imageLoaderView: MaterialTextView
    private lateinit var imageLoaderButton: MaterialButton
    private lateinit var showButton: MaterialButton
    private lateinit var recyclerView: RecyclerView

    private lateinit var imageLoader: ImageLoader

    private lateinit var adapter: MediaResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageLoaderView = findViewById(R.id.imageLoaderView)
        imageLoaderButton = findViewById(R.id.imageLoaderButton)
        showButton = findViewById(R.id.showButton)
        recyclerView = findViewById(R.id.recyclerView)

        imageLoaderView.text = DEFAULT_IMAGE_LOAD.first
        imageLoader = DEFAULT_IMAGE_LOAD.second

        adapter = MediaResultAdapter(imageLoader)
        recyclerView.adapter = adapter

        imageLoaderButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setSingleChoiceItems(arrayOf("Coil", "Glide"), -1) { dialog, which ->
                    dialog.dismiss()
                    when (which) {
                        0 -> {
                            imageLoader = CoilImageLoader()
                            imageLoaderView.text = "Coil"
                        }
                        1 -> {
                            imageLoader = GlideImageLoader()
                            imageLoaderView.text = "Glide"
                        }
                        else -> {
                            imageLoader = DEFAULT_IMAGE_LOAD.second
                            imageLoaderView.text = DEFAULT_IMAGE_LOAD.first
                        }
                    }
                    adapter.imageLoader = imageLoader
                }
                .show()
        }

        showButton.setOnClickListener {
            if (checkPermissions()) {
                Bazaar.Builder(AbstractResultCallback { adapter.media = it })
                    .setTag(Bazaar.TAG)
                    .setImageLoader(imageLoader)
                    .setMode(Mode.VIDEO)
                    .setMaxSelectionCount(5)
                    .setCameraSettings(CameraSettings(isPhotoShootEnabled = true, isVideoCaptureEnabled = true))
                    .setLocalMediaSearchAndSelectEnabled(true)
                    .setAlbumBasedInterfaceEnabled(true)
                    .show(supportFragmentManager)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return if (permissions.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED}) {
            true
        } else {
            ActivityCompat.requestPermissions(this, permissions, RequestCode.READ_EXTERNAL_STORAGE)
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

    override fun onCameraResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter.media = listOf(media)
    }

    override fun onLocalMediaGalleryResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter.media = listOf(media)
    }

    override fun onLocalMediaGalleryResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter.media = media
    }

    override fun onMediaGallerySelectResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter.media = media
    }

}