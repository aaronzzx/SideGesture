package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.entity.GestureButton
import kotlinx.serialization.Serializable

@Serializable
@Keep
enum class RestorePhase {
    Complete,
    BlockRequested,
    Blocked,
    Writing,
    CommitRequested
}

@Serializable
@Keep
data class RestoreAck(
    val generation: Long,
    val serviceSession: String,
    val digest: String
)

@Serializable
@Keep
data class RestoreCoordination(
    val generation: Long = 0L,
    val phase: RestorePhase = RestorePhase.Complete,
    val inProgress: Boolean = false,
    val serviceSession: String = "",
    val blockedGenerationAck: Long = 0L,
    val blockedServiceSession: String = "",
    val noConsumerPath: Boolean = false,
    val commitGeneration: Long = 0L,
    val targetDigest: String = "",
    val commitReadyAck: RestoreAck? = null,
    val appliedAck: RestoreAck? = null,
    val failureReason: String? = null
) {
    val blocksRuntime: Boolean
        get() = inProgress || phase != RestorePhase.Complete
}

@Serializable
@Keep
data class RestorePayload(
    val initialSettings: InitialSettings,
    val advancedSettings: AdvancedSettings,
    val gestureSettings: GestureSettings,
    val actionSettings: ActionSettings,
    val sideGestureButtons: List<GestureButton>,
    val bottomGestureButtons: List<GestureButton>,
    val topGestureButtons: List<GestureButton>
)

@Serializable
@Keep
data class RestoreJournal(
    val generation: Long = 0L,
    val original: RestorePayload? = null,
    val target: RestorePayload? = null,
    val originalDigest: String = "",
    val targetDigest: String = "",
    val originalImageDigest: String = "",
    val targetImageDigest: String = "",
    val originalImagesDirectory: String = "",
    val targetImagesDirectory: String = "",
    val createdAt: Long = 0L
) {
    fun isValidFor(generation: Long): Boolean {
        return this.generation == generation &&
            original != null &&
            target != null &&
            originalDigest.isNotBlank() &&
            targetDigest.isNotBlank() &&
            originalImagesDirectory.isNotBlank() &&
            targetImagesDirectory.isNotBlank()
    }
}
