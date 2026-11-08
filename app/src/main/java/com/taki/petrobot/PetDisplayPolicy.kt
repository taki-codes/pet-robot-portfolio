package com.taki.petrobot

data class PetDisplayPolicy(
    val keepScreenOn: Boolean,
    val hideStatusBar: Boolean,
    val hideNavigationBar: Boolean,
    val allowTransientSystemBarsBySwipe: Boolean,
    val reapplyWhenWindowRegainsFocus: Boolean,
)

val DEDICATED_PET_DISPLAY_POLICY = PetDisplayPolicy(
    keepScreenOn = true,
    hideStatusBar = true,
    hideNavigationBar = true,
    allowTransientSystemBarsBySwipe = true,
    reapplyWhenWindowRegainsFocus = true,
)
