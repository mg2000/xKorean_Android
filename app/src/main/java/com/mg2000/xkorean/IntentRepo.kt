package com.mg2000.xkorean

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

class IntentRepo {
    private val _intent = MutableLiveData<Intent>()

    val get: LiveData<Intent> = Transformations.map(_intent) { it!! }

    fun set(intent: Intent) { _intent.value = intent }
}