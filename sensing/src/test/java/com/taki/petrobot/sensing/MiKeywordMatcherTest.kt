package com.taki.petrobot.sensing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiKeywordMatcherTest {
    @Test
    fun shortAndFullNameVariantsMatch() {
        assertTrue(containsMiKeyword("ミー"))
        assertTrue(containsMiKeyword("みー"))
        assertTrue(containsMiKeyword("ミ"))
        assertTrue(containsMiKeyword("み"))
        assertTrue(containsMiKeyword("ミイ"))
        assertTrue(containsMiKeyword("みい"))
        assertTrue(containsMiKeyword("ねえ、ミー！"))
        assertTrue(containsMiKeyword("ミーニャ"))
        assertTrue(containsMiKeyword("みーにゃ"))
        assertTrue(containsMiKeyword("ねえ、ミーニャ！"))
    }

    @Test
    fun unrelatedSpeechDoesNotMatch() {
        assertFalse(containsMiKeyword("こんにちは"))
        assertFalse(containsMiKeyword("ミーティング"))
        assertFalse(containsMiKeyword("海を見よう"))
        assertFalse(containsMiKeyword("見て"))
        assertFalse(containsMiKeyword("みんな"))
        assertFalse(containsMiKeyword("おみくじ"))
    }

    @Test
    fun longerUnapprovedNamesDoNotAccidentallyMatch() {
        assertFalse(containsMiKeyword("みーちゃん"))
        assertFalse(containsMiKeyword("ミーニャちゃん"))
    }
}
