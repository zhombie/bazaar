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
import kz.zhombie.bazaar.api.ImageLoader
import kz.zhombie.bazaar.api.ResultCallback
import kz.zhombie.bazaar.api.model.Media

class MainActivity : AppCompatActivity(), ResultCallback {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName

        private val DEFAULT_IMAGE_LOAD = "Coil" to CoilImageLoader()
    }

    private object RequestCode {
        const val READ_EXTERNAL_STORAGE = 100
    }

    private lateinit var imageLoaderView: MaterialTextView
    private lateinit var imageLoaderButton: MaterialButton
    private lateinit var showButton: MaterialButton
    private lateinit var recyclerView: RecyclerView

    private lateinit var imageLoader: ImageLoader

    private lateinit var adapter: ResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageLoaderView = findViewById(R.id.imageLoaderView)
        imageLoaderButton = findViewById(R.id.imageLoaderButton)
        showButton = findViewById(R.id.showButton)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = ResultAdapter()
        recyclerView.adapter = adapter

        imageLoaderView.text = DEFAULT_IMAGE_LOAD.first
        imageLoader = DEFAULT_IMAGE_LOAD.second

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
                }
                .show()
        }

        showButton.setOnClickListener {
            if (checkPermissions()) {
                Bazaar.Builder(this)
                    .setTag(Bazaar.TAG)
                    .setImageLoader(imageLoader)
                    .show(supportFragmentManager)
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

    override fun onCameraResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter.media = listOf(media)
    }

    override fun onGalleryResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter.media = listOf(media)
    }

    override fun onMediaSelectResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter.media = media
    }

}