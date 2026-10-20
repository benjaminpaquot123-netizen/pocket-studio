package com.pocketstudio.app.audio

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Runs [block] on [context] to its end, even if the caller is cancelled meanwhile, and returns its
 * result. A result the caller can no longer take (cancelled while [block] ran: withContext then
 * throws instead of returning) goes to [release], so that native memory is never lost on the way.
 */
suspend fun <T : Any> finishOrRelease(context: CoroutineContext, release: (T) -> Unit, block: suspend () -> T?): T? {
    var result: T? = null
    try {
        withContext(context + NonCancellable) { result = block() }
    } catch (e: CancellationException) {
        result?.let(release)
        throw e
    }
    return result
}
