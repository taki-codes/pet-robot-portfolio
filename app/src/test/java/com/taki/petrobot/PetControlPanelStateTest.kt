package com.taki.petrobot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetControlPanelStateTest {
    @Test
    fun open_showsControls() {
        assertTrue(
            reducePetControlPanelVisibility(
                isVisible = false,
                event = PetControlPanelEvent.OPEN,
            ),
        )
    }

    @Test
    fun close_hidesControls() {
        assertFalse(
            reducePetControlPanelVisibility(
                isVisible = true,
                event = PetControlPanelEvent.CLOSE,
            ),
        )
    }

    @Test
    fun completedAction_hidesControls() {
        assertFalse(
            reducePetControlPanelVisibility(
                isVisible = true,
                event = PetControlPanelEvent.ACTION_COMPLETED,
            ),
        )
    }

    @Test
    fun repeatedOpen_keepsControlsVisible() {
        assertTrue(
            reducePetControlPanelVisibility(
                isVisible = true,
                event = PetControlPanelEvent.OPEN,
            ),
        )
    }
}
