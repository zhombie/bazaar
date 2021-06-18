package kz.zhombie.bazaar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.Bazaar
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.core.showSafely
import kz.zhombie.bazaar.api.event.EventListener
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Multimedia
import kz.zhombie.bazaar.api.result.AbstractResultCallback
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.imageloader.CoilImageLoader
import kz.zhombie.bazaar.imageloader.GlideImageLoader
import kz.zhombie.bazaar.ui.adapter.MultimediaResultAdapter
import kz.zhombie.bazaar.utils.OpenFileAction
import kz.zhombie.bazaar.utils.open
import kz.zhombie.bazaar.utils.tryToLaunch
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.museum.MuseumDialogFragment
import java.io.File

class MainActivity : AppCompatActivity(), ResultCallback {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private object RequestCode {
        const val EXTERNAL_STORAGE_ACCESS = 100
    }

    private lateinit var glideImageLoader: Pair<String, ImageLoader>
    private lateinit var coilImageLoader: Pair<String, ImageLoader>
    private lateinit var defaultImageLoader: Pair<String, ImageLoader>

    private lateinit var imageLoader: ImageLoader

    private var viewHolder: ViewHolder? = null

    private var adapter: MultimediaResultAdapter? = null

    private var mode: Mode? = null
        set(value) {
            field = value

            if (value == null) {
                viewHolder?.modeView?.text = "ALL"
            } else {
                viewHolder?.modeView?.text = value.toString()
            }
        }

    private var maxSelectionCount: Int = 3
        set(value) {
            field = value
            viewHolder?.maxSelectionCountView?.text = value.toString()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewHolder = ViewHolder(this)

        setup()

        setupRecyclerView()
        setupImageLoaderButton()
        setupModeButton()
        setupMaxSelectionCountButton()
        setupShowButton()

//        lifecycleScope.launch(Dispatchers.IO) {
//            Bazaar.preloadAll(this@MainActivity)
//        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "launch background media sync", Toast.LENGTH_SHORT).show()
            }

            Bazaar.sync(applicationContext)
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        return if (permissions.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED}) {
            true
        } else {
            ActivityCompat.requestPermissions(this, permissions,
                RequestCode.EXTERNAL_STORAGE_ACCESS
            )
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

        if (imageLoader is CoilImageLoader) {
            (imageLoader as? CoilImageLoader)?.clearCache()
        }

        adapter = null

        viewHolder = null
    }

    private fun setup() {
        // Image loader declaration
        glideImageLoader = "Glide" to GlideImageLoader()
        coilImageLoader = "Coil" to CoilImageLoader(this)
        defaultImageLoader = coilImageLoader

        imageLoader = defaultImageLoader.second
        viewHolder?.imageLoaderView?.text = defaultImageLoader.first

        // Library initialization
        MuseumDialogFragment.init(imageLoader, true)
        CinemaDialogFragment.init(true)
        Bazaar.init(imageLoader, true)

        // Mode
        mode = Mode.IMAGE_AND_VIDEO

        // Max selection count
        maxSelectionCount = 3
    }

    private fun setupRecyclerView() {
        adapter = MultimediaResultAdapter(imageLoader) { multimedia ->
            val path = multimedia.path
            val file = if (!path.isNullOrBlank()) File(path) else null
            when (val action = file?.open(this)) {
                is OpenFileAction.Success -> {
                    if (!action.tryToLaunch(this)) {
                        Toast.makeText(this, "error_file_cannot_be_read", Toast.LENGTH_SHORT).show()
                    }
                }
                is OpenFileAction.Error -> {
                    when (action.reason) {
                        OpenFileAction.Error.Reason.UNKNOWN -> {
                            Toast.makeText(this, "error_file_cannot_be_read", Toast.LENGTH_SHORT).show()
                        }
                        OpenFileAction.Error.Reason.FILE_DOES_NOT_EXIST -> {
                            Toast.makeText(this, "not_found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        viewHolder?.recyclerView?.adapter = adapter
    }

    private fun setupImageLoaderButton() {
        viewHolder?.imageLoaderButton?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Image loader")
                .setSingleChoiceItems(arrayOf(coilImageLoader.first, glideImageLoader.first), -1) { dialog, which ->
                    dialog.dismiss()
                    val selectedImageLoader = when (which) {
                        0 -> coilImageLoader
                        1 -> glideImageLoader
                        else -> glideImageLoader
                    }

                    imageLoader = selectedImageLoader.second
                    viewHolder?.imageLoaderView?.text = selectedImageLoader.first
                    adapter?.imageLoader = selectedImageLoader.second
                }
                .show()
        }
    }

    private fun setupModeButton() {
        viewHolder?.modeButton?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Mode")
                .setSingleChoiceItems(
                    arrayOf(
                        "ALL",
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
                        0 -> null
                        1 -> Mode.IMAGE
                        2 -> Mode.VIDEO
                        3 -> Mode.IMAGE_AND_VIDEO
                        4 -> Mode.AUDIO
                        5 -> Mode.DOCUMENT
                        else -> null
                    }
                }
                .show()
        }
    }

    private fun setupMaxSelectionCountButton() {
        viewHolder?.maxSelectionCountButton?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Max selection count")
                .setSingleChoiceItems(arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), -1) { dialog, which ->
                    dialog.dismiss()
                    maxSelectionCount = which + 1
                }
                .show()
        }
    }

    private fun setupShowButton() {
        viewHolder?.showButton?.setOnClickListener {
            if (checkPermissions()) {
                if (mode == null) {
                    Bazaar.selectMode(this) { selectedMode ->
                        launchBazaar(selectedMode)
                    }
                } else {
                    launchBazaar(requireNotNull(mode))
                }
            }
        }
    }

    private fun launchBazaar(mode: Mode) {
        Bazaar.Builder(object : AbstractResultCallback {
            override fun onMultimediaSelectResult(multimedia: List<Multimedia>) {
                Log.d(TAG, "multimedia: $multimedia")
                adapter?.multimedia = multimedia
            }

            override fun onMediaSelectResult(media: List<Media>) {
                Log.d(TAG, "media: $media")
                adapter?.multimedia = media
            }
        })
            .setTag(Bazaar.TAG)
            .setMode(mode)
            .setEventListener(object : EventListener {
                override fun onDestroy() {
                    Log.d(TAG, "onDestroy()")
                }
            })
            .setMaxSelectionCount(maxSelectionCount)
            .setCameraSettings(
                CameraSettings(
                    isPhotoShootEnabled = true,
                    isVideoCaptureEnabled = true
                )
            )
            .setLocalMediaSearchAndSelectEnabled(true)
            .setFoldersBasedInterfaceEnabled(true)
            .showSafely(supportFragmentManager)
    }

    /**
     * [ResultCallback] implementation
     */

    override fun onCameraResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter?.multimedia = listOf(media)
    }

    override fun onLocalMediaStoreResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter?.multimedia = listOf(media)
    }

    override fun onLocalMediaStoreResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter?.multimedia = media
    }

    override fun onMultimediaLocalMediaStoreResult(multimedia: Multimedia) {
        Log.d(TAG, "multimedia: $multimedia")
        adapter?.multimedia = listOf(multimedia)
    }

    override fun onMultimediaLocalMediaStoreResult(multimedia: List<Multimedia>) {
        Log.d(TAG, "multimedia: $multimedia")
        adapter?.multimedia = multimedia
    }

    override fun onMediaGallerySelectResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter?.multimedia = media
    }

    override fun onMultimediaGallerySelectResult(multimedia: List<Multimedia>) {
        Log.d(TAG, "multimedia: $multimedia")
        adapter?.multimedia = multimedia
    }

}