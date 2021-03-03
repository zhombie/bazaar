package kz.zhombie.bazaar

import androidx.fragment.app.FragmentManager

class Bazaar {

    companion object {
        val TAG: String = Bazaar::class.java.simpleName
    }

    class Builder constructor(private val fragmentManager: FragmentManager) {

        private var tag: String? = null

        fun setTag(tag: String): Builder {
            this.tag = tag
            return this
        }

        fun show(): String? {
            val fragment = MediaStoreFragment.newInstance()
            fragment.show(fragmentManager, tag)
            return tag
        }
    }

}