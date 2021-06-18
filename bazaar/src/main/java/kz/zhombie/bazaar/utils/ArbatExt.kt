package kz.zhombie.bazaar.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.museum.MuseumDialogFragment

const val TAG = "ArbatDialogFragment"

@Suppress("unused")
fun MuseumDialogFragment.Builder.dismissPrevious(fragmentManager: FragmentManager): Fragment? {
    val fragment = fragmentManager.findFragmentByTag(TAG)
    if (fragment is MuseumDialogFragment) {
        fragment.dismiss()
    }
    return fragment
}

fun MuseumDialogFragment.Builder.showSafely(fragmentManager: FragmentManager): MuseumDialogFragment {
    dismissPrevious(fragmentManager)
    setTag(TAG)
    return show(fragmentManager)
}


@Suppress("unused")
fun CinemaDialogFragment.Builder.dismissPrevious(fragmentManager: FragmentManager): Fragment? {
    val fragment = fragmentManager.findFragmentByTag(TAG)
    if (fragment is CinemaDialogFragment) {
        fragment.dismiss()
    }
    return fragment
}

fun CinemaDialogFragment.Builder.showSafely(fragmentManager: FragmentManager): CinemaDialogFragment {
    dismissPrevious(fragmentManager)
    setTag(TAG)
    return show(fragmentManager)
}