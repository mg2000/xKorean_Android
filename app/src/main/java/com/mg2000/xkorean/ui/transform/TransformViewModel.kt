package com.mg2000.xkorean.ui.transform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TransformViewModel : ViewModel() {
    var games: MutableLiveData<List<Game>> = MutableLiveData()

    fun update(gameList: List<Game>) {
        games.value = gameList
    }
}