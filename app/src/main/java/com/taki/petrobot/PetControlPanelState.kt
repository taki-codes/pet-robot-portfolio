package com.taki.petrobot

internal enum class PetControlPanelEvent {
    OPEN,
    CLOSE,
    ACTION_COMPLETED,
}

internal fun reducePetControlPanelVisibility(
    isVisible: Boolean,
    event: PetControlPanelEvent,
): Boolean = when (event) {
    PetControlPanelEvent.OPEN -> true
    PetControlPanelEvent.CLOSE,
    PetControlPanelEvent.ACTION_COMPLETED,
    -> false
}
