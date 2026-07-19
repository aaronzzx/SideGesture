package com.aaron.sidegesture.feature.quicktools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class QuickToolsBrightnessRange(
    val minimum: Int,
    val maximum: Int
) {
    init {
        require(minimum < maximum)
    }
}

enum class QuickToolsBrightnessWriteCapability {
    WriteSettings,
    Shizuku,
    None;

    val canWrite: Boolean
        get() = this != None
}

data class QuickToolsBrightnessSnapshot(
    val rawValue: Int,
    val ratio: Float,
    val autoEnabled: Boolean,
    val range: QuickToolsBrightnessRange,
    val writeCapability: QuickToolsBrightnessWriteCapability
)

data class QuickToolsBrightnessOperation(
    val result: QuickToolsOperationResult,
    val snapshot: QuickToolsBrightnessSnapshot
)

interface QuickToolsBrightnessGateway {

    fun readSnapshot(): QuickToolsBrightnessSnapshot

    fun observeChanges(onChanged: () -> Unit): AutoCloseable

    suspend fun setRatio(ratio: Float): QuickToolsBrightnessOperation

    suspend fun toggleAuto(): QuickToolsBrightnessOperation
}

object QuickToolsBrightnessMapping {

    private const val PerceptualBrightnessApi = 28
    private const val ModernBrightnessApi = 29
    private const val DefaultMinimum = 1
    private const val DefaultMaximum = 255
    private const val R = 0.5f
    private const val A = 0.17883277f
    private const val B = 0.28466892f
    private const val C = 0.55991073f
    private const val HlgScale = 12f

    fun resolveRange(
        sdkInt: Int,
        configuredMinimum: Int?,
        configuredMaximum: Int?
    ): QuickToolsBrightnessRange {
        val maximum = configuredMaximum?.takeIf { it > DefaultMinimum } ?: DefaultMaximum
        val configuredRangeMinimum = configuredMinimum
            ?.takeIf { it in DefaultMinimum until maximum }
            ?: DefaultMinimum
        val minimum = if (sdkInt >= ModernBrightnessApi && maximum == DefaultMaximum) {
            DefaultMinimum
        } else {
            configuredRangeMinimum
        }
        return QuickToolsBrightnessRange(minimum = minimum, maximum = maximum)
    }

    fun rawToRatio(
        rawValue: Int,
        range: QuickToolsBrightnessRange,
        sdkInt: Int
    ): Float {
        val linearRatio = normalize(rawValue.toFloat(), range)
        if (sdkInt < PerceptualBrightnessApi) {
            return linearRatio
        }
        val hlgValue = linearRatio * HlgScale
        val gammaRatio = if (hlgValue <= 1f) {
            sqrt(hlgValue) * R
        } else {
            A * ln(hlgValue - B) + C
        }
        return gammaRatio.coerceIn(0f, 1f)
    }

    fun ratioToRaw(
        ratio: Float,
        range: QuickToolsBrightnessRange,
        sdkInt: Int
    ): Int {
        val normalizedRatio = ratio.coerceIn(0f, 1f)
        if (sdkInt < PerceptualBrightnessApi) {
            return interpolate(normalizedRatio, range)
                .roundToInt()
                .coerceIn(range.minimum, range.maximum)
        }
        val hlgValue = if (normalizedRatio <= R) {
            val value = normalizedRatio / R
            value * value
        } else {
            exp((normalizedRatio - C) / A) + B
        }
        return interpolate(hlgValue / HlgScale, range)
            .roundToInt()
            .coerceIn(range.minimum, range.maximum)
    }

    private fun normalize(value: Float, range: QuickToolsBrightnessRange): Float {
        return ((value - range.minimum) / (range.maximum - range.minimum))
            .coerceIn(0f, 1f)
    }

    private fun interpolate(ratio: Float, range: QuickToolsBrightnessRange): Float {
        return range.minimum + (range.maximum - range.minimum) * ratio.coerceIn(0f, 1f)
    }
}

class QuickToolsBrightnessController(
    private val gateway: QuickToolsBrightnessGateway
) {

    private val brightnessWriteMutex = Mutex()
    private val autoToggleMutex = Mutex()
    private val brightnessWriteSequence = AtomicLong(0)
    private var observation: AutoCloseable? = null

    var snapshot: QuickToolsBrightnessSnapshot by mutableStateOf(gateway.readSnapshot())
        private set

    var pendingRatio: Float? by mutableStateOf(null)
        private set

    val displayedRatio: Float
        get() = pendingRatio ?: snapshot.ratio

    fun start() {
        if (observation == null) {
            observation = gateway.observeChanges(::refresh)
        }
        refresh()
    }

    fun stop() {
        brightnessWriteSequence.incrementAndGet()
        pendingRatio = null
        observation?.close()
        observation = null
    }

    suspend fun setRatio(ratio: Float): QuickToolsOperationResult {
        if (!snapshot.writeCapability.canWrite) {
            return QuickToolsOperationResult.NeedsWriteSettingsOrShizuku
        }
        val normalizedRatio = ratio.coerceIn(0f, 1f)
        val sequence = brightnessWriteSequence.incrementAndGet()
        pendingRatio = normalizedRatio
        brightnessWriteMutex.lock()
        try {
            if (sequence != brightnessWriteSequence.get()) {
                return QuickToolsOperationResult.Superseded
            }
            val operation = gateway.setRatio(normalizedRatio)
            if (sequence == brightnessWriteSequence.get()) {
                snapshot = operation.snapshot
                pendingRatio = null
            }
            return operation.result
        } finally {
            brightnessWriteMutex.unlock()
        }
    }

    suspend fun toggleAuto(): QuickToolsOperationResult {
        if (!snapshot.writeCapability.canWrite) {
            return QuickToolsOperationResult.NeedsWriteSettingsOrShizuku
        }
        autoToggleMutex.lock()
        try {
            val operation = gateway.toggleAuto()
            snapshot = operation.snapshot
            return operation.result
        } finally {
            autoToggleMutex.unlock()
        }
    }

    private fun refresh() {
        snapshot = gateway.readSnapshot()
    }
}
