package kz.zhombie.bazaar.core

import androidx.recyclerview.widget.RecyclerView

class ViewHolderException constructor(private val viewType: Int) : IllegalStateException() {

    override val message: String
        get() = "There is no [${RecyclerView.ViewHolder::class.java.simpleName}] for this [${viewType}]."

}