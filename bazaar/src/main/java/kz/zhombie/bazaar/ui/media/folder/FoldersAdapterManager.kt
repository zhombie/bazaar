package kz.zhombie.bazaar.ui.media.folder

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.ui.components.recyclerview.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIFolder

internal class FoldersAdapterManager constructor(
    private val context: Context,
    private val recyclerView: RecyclerView,
) {

    companion object {
        private val TAG = FoldersAdapterManager::class.java.simpleName
    }

    private var foldersAdapter: FoldersAdapter? = null

    fun create(onFolderClicked: (uiFolder: UIFolder) -> Unit) {
        if (foldersAdapter == null) {
            foldersAdapter = FoldersAdapter(Settings.getImageLoader()) {
                onFolderClicked(it)
            }

            recyclerView.adapter = foldersAdapter

            val layoutManager = GridLayoutManager(
                context,
                2,
                GridLayoutManager.VERTICAL,
                false
            )

            recyclerView.layoutManager = layoutManager

            recyclerView.setHasFixedSize(true)

            recyclerView.itemAnimator = null

            recyclerView.addItemDecoration(
                SpacingItemDecoration(
                    spacingLeft = context.resources.getDimensionPixelOffset(R.dimen.folder_item_margin_left),
                    spacingTop = context.resources.getDimensionPixelOffset(R.dimen.folder_item_margin_top),
                    spacingRight = context.resources.getDimensionPixelOffset(R.dimen.folder_item_margin_right),
                    spacingBottom = context.resources.getDimensionPixelOffset(R.dimen.folder_item_margin_bottom)
                )
            )
        }
    }

    fun show() {
        recyclerView.visibility = View.VISIBLE
    }

    fun hide() {
        recyclerView.visibility = View.GONE
    }

    fun setPadding(
        extraPaddingLeft: Int = 0,
        extraPaddingTop: Int = 0,
        extraPaddingRight: Int = 0,
        extraPaddingBottom: Int = 0
    ) {
        recyclerView.setPadding(
            recyclerView.paddingLeft + extraPaddingLeft,
            recyclerView.paddingTop + extraPaddingTop,
            recyclerView.paddingRight + extraPaddingRight,
            recyclerView.paddingBottom + extraPaddingBottom
        )
    }

    fun submitList(folders: List<UIFolder>) {
        foldersAdapter?.submitList(folders)
    }

    fun destroy() {
        foldersAdapter = null
    }

}