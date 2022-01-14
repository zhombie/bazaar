package kz.zhombie.bazaar.ui.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kz.zhombie.bazaar.Bazaar
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.core.logging.Logger

internal class MediaSelectionFragment : Fragment() {

    companion object {
        private val TAG: String = MediaSelectionFragment::class.java.simpleName

        fun newInstance(settings: MediaStoreScreen.Settings): MediaSelectionFragment {
            val fragment = MediaSelectionFragment()
            val bundle = Bundle()
            bundle.putSerializable("settings", settings)
            fragment.arguments = bundle
            return fragment
        }
    }

    private val viewModel by viewModels<MediaSelectionViewModel>()

    private var tabLayout: TabLayout? = null
    private var viewPager: ViewPager2? = null

    private var tabLayoutMediator: TabLayoutMediator? = null

    private var viewPagerAdapter: ViewPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configuration = Bazaar.getConfiguration(requireContext())
        Logger.debug(TAG, "configuration: $configuration")

        val imageLoader = Bazaar.getImageLoader(requireContext())
        Logger.debug(TAG, "imageLoader: $imageLoader")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bazaar_fragment, container, false)

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val dialog = super.onCreateDialog(savedInstanceState)
//        if (dialog is BottomSheetDialog) {
//            dialog.behavior.isDraggable = true
//            dialog.behavior.isFitToContents = true
//            dialog.behavior.halfExpandedRatio = 0.5F
////            dialog.setOnShowListener {
////                dialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
////            }
//        }
//        return dialog
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        setupViewPager()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewPagerAdapter = null

        tabLayoutMediator?.detach()
        tabLayoutMediator = null
    }

    fun show(fragmentManager: FragmentManager, tag: String?): Int =
        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(this, tag)
            .commit()

    private fun setupViewPager() {
        viewPager?.isUserInputEnabled = true
        viewPager?.offscreenPageLimit = 2

        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager?.adapter = viewPagerAdapter

        if (tabLayoutMediator == null) {
            tabLayoutMediator = TabLayoutMediator(
                requireNotNull(tabLayout),
                requireNotNull(viewPager),
                true,
                true
            ) { _, _ ->
            }
        }

        if (tabLayoutMediator?.isAttached == false) {
            tabLayoutMediator?.attach()
        }
    }

}