package com.mg2000.xkorean.ui.transform

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EditionViewModel : ViewModel() {
    var editions: MutableLiveData<List<Edition>> = MutableLiveData()

    fun update(editionList: List<Edition>) {
        editions.value = editionList
    }
}