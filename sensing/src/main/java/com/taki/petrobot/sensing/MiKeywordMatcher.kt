package com.taki.petrobot.sensing

import java.text.Normalizer

private val MI_KEYWORD_PATTERN =
    Regex("""(^|[^\p{L}\p{N}])(?:ミーニャ|ミー|ミイ|ミ)($|[^\p{L}\p{N}])""")

/**
 * Reduces a transient Japanese recognition hypothesis to the allowed short or full pet name.
 * Short exact aliases cover common on-device transcriptions of the one-mora call “ミー”.
 * Callers must discard [hypothesis] immediately after this function returns.
 */
fun containsMiKeyword(hypothesis: String): Boolean {
    val normalized = Normalizer.normalize(hypothesis, Normalizer.Form.NFKC)
        .replace("みーにゃ", "ミーニャ")
        .replace("みぃにゃ", "ミーニャ")
        .replace("ミーにゃ", "ミーニャ")
        .replace("ミィニャ", "ミーニャ")
        .replace("みー", "ミー")
        .replace("みぃ", "ミー")
        .replace("ミィ", "ミー")
        .replace("みい", "ミイ")
        .replace("み", "ミ")
        .trim()
    return MI_KEYWORD_PATTERN.containsMatchIn(normalized)
}
