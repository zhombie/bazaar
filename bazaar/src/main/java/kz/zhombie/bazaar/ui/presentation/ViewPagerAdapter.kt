package kz.zhombie.bazaar.ui.presentation

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

internal class ViewPagerAdapter constructor(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> GalleryFragment.newInstance()
            1 -> AlbumsFragment.newInstance()
            else -> throw IllegalStateException("createFragment($position)")
        }

}