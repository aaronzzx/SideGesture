package com.aaron.sidegesture.feature.gesture

import kotlin.math.pow

class DoubleTapStateMachine<T : Any>(
    val timeoutMillis: Long,
    doubleTapSlop: Float
) {

    private val doubleTapSlopSquared = doubleTapSlop.pow(2)
    private var nextGeneration = 0L
    private var pending: Pending<T>? = null

    init {
        require(timeoutMillis >= 0L)
        require(doubleTapSlop >= 0f)
    }

    val hasPending: Boolean
        get() = pending != null

    fun begin(
        buttonKey: String,
        downX: Float,
        downY: Float,
        upTimeMillis: Long,
        singleTap: T,
        doubleTap: T
    ): TimeoutToken {
        val generation = ++nextGeneration
        pending = Pending(
            generation = generation,
            buttonKey = buttonKey,
            downX = downX,
            downY = downY,
            upTimeMillis = upTimeMillis,
            singleTap = singleTap,
            doubleTap = doubleTap
        )
        return TimeoutToken(generation)
    }

    fun onDown(
        buttonKey: String?,
        downX: Float,
        downY: Float,
        downTimeMillis: Long
    ): DownResult<T> {
        val pending = pending ?: return DownResult(DownResolution.NoPending)
        val elapsedMillis = downTimeMillis - pending.upTimeMillis
        if (elapsedMillis > timeoutMillis) {
            clear()
            return DownResult(
                resolution = DownResolution.Expired,
                expiredSingleTap = pending.singleTap
            )
        }
        val distanceSquared = (downX - pending.downX).pow(2) +
            (downY - pending.downY).pow(2)
        val matches = elapsedMillis >= 0L &&
            buttonKey == pending.buttonKey &&
            distanceSquared <= doubleTapSlopSquared
        if (!matches) {
            clear()
            return DownResult(DownResolution.Rejected)
        }
        pending.secondDownMatched = true
        return DownResult(DownResolution.Matched)
    }

    fun completeSecondTap(upTimeMillis: Long): T? {
        val pending = pending ?: return null
        if (!pending.secondDownMatched) return null
        val elapsedMillis = upTimeMillis - pending.upTimeMillis
        val result = if (elapsedMillis in 0L..timeoutMillis) {
            pending.doubleTap
        } else {
            null
        }
        clear()
        return result
    }

    fun consumeTimeout(token: TimeoutToken): T? {
        val pending = pending ?: return null
        if (pending.generation != token.generation || pending.secondDownMatched) {
            return null
        }
        clear()
        return pending.singleTap
    }

    fun cancel() {
        clear()
    }

    private fun clear() {
        pending = null
    }

    enum class DownResolution {
        NoPending,
        Matched,
        Rejected,
        Expired
    }

    data class DownResult<T : Any>(
        val resolution: DownResolution,
        val expiredSingleTap: T? = null
    )

    data class TimeoutToken(
        val generation: Long
    )

    private data class Pending<T : Any>(
        val generation: Long,
        val buttonKey: String,
        val downX: Float,
        val downY: Float,
        val upTimeMillis: Long,
        val singleTap: T,
        val doubleTap: T,
        var secondDownMatched: Boolean = false
    )
}
