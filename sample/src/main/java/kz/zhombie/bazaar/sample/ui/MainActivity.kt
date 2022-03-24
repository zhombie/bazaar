package kz.zhombie.bazaar.sample.ui

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
import kz.garage.multimedia.store.model.Content
import kz.garage.multimedia.store.model.Media
import kz.zhombie.bazaar.Bazaar
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.core.showSafely
import kz.zhombie.bazaar.api.event.EventListener
import kz.zhombie.bazaar.api.result.AbstractResultCallback
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.imageLoader
import kz.zhombie.bazaar.sample.App
import kz.zhombie.bazaar.sample.R
import kz.zhombie.bazaar.sample.loader.CoilImageLoader
import kz.zhombie.bazaar.sample.loader.GlideImageLoader
import kz.zhombie.bazaar.sample.ui.adapter.ContentsAdapter
import kz.zhombie.bazaar.utils.OpenFile
import kz.zhombie.bazaar.utils.open
import kz.zhombie.bazaar.utils.tryToOpen
import kz.zhombie.cinema.Cinema
import kz.zhombie.museum.Museum
import kz.zhombie.museum.paintingLoader

class MainActivity : AppCompatActivity(), ResultCallback {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private object RequestCode {
        const val EXTERNAL_STORAGE_ACCESS = 100
    }

    private val app: App?
        get() = (applicationContext as? App)

    private var GLIDE_IMAGE_LOADER: Pair<String, ImageLoader>? = null
    private var COIL_IMAGE_LOADER: Pair<String, ImageLoader>? = null

    private var IMAGE_LOADER: Pair<String, ImageLoader>? = null
        set(value) {
            field = value

            Log.d(TAG, "IMAGE_LOADER -> $value")

            if (value != null) {
                Museum.setPaintingLoader(value.second)
            }

            uiViewHolder?.imageLoaderView?.text = value?.first
        }

    private var uiViewHolder: UIViewHolder? = null

    private var adapter: ContentsAdapter? = null

    private var mode: Mode? = null
        set(value) {
            field = value

            if (value == null) {
                uiViewHolder?.modeView?.text = "ALL"
            } else {
                uiViewHolder?.modeView?.text = value.toString()
            }
        }

    private var maxSelectionCount: Int = 3
        set(value) {
            field = value
            uiViewHolder?.maxSelectionCountView?.text = value.toString()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uiViewHolder = UIViewHolder(this)

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

        adapter = null

        uiViewHolder = null

        // Cinema
        Cinema.clear()

        // Museum
        paintingLoader.clearCache()
        Museum.clear()

        // Bazaar
        imageLoader.clearCache()
        Bazaar.clear()
    }

    private fun setup() {
        // Image Loader declaration
        val imageLoaderInstance = app?.getImageLoader()

        Log.d(TAG, "setup() -> $imageLoaderInstance")

        var default: Pair<String, ImageLoader>? = null

        GLIDE_IMAGE_LOADER = if (imageLoaderInstance is GlideImageLoader) {
            val new = "Glide" to imageLoaderInstance
            default = new
            new
        } else {
            "Glide" to GlideImageLoader(this, this)
        }

        COIL_IMAGE_LOADER = if (imageLoaderInstance is CoilImageLoader) {
            val new = "Coil" to imageLoaderInstance
            default = new
            new
        } else {
            "Coil" to CoilImageLoader(this, Bazaar.isLoggingEnabled())
        }

        // Default Image Loader
        IMAGE_LOADER = default

        // Mode
        mode = Mode.IMAGE_AND_VIDEO

        // Max selection count
        maxSelectionCount = 3
    }

    private fun setupRecyclerView() {
        adapter = ContentsAdapter { content ->
            val file = content.publicFile?.getFile() ?: return@ContentsAdapter
            when (val action = file.open(this)) {
                is OpenFile.Success -> {
                    if (!action.tryToOpen(this)) {
                        Toast.makeText(this, "error_file_cannot_be_read", Toast.LENGTH_SHORT).show()
                    }
                }
                is OpenFile.Error -> {
                    when (action) {
                        is OpenFile.Error.Unknown -> {
                            Toast.makeText(this, "error_file_cannot_be_read", Toast.LENGTH_SHORT).show()
                        }
                        is OpenFile.Error.FileDoesNotExist -> {
                            Toast.makeText(this, "not_found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        uiViewHolder?.recyclerView?.adapter = adapter
    }

    private fun setupImageLoaderButton() {
        uiViewHolder?.imageLoaderButton?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Image loader")
                .setSingleChoiceItems(
                    arrayOf(COIL_IMAGE_LOADER?.first, GLIDE_IMAGE_LOADER?.first),
                    -1
                ) { dialog, which ->
                    dialog.dismiss()
                    IMAGE_LOADER = when (which) {
                        0 -> COIL_IMAGE_LOADER
                        1 -> GLIDE_IMAGE_LOADER
                        else -> COIL_IMAGE_LOADER
                    }
                }
                .show()
        }
    }

    private fun setupModeButton() {
        uiViewHolder?.modeButton?.setOnClickListener {
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
        uiViewHolder?.maxSelectionCountButton?.setOnClickListener {
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
        uiViewHolder?.showButton?.setOnClickListener {
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
            override fun onSelectResult(contents: List<Content>) {
                Log.d(TAG, "contents: $contents")
                adapter?.contents = contents
            }
        })
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
        adapter?.contents = listOf(media)
    }

    override fun onMediaResult(media: Media) {
        Log.d(TAG, "media: $media")
        adapter?.contents = listOf(media)
    }

    override fun onMediaResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter?.contents = media
    }

    override fun onContentResult(content: Content) {
        Log.d(TAG, "content: $content")
        adapter?.contents = listOf(content)
    }

    override fun onContentsResult(contents: List<Content>) {
        Log.d(TAG, "contents: $contents")
        adapter?.contents = contents
    }

    override fun onGalleryMediaResult(media: List<Media>) {
        Log.d(TAG, "media: $media")
        adapter?.contents = media
    }

    override fun onGalleryContentsResult(contents: List<Content>) {
        Log.d(TAG, "contents: $contents")
        adapter?.contents = contents
    }

}