package kz.zhombie.bazaar.ui.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kz.zhombie.bazaar.R

internal class AlbumsFragment : Fragment(R.layout.bazaar_fragment_albums) {

    companion object {
        private val TAG: String = AlbumsFragment::class.java.simpleName

        fun newInstance(): AlbumsFragment {
            val fragment = AlbumsFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    private val viewModel by activityViewModels<MediaSelectionViewModel>()

    private var recyclerView: RecyclerView? = null

    private var albumsAdapter: AlbumsAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)

        setupRecyclerView()

        observe()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        albumsAdapter = null
    }

    private fun setupRecyclerView() {
        recyclerView?.layoutManager = GridLayoutManager(
            requireContext(),
            2,
            GridLayoutManager.VERTICAL,
            false
        )

        albumsAdapter = AlbumsAdapter()

        recyclerView?.adapter = albumsAdapter
    }

    private fun observe() {
        lifecycleScope.launch {
            viewModel.contents.collectLatest { pagingData ->
                albumsAdapter?.submitData(pagingData)
            }
        }
    }

}