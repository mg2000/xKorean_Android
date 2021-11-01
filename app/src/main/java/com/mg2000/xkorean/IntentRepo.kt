package com.mg2000.xkorean

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

class IntentRepo {
    private val _intent = MutableLiveData<String>()

    val get: LiveData<String> = Transformations.map(_intent) { it!! }

    fun set(keyword: String) { _intent.value = keyword }
}