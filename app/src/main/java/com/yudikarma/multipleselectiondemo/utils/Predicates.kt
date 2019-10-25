package com.company107.zonapets.utils.mulitple_select_recycleview

import androidx.recyclerview.selection.SelectionTracker

/**
 * Configuration for tracker selection
 */
class Predicates : SelectionTracker.SelectionPredicate<Long>(){
    override fun canSelectMultiple(): Boolean {
        return true
    }

    override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean {
        return true
    }

    override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean {
        return true
    }

}