package com.mg2000.xkorean.ui.transform

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TransformViewModel : ViewModel() {
    var filteredGames: MutableLiveData<List<Game>> = MutableLiveData()
    var totalGames: MutableLiveData<List<Game>> = MutableLiveData()

    var gameList: List<Game>?
        get() = totalGames.value
        set(gameList) {
            totalGames.value = gameList
        }

    fun update(gameList: List<Game>) {
        filteredGames.value = gameList
    }
}