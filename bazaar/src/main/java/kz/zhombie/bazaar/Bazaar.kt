package kz.zhombie.bazaar

import androidx.fragment.app.FragmentManager
import kz.zhombie.bazaar.api.ImageLoader
import kz.zhombie.bazaar.api.ResultCallback
import kz.zhombie.bazaar.ui.media.MediaStoreFragment

class Bazaar {

    companion object {
        val TAG: String = Bazaar::class.java.simpleName
    }

    class Builder constructor(private val resultCallback: ResultCallback? = null) {

        private var tag: String? = null
        private var imageLoader: ImageLoader? = null

        fun setTag(tag: String): Builder {
            this.tag = tag
            return this
        }

        fun setImageLoader(imageLoader: ImageLoader): Builder {
            this.imageLoader = imageLoader
            return this
        }

        fun show(fragmentManager: FragmentManager): String? {
            imageLoader?.let { Settings.setImageLoader(it) }

            val fragment = MediaStoreFragment.newInstance()
            if (resultCallback != null) {
                fragment.setResultCallback(resultCallback)
            }
            fragment.show(fragmentManager, tag)
            return tag
        }
    }

}