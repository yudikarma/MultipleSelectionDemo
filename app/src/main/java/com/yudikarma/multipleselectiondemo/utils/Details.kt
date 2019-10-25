package com.company107.zonapets.utils.mulitple_select_recycleview

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup

/**
 * Class for get data position from viewholder
 */
class Details : ItemDetailsLookup.ItemDetails<Long>() {

    var position:Long? = null
    override fun getSelectionKey(): Long? {
        return position?.toLong()
    }

    override fun getPosition(): Int {
        return position?.toInt() ?: 0
    }

    override fun inSelectionHotspot(e: MotionEvent): Boolean {
        return true
    }

}