package com.parhar.noor.utils

sealed interface UIState<out T> {
    data object Idle : UIState<Nothing>

    data object Loading : UIState<Nothing>

    data class Success<out T>(val data: T) : UIState<T>

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : UIState<Nothing>
}
