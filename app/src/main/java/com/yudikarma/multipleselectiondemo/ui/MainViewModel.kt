package com.yudikarma.multipleselectiondemo.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yudikarma.multipleselectiondemo.model.GaleryFragmentModel

class MainViewModel  : ViewModel() {
    var selectedFile = MutableLiveData<GaleryFragmentModel>()
    var selectedPosition = MutableLiveData<Int>()
    var isSupportMultipleSelect = MutableLiveData<Boolean>()
}