package org.basnalcorp.indiecomicapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _greetingText = MutableLiveData<String>().apply {
        value = "Hello reader"
    }
    val greetingText: LiveData<String> = _greetingText

    fun updateGreeting(userName: String?) {
        _greetingText.value = if (userName != null) {
            "Hello $userName"
        } else {
            "Hello reader"
        }
    }
}