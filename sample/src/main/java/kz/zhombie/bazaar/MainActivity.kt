package kz.zhombie.bazaar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Multimedia
import kz.zhombie.bazaar.api.result.AbstractResultCallback
import kz.zhombie.bazaar.api.result.ResultCallback

class MainActivity : AppCompatActivity(), ResultCallback {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName

        private val IMAGE_LOADER_GLIDE = "Glide" to GlideImageLoader()
        private val IMAGE_LOADER_COIL = "Coil" to CoilImageLoader()
        private val DEFAULT_IMAGE_LOADER = IMAGE_LOADER_GLIDE
    }

    private object RequestCode {
        const val EXTERNAL_STORAGE_ACCESS = 100
    }

    private lateinit var imageLoaderView: MaterialTextView
    private lateinit var imageLoaderButton: MaterialButton
    private lateinit var modeView: MaterialTextView
    private lateinit var modeButton: MaterialButton
    private lateinit var maxSelectionCountView: MaterialTextView
    private lateinit var maxSelectionCountButton: MaterialButton
    private lateinit var showButton: MaterialButton
    private lateinit var recyclerView: RecyclerView

    private lateinit var imageLoader: ImageLoader
    private lateinit var mode: Mode
    private var maxSelectionCount: Int = 3

    private lateinit var adapter: MediaResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageLoaderView = findViewById(R.id.imageLoaderView)
        imageLoaderButton = findViewById(R.id.imageLoaderButton)
        modeView = findViewById(R.id.modeView)
        modeButton = findViewById(R.id.modeButton)
        maxSelectionCountView = findViewById(R.id.maxSelectionCountView)
        maxSelectionCountButton = findViewById(R.id.maxSelectionCountButton)
        showButton = findViewById(R.id.showButton)
        recyclerView = findViewById(R.id.recyclerView)

        imageLoader = DEFAULT_IMAGE_LOADER.second
        imageLoaderView.text = DEFAULT_IMAGE_LOADER.first

        Bazaar.init(imageLoader, true)

        mode = Mode.IMAGE_AND_VIDEO
        modeView.text = mode.toString()

        maxSelectionCount = 3
        maxSelectionCountView.text = maxSelectionCount.toString()

        adapter = MediaResultAdapter(imageLoader)
        recyclerView.adapter = adapter

        imageLoaderButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Image loader")
                .setSingleChoiceItems(arrayOf(IMAGE_LOADER_COIL.first, IMAGE_LOADER_GLIDE.first), -1) { dialog, which ->
                    dialog.dismiss()
                    val selectedImageLoader = when (which) {
                        0 -> IMAGE_LOADER_COIL
                        1 -> IMAGE_LOADER_GLIDE
                        else -> IMAGE_LOADER_GLIDE
                    }

                    imageLoader = selectedImageLoader.second
                    imageLoaderView.text = selectedImageLoader.first
                    adapter.imageLoader = selectedImageLoader.second
                }
                .show()
        }

        modeButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Mode")
                .setSingleChoiceItems(
                    arrayOf(
                        Mode.IMAGE.toString(),
                        Mode.VIDEO.toString(),
                        Mode.IMAGE_AND_VIDEO.toString(),
                        Mode.AUDIO.toString(),
                        Mode.DOCUMENT.toString(),
                    ),
                    -1
                ) { dialog, which ->
                    dialog.dismiss()
                    mode = when (which) {
                        0 -> Mode.IMAGE
                        1 -> Mode.VIDEO
                        2 -> Mode.IMAGE_AND_VIDEO
                        3 -> Mode.AUDIO
                        4 -> Mode.DOCUMENT
                        else -> Mode.IMAGE_AND_VIDEO
                    }
                    modeView.text = mode.toString()
                }
                .show()
        }

        maxSelectionCountButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Max selection count")
                .setSingleChoiceItems(arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), -1) { dialog, which ->
                    dialog.dismiss()
                    maxSelectionCount = which + 1
                    maxSelectionCountView.text = maxSelectionCount.toString()
                }
                .show()
        }

        showButton.setOnClickListener {
            if (checkPermissions()) {
                Bazaar.Builder(object : AbstractResultCallback {
                    override fun onMultimediaSelectResult(multimedia: List<Multimedia>) {
                        Log.d(TAG, "multimedia: $multimedia")
                        adapter.multimedia = multimedia
                    }

                    override fun onMediaSelectResult(media: List<Media>) {
                        Log.d(TAG, "media: $media")
                        adapter.multimedia = media
                    }
                })
                    .setTag(Bazaar.TAG)
//                    .setImageLoader(imageLoader)
                    .setEventListener {
                        Log.d(TAG, "onDestroy()")
                    }
                    .setMode(mode)
                    .setMaxSelectionCount(maxSelectionCount)
                    .setCameraSettings(CameraSettings(isPhotoShootEnabled = true, isVideoCaptureEnabled = true))
                    .setLocalMediaSearchAndSelectEnabled(true)
                    .setFoldersBasedInterfaceEnabled(true)
                    .show(supportFragmentManager)
            }
        }

//        lifecycleScope.launch(Dispatchers.IO) {
//            Bazaar.preloadAll(this@MainActivity)
//        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "launch", Toast.LENGTH_SHORT).show()
            }

            Bazaar.sync(applicationContext)
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        return if (permissions.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED}) {
            true
        } else {
            ActivityCompat.requestPermissions(this, permissions, RequestCode.EXTERNAL_STORAGE_ACCESS)
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RequestCode.EXTERNAL_STORAGE_ACCESS) {
            checkPermissions()
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch(Dispatchers.IO) {
            Bazaar.destroyCache()
        }

        super.onDestroy()
    }

    override fun onCameraResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter.multimedia = listOf(media)
    }

    override fun onLocalMediaStoreResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter.multimedia = listOf(media)
    }

    override fun onLocalMediaStoreResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter.multimedia = media
    }

    override fun onMultimediaLocalMediaStoreResult(multimedia: Multimedia) {
        Log.d(TAG, "multimedia: $multimedia")
        adapter.multimedia = listOf(multimedia)
    }

    override fun onMultimediaLocalMediaStoreResult(multimedia: List<Multimedia>) {
        Log.d(TAG, "multimedia: $multimedia")
        adapter.multimedia = multimedia
    }

    override fun onMediaGallerySelectResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter.multimedia = media
    }

    override fun onMultimediaGallerySelectResult(multimedia: List<Multimedia>) {
        Log.d(TAG, "multimedia: $multimedia")
        adapter.multimedia = multimedia
    }

}