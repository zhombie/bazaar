package kz.zhombie.bazaar

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.exception.ImageLoaderNullException
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.core.worker.MediaSyncWorker
import kz.zhombie.bazaar.api.event.EventListener
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.core.media.MediaScanManager
import kz.zhombie.bazaar.ui.presentation.MediaStoreFragment
import kz.zhombie.bazaar.ui.presentation.MediaStoreScreen

object Bazaar {
    const val TAG = "Bazaar"

    /**
     * Settings
     */

    data class Configuration constructor(
        val isLoggingEnabled: Boolean
    )

    interface Factory {
        fun getBazaarConfiguration(): Configuration
    }

    private var configuration: Configuration? = null
    private var configurationFactory: Factory? = null

    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: ImageLoader.Factory? = null

    fun isLoggingEnabled(): Boolean = configuration?.isLoggingEnabled ?: false

    @Synchronized
    fun getImageLoader(context: Context?): ImageLoader =
        imageLoader ?: setImageLoaderFactory(context)

    @Synchronized
    fun setImageLoader(loader: ImageLoader) {
        imageLoaderFactory = null
        imageLoader = loader
    }

    @Synchronized
    fun setImageLoader(factory: ImageLoader.Factory) {
        imageLoaderFactory = factory
        imageLoader = null
    }

    @Synchronized
    fun setImageLoaderFactory(context: Context?): ImageLoader {
        imageLoader?.let { return it }

        imageLoader = imageLoaderFactory?.getImageLoader()
            ?: (context?.applicationContext as? ImageLoader.Factory)?.getImageLoader()

        Logger.debug(TAG, "setImageLoaderFactory() -> $imageLoader")

        imageLoaderFactory = null

        return imageLoader ?: throw ImageLoaderNullException()
    }

    @Synchronized
    fun getConfiguration(context: Context?): Configuration =
        configuration ?: setConfigurationFactory(context)

    @Synchronized
    fun setConfiguration(configuration: Configuration?) {
        configurationFactory = null
        this.configuration = configuration
    }

    @Synchronized
    fun setConfiguration(factory: Factory) {
        configuration = null
        configurationFactory = factory
    }

    @Synchronized
    fun setConfigurationFactory(context: Context?): Configuration {
        configuration?.let { return it }

        configuration = configurationFactory?.getBazaarConfiguration()
            ?: (context?.applicationContext as? Factory)?.getBazaarConfiguration()
            ?: Configuration(false)

        Logger.debug(TAG, "setConfigurationFactory() -> $configuration")

        configurationFactory = null

        return requireNotNull(configuration)
    }

    @Synchronized
    fun clear() {
        Logger.debug(TAG, "clear()")

        configuration = null
        configurationFactory = null

        imageLoader = null
        imageLoaderFactory = null
    }

    /**
     * Handy bridge-based methods
     */

    suspend fun sync(context: Context): Boolean =
        MediaSyncWorker.startWork(context)

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    suspend fun preload(context: Context, mode: Mode) {
        MediaScanManager.preload(context, mode)
    }

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    suspend fun preloadAll(context: Context) {
        MediaScanManager.preload(context, Mode.IMAGE_AND_VIDEO)
        MediaScanManager.preload(context, Mode.AUDIO)
        MediaScanManager.preload(context, Mode.DOCUMENT)
    }

    suspend fun clearCache(): Boolean =
        MediaScanManager.clearCache()

    suspend fun destroyCache() {
        MediaScanManager.destroyCache()
    }

    /**
     * Pre-selection
     */

    fun selectMode(
        context: Context,
        defaultMode: Mode = Mode.IMAGE,
        amongModes: List<Mode> = listOf(Mode.IMAGE, Mode.VIDEO, Mode.AUDIO, Mode.DOCUMENT),
        callback: (mode: Mode) -> Unit
    ): AlertDialog? {
        val items = mutableListOf<String>()

        if (Mode.IMAGE in amongModes) {
            items.add(context.getString(R.string.bazaar_image))
        }
        if (Mode.VIDEO in amongModes) {
            items.add(context.getString(R.string.bazaar_video))
        }
        if (Mode.AUDIO in amongModes) {
            items.add(context.getString(R.string.bazaar_audio))
        }
        if (Mode.DOCUMENT in amongModes) {
            items.add(context.getString(R.string.bazaar_document))
        }

        var checkedItem = amongModes.indexOf(defaultMode)
        if (checkedItem < 0) {
            checkedItem = 0
        }

        return MaterialAlertDialogBuilder(context, R.style.Bazaar_AlertDialogTheme)
            .setTitle(context.getString(R.string.bazaar_mode_selection))
            .setSingleChoiceItems(items.toTypedArray(), checkedItem) { dialog, which ->
                dialog.dismiss()

                try {
                    callback(amongModes[which])
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton(R.string.bazaar_close) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Builder pattern
     */

    class Builder constructor(private var resultCallback: ResultCallback? = null) {

        private var tag: String? = null
        private var imageLoader: ImageLoader? = null

        private var mode: Mode = Mode.IMAGE
        private var maxSelectionCount: Int = 1
        private var cameraSettings: CameraSettings = CameraSettings(
            isPhotoShootEnabled = false,
            isVideoCaptureEnabled = false
        )
        private var isLocalMediaSearchAndSelectEnabled: Boolean = false
        private var isFolderBasedInterfaceEnabled: Boolean = false
        private var eventListener: EventListener? = null

        fun setTag(tag: String): Builder {
            this.tag = tag
            return this
        }

        fun setImageLoader(imageLoader: ImageLoader): Builder {
            this.imageLoader = imageLoader
            return this
        }

        fun setMode(mode: Mode): Builder {
            this.mode = mode
            return this
        }

        fun setSingleSelection(): Builder {
            this.maxSelectionCount = 1
            return this
        }

        fun setMaxSelectionCount(count: Int): Builder {
            this.maxSelectionCount = count
            return this
        }

        fun setCameraDisabled(): Builder {
            this.cameraSettings = CameraSettings(isPhotoShootEnabled = false, isVideoCaptureEnabled = false)
            return this
        }

        fun setCameraSettings(settings: CameraSettings): Builder {
            if (mode == Mode.IMAGE || mode == Mode.VIDEO || mode == Mode.IMAGE_AND_VIDEO) {
                this.cameraSettings = settings
            } else {
                this.cameraSettings = CameraSettings(isPhotoShootEnabled = false, isVideoCaptureEnabled = false)
            }
            return this
        }

        fun setLocalMediaSearchAndSelectEnabled(isEnabled: Boolean): Builder {
            this.isLocalMediaSearchAndSelectEnabled = isEnabled
            return this
        }

        fun setFoldersBasedInterfaceEnabled(isEnabled: Boolean): Builder {
            this.isFolderBasedInterfaceEnabled = isEnabled
            return this
        }

        fun setEventListener(listener: EventListener): Builder {
            this.eventListener = listener
            return this
        }

        fun setResultCallback(callback: ResultCallback): Builder {
            this.resultCallback = callback
            return this
        }

        fun show(fragmentManager: FragmentManager): BottomSheetDialogFragment {
            require(maxSelectionCount in 1..10) { "Max selection count MUST be between 1 & 10" }

            val fragment = MediaStoreFragment.newInstance(
                MediaStoreScreen.Settings(
                    mode = mode,
                    maxSelectionCount = maxSelectionCount,
                    cameraSettings = cameraSettings,
                    isLocalMediaSearchAndSelectEnabled = isLocalMediaSearchAndSelectEnabled,
                    isFolderBasedInterfaceEnabled = isFolderBasedInterfaceEnabled
                )
            )
            eventListener?.let { fragment.setEventListener(it) }
            resultCallback?.let { fragment.setResultCallback(it) }
            fragment.show(fragmentManager, tag)
            return fragment
        }
    }

}