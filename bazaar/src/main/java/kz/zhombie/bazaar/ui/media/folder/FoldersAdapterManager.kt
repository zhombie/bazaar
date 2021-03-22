package kz.zhombie.bazaar.ui.media.folder

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.ui.components.recyclerview.decoration.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIFolder

internal class FoldersAdapterManager constructor(
    private val context: Context,
    private val recyclerView: RecyclerView,
) {

    companion object {
        private val TAG = FoldersAdapterManager::class.java.simpleName
    }

    enum class Type {
        LIST,
        GRID
    }

    private var foldersAdapter: FoldersAdapter? = null

    private var itemDecoration: SpacingItemDecoration? = null

    fun create(
        type: Type,
        isCoverEnabled: Boolean,
        onFolderClicked: (uiFolder: UIFolder) -> Unit
    ) {
        if (foldersAdapter == null) {
            foldersAdapter = FoldersAdapter(
                imageLoader = Settings.getImageLoader(),
                type = if (type == Type.LIST) {
                    FoldersAdapter.Type.RECTANGLE
                } else {
                    FoldersAdapter.Type.SQUARE
                },
                isCoverEnabled = isCoverEnabled,
                onFolderClicked = { onFolderClicked(it) },
                onLeftOffsetReadyListener = { leftOffset ->
                    itemDecoration?.decoratorLeftOffset = leftOffset
                    recyclerView.invalidateItemDecorations()
                }
            )

            recyclerView.adapter = foldersAdapter

            val layoutManager: RecyclerView.LayoutManager
            val itemDecoration: SpacingItemDecoration

            when (type) {
                Type.LIST -> {
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

                    itemDecoration = SpacingItemDecoration(
                        isDecoratorEnabled = true,
                        decoratorColor = ContextCompat.getColor(context, R.color.gray),
                        decoratorWidth = context.resources.getDimension(R.dimen.folder_item_list_decorator_width),
                        isFirstItemDecoratorEnabled = true,
                        isLastItemDecoratorEnabled = false,
                        spacingLeft = context.resources.getDimensionPixelOffset(R.dimen.folder_item_list_margin_left),
                        spacingTop = context.resources.getDimensionPixelOffset(R.dimen.folder_item_list_margin_top),
                        spacingRight = context.resources.getDimensionPixelOffset(R.dimen.folder_item_list_margin_right),
                        spacingBottom = context.resources.getDimensionPixelOffset(R.dimen.folder_item_list_margin_bottom)
                    )
                }
                Type.GRID -> {
                    layoutManager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)

                    itemDecoration = SpacingItemDecoration(
                        spacingLeft = context.resources.getDimensionPixelOffset(R.dimen.folder_item_grid_margin_left),
                        spacingTop = context.resources.getDimensionPixelOffset(R.dimen.folder_item_grid_margin_top),
                        spacingRight = context.resources.getDimensionPixelOffset(R.dimen.folder_item_grid_margin_right),
                        spacingBottom = context.resources.getDimensionPixelOffset(R.dimen.folder_item_grid_margin_bottom)
                    )
                }
            }

            recyclerView.layoutManager = layoutManager

            recyclerView.setHasFixedSize(true)

            recyclerView.itemAnimator = null

            recyclerView.addItemDecoration(itemDecoration)
            this.itemDecoration = itemDecoration
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