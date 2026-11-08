package com.taki.petrobot.storage

import android.content.Context

object StorageModule {
    fun openStructuredLog(
        context: Context,
        onStored: (StructuredEventType, Long) -> Unit = { _, _ -> },
        onError: (Throwable) -> Unit = {},
    ): StructuredLogStore = StructuredLogStore(
        context = context,
        onStored = onStored,
        onError = onError,
    )
}
