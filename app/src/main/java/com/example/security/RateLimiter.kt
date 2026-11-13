package com.example.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RateLimitSnapshot(
    val requestsInLastMinute: Int,
    val maxPerMinute: Int,
    val totalRequestsThisSession: Int,
    val cooldownRemainingSeconds: Int
)

/**
 * Client-side sliding-window rate limiter for abuse prevention and cost protection.
 */
class RateLimiter(
    private val maxRequestsPerMinute: Int = 12,
    private val windowMillis: Long = 60_000L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val timestamps = ArrayDeque<Long>()
    private var totalSessionRequests = 0

    private val _snapshot = MutableStateFlow(
        RateLimitSnapshot(
            requestsInLastMinute = 0,
            maxPerMinute = maxRequestsPerMinute,
            totalRequestsThisSession = 0,
            cooldownRemainingSeconds = 0
        )
    )
    val snapshot: StateFlow<RateLimitSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    fun tryAcquire(): Result<Unit> {
        val now = timeProvider()
        pruneOld(now)
        if (timestamps.size >= maxRequestsPerMinute) {
            val oldest = timestamps.first()
            val waitMillis = (oldest + windowMillis - now).coerceAtLeast(1000L)
            val waitSec = ((waitMillis + 999L) / 1000L).toInt()
            updateSnapshot(now, waitSec)
            return Result.failure(
                IllegalStateException(
                    "Rate limit active ($maxRequestsPerMinute requests/min) for abuse prevention. Please wait ${waitSec}s."
                )
            )
        }
        timestamps.addLast(now)
        totalSessionRequests++
        updateSnapshot(now, 0)
        return Result.success(Unit)
    }

    @Synchronized
    fun refreshStatus() {
        val now = timeProvider()
        pruneOld(now)
        val waitSec = if (timestamps.size >= maxRequestsPerMinute && timestamps.isNotEmpty()) {
            val waitMillis = (timestamps.first() + windowMillis - now).coerceAtLeast(0L)
            ((waitMillis + 999L) / 1000L).toInt()
        } else 0
        updateSnapshot(now, waitSec)
    }

    private fun pruneOld(now: Long) {
        while (timestamps.isNotEmpty() && now - timestamps.first() >= windowMillis) {
            timestamps.removeFirst()
        }
    }

    private fun updateSnapshot(now: Long, cooldownSec: Int) {
        pruneOld(now)
        _snapshot.value = RateLimitSnapshot(
            requestsInLastMinute = timestamps.size,
            maxPerMinute = maxRequestsPerMinute,
            totalRequestsThisSession = totalSessionRequests,
            cooldownRemainingSeconds = cooldownSec
        )
    }
}
