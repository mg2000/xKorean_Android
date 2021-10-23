package com.mg2000.xkorean

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModel(intentRepo: IntentRepo) : ViewModel() {
    val intent = intentRepo

    class Factory(val intentRepo: IntentRepo) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(intentRepo) as T
        }

    }
}