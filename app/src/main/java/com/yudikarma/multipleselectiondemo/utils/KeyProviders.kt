package com.company107.zonapets.utils.mulitple_select_recycleview

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView

/**
 * providers for key key from selection item from viewholer
 */
class KeyProviders(recycler_facility: RecyclerView) : ItemKeyProvider<Long>(ItemKeyProvider.SCOPE_MAPPED) {
    override fun getKey(position: Int): Long? {
        return position.toLong()
    }

    override fun getPosition(key: Long): Int {
        var value :Long  = key
        return value.toInt()
    }

}