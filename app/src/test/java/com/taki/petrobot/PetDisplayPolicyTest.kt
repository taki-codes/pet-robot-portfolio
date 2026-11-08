package com.taki.petrobot

import org.junit.Assert.assertTrue
import org.junit.Test

class PetDisplayPolicyTest {
    @Test
    fun dedicatedPetDisplayIsImmersiveButStillRecoverable() {
        val policy = DEDICATED_PET_DISPLAY_POLICY

        assertTrue(policy.keepScreenOn)
        assertTrue(policy.hideStatusBar)
        assertTrue(policy.hideNavigationBar)
        assertTrue(policy.allowTransientSystemBarsBySwipe)
        assertTrue(policy.reapplyWhenWindowRegainsFocus)
    }
}
