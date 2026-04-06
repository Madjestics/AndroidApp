package com.example.movieandroid.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object RefreshBus {
    private val mutableEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<Unit> = mutableEvents

    fun requestRefresh() {
        mutableEvents.tryEmit(Unit)
    }
}
