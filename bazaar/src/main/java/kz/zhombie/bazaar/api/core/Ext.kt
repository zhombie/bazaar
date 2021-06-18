package kz.zhombie.bazaar.api.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kz.zhombie.bazaar.Bazaar

@Suppress("unused")
fun Bazaar.Builder.dismissPrevious(fragmentManager: FragmentManager): Fragment? {
    val fragment = fragmentManager.findFragmentByTag(Bazaar.TAG)
    if (fragment is BottomSheetDialogFragment) {
        fragment.dismiss()
    }
    return fragment
}

fun Bazaar.Builder.showSafely(fragmentManager: FragmentManager): BottomSheetDialogFragment {
    dismissPrevious(fragmentManager)
    setTag(Bazaar.TAG)
    return show(fragmentManager)
}