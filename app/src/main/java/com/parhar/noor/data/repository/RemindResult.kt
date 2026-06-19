package com.parhar.noor.data.repository

sealed class RemindResult {
    data object Sent : RemindResult()
    data object Offline : RemindResult()
    data object PendingUnseen : RemindResult()
    data class Failed(val message: String) : RemindResult()
}
