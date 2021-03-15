package kz.zhombie.bazaar

import androidx.fragment.app.FragmentManager
import kz.zhombie.bazaar.api.core.settings.CameraSettings
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.exception.ImageLoaderNullException
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.event.EventListener
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.ui.media.MediaStoreFragment
import kz.zhombie.bazaar.ui.media.MediaStoreScreen

class Bazaar private constructor() {

    companion object {
        val TAG: String = Bazaar::class.java.simpleName
    }

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

        fun setMaxSelectionCount(count: Int): Builder {
            this.maxSelectionCount = count
            return this
        }

        fun setCameraSettings(settings: CameraSettings): Builder {
            this.cameraSettings = settings
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

        fun show(fragmentManager: FragmentManager): String? {
            Settings.setImageLoader(requireNotNull(imageLoader) { ImageLoaderNullException() })

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
            fragment.setResultCallback(requireNotNull(resultCallback) { "It makes no sense without a resultant callback." })
            fragment.show(fragmentManager, tag)
            return tag
        }
    }

}