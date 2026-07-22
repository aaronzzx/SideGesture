package com.aaron.sidegesture.utils

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.constant.Paths
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.Backup
import com.aaron.sidegesture.entity.global.RestoreCoordination
import com.aaron.sidegesture.entity.global.RestoreJournal
import com.aaron.sidegesture.entity.global.RestorePayload
import com.aaron.sidegesture.entity.global.RestorePhase
import com.aaron.sidegesture.entity.global.forceCrosshairMoveScreenStyle
import com.aaron.sidegesture.feature.servicesettings.RestoreDigest
import com.aaron.sidegesture.feature.servicesettings.RestoreProtocol
import com.aaron.sidegesture.utils.DataStoreHolder.actionSettings
import com.aaron.sidegesture.utils.DataStoreHolder.advancedSettings
import com.aaron.sidegesture.utils.DataStoreHolder.bottomGestureButtons
import com.aaron.sidegesture.utils.DataStoreHolder.gestureSettings
import com.aaron.sidegesture.utils.DataStoreHolder.initialSettings
import com.aaron.sidegesture.utils.DataStoreHolder.restoreCoordination
import com.aaron.sidegesture.utils.DataStoreHolder.restoreJournal
import com.aaron.sidegesture.utils.DataStoreHolder.sideGestureButtons
import com.aaron.sidegesture.utils.DataStoreHolder.topGestureButtons
import com.blankj.utilcode.util.EncodeUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ZipUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/7/1
 */
object BackupHelper {

    data class RestoreResult(
        val topConfigurationIncluded: Boolean
    )

    private data class DecodedRestore(
        val backup: Backup,
        val imageSource: File?,
        val clearImages: Boolean
    )

    private const val ZIP_BACKUP = "backup"
    private const val ZIP_IMAGES = "images"
    private const val RESTORE_ACK_TIMEOUT_MS = 10_000L
    private const val RESTORE_JOURNAL_DIRECTORY = "restore_journal"

    private val backupDir = "${Paths.AppCache}/backup"
    private val restoreDir = "${Paths.AppCache}/restore"

    private val backupItemFilePath = "$backupDir/$ZIP_BACKUP"
    private val zipImagePath = "$backupDir/$ZIP_IMAGES"
    private val zipFilePath = "$backupDir/zip"
    private val restoreFilePath = "$restoreDir/restore"

    suspend fun backup(context: Context, saveTo: Uri) {
        try {
            FileUtils.createOrExistsDir(backupDir)

            val backupItemBytes = getBackupItemBytes()
            val backupItemFile = File(backupItemFilePath).also {
                FileUtils.createFileByDeleteOldFile(it)
                it.appendBytes(backupItemBytes)
            }

            val zipImageDirFile = File(zipImagePath).also {
                FileUtils.createFileByDeleteOldFile(it)
            }
            val imageFiles = FileUtils.listFilesInDir(Paths.Image)
            ZipUtils.zipFiles(imageFiles, zipImageDirFile)

            val zipFile = File(zipFilePath).also {
                FileUtils.createFileByDeleteOldFile(it)
            }
            ZipUtils.zipFiles(listOf(backupItemFile, zipImageDirFile), zipFile)

            context.contentResolver.openOutputStream(saveTo)!!.use { outputStream ->
                val zipFileBytes = FileIOUtils.readFile2BytesByStream(zipFile)
                outputStream.write(zipFileBytes)
                outputStream.flush()
            }
        } finally {
            FileUtils.deleteAllInDir(backupDir)
        }
    }

    suspend fun restore(context: Context, restoreFrom: Uri): RestoreResult {
        recoverIncompleteRestore(context)
        val input = context.contentResolver.openInputStream(restoreFrom)!!.use { it.readBytes() }
        val temporaryDirectory = File(restoreDir)
        try {
            resetDirectory(temporaryDirectory)
            val decoded = decodeRestore(input, temporaryDirectory)
            val original = RestoreDigest.readPayload()
            val target = mergeAndValidate(original, decoded.backup)
            val currentCoordination = restoreCoordination.data.first()
            check(!currentCoordination.blocksRuntime) { "Another restore is already in progress" }
            val generation = RestoreProtocol.nextGeneration(currentCoordination)
            val journal = createJournal(
                context = context,
                generation = generation,
                original = original,
                target = target,
                decoded = decoded
            )
            restoreJournal.updateData { journal }
            restoreCoordination.updateData { current ->
                check(current.generation == currentCoordination.generation && !current.blocksRuntime) {
                    "Restore coordination changed before begin"
                }
                RestoreProtocol.begin(
                    current = current,
                    generation = generation,
                    targetDigest = journal.targetDigest
                )
            }

            try {
                executeGeneration(
                    context = context,
                    journal = journal,
                    payload = target,
                    imagesDirectory = File(journal.targetImagesDirectory)
                )
                cleanupJournal(context, journal)
            } catch (error: Throwable) {
                val coordination = restoreCoordination.data.first()
                if (!coordination.blocksRuntime) {
                    throw error
                }
                runCatching {
                    restartGeneration(journal, journal.originalDigest)
                    executeGeneration(
                        context = context,
                        journal = journal,
                        payload = original,
                        imagesDirectory = File(journal.originalImagesDirectory)
                    )
                    cleanupJournal(context, journal)
                }.onFailure { rollbackError ->
                    recordRestoreFailure(
                        generation = journal.generation,
                        reason = "rollback:${rollbackError.message.orEmpty()}"
                    )
                }.getOrThrow()
                throw error
            }
            return RestoreResult(
                topConfigurationIncluded = decoded.backup.topGestureButtons != null
            )
        } finally {
            FileUtils.deleteAllInDir(restoreDir)
        }
    }

    suspend fun recoverIncompleteRestore(context: Context) {
        val coordination = restoreCoordination.data.first()
        val journal = restoreJournal.data.first()
        if (!coordination.blocksRuntime) {
            if (journal.generation > 0L) cleanupJournal(context, journal)
            return
        }
        check(journal.isValidFor(coordination.generation)) {
            "Restore is blocked without a valid journal"
        }

        val target = requireNotNull(journal.target)
        val targetImages = File(journal.targetImagesDirectory)
        val targetStagingDigest = RestoreDigest.digest(target, targetImages)
        val targetResult = runCatching {
            check(targetStagingDigest == journal.targetDigest) {
                "Restore target journal digest mismatch"
            }
            restartGeneration(journal, journal.targetDigest)
            executeGeneration(context, journal, target, targetImages)
            cleanupJournal(context, journal)
        }
        if (targetResult.isSuccess) return
        if (!restoreCoordination.data.first().blocksRuntime) {
            cleanupJournal(context, journal)
            return
        }

        val original = requireNotNull(journal.original)
        val originalImages = File(journal.originalImagesDirectory)
        val originalStagingDigest = RestoreDigest.digest(original, originalImages)
        try {
            check(originalStagingDigest == journal.originalDigest) {
                "Restore original journal digest mismatch"
            }
            restartGeneration(journal, journal.originalDigest)
            executeGeneration(context, journal, original, originalImages)
            cleanupJournal(context, journal)
        } catch (rollbackError: Throwable) {
            recordRestoreFailure(
                generation = journal.generation,
                reason = "recovery:${rollbackError.message.orEmpty()}"
            )
            throw IllegalStateException(
                "Unable to replay or roll back incomplete restore",
                rollbackError
            )
        }
    }

    private suspend fun getBackupItemBytes(): ByteArray {
        val backup = coroutineScope {
            Backup(
                initialSettings = async { initialSettings.data.first() }.await(),
                advancedSettings = async { advancedSettings.data.first() }.await(),
                gestureSettings = async { gestureSettings.data.first() }.await(),
                actionSettings = async { actionSettings.data.first() }.await(),
                gestureButtons = async { sideGestureButtons.data.first() }.await(),
                bottomGestureButtons = async { bottomGestureButtons.data.first() }.await(),
                topGestureButtons = async { topGestureButtons.data.first() }.await(),
                timestamp = System.currentTimeMillis(),
                version = BuildConfig.VERSION_NAME
            )
        }
        val json = JsonHelper.encodeToString(backup)
        return EncodeUtils.base64Encode(json.toByteArray())
    }

    private fun decodeRestore(bytes: ByteArray, temporaryDirectory: File): DecodedRestore {
        decodeBackup(bytes)?.let { backup ->
            val emptyImages = File(temporaryDirectory, "legacy_images").apply { mkdirs() }
            return DecodedRestore(backup, emptyImages, clearImages = true)
        }

        val restoreFile = File(restoreFilePath).also {
            FileUtils.createFileByDeleteOldFile(it)
            it.appendBytes(bytes)
        }
        val outerDirectory = File(temporaryDirectory, "outer").apply { mkdirs() }
        val outerFiles = ZipUtils.unzipFile(restoreFile, outerDirectory)
        val backupFile = outerFiles.firstOrNull { it.name == ZIP_BACKUP }
            ?: error("Backup payload is missing")
        val backup = decodeBackup(backupFile.readBytes())
            ?: error("Backup payload is invalid")
        val imageArchive = outerFiles.firstOrNull { it.name == ZIP_IMAGES }
        val imageSource = imageArchive?.let { archive ->
            File(temporaryDirectory, "images").also { imageDirectory ->
                resetDirectory(imageDirectory)
                ZipUtils.unzipFile(archive, imageDirectory)
            }
        }
        return DecodedRestore(
            backup = backup,
            imageSource = imageSource,
            clearImages = imageArchive != null
        )
    }

    private fun decodeBackup(bytes: ByteArray): Backup? {
        return runCatching {
            val decoded = EncodeUtils.base64Decode(bytes)
            JsonHelper.decodeFromString<Backup>(String(decoded))
        }.getOrNull()
    }

    private fun mergeAndValidate(original: RestorePayload, backup: Backup): RestorePayload {
        val target = RestorePayload(
            initialSettings = backup.initialSettings ?: original.initialSettings,
            advancedSettings = backup.advancedSettings ?: original.advancedSettings,
            gestureSettings = backup.gestureSettings ?: original.gestureSettings,
            actionSettings = backup.actionSettings?.forceCrosshairMoveScreenStyle()
                ?: original.actionSettings,
            sideGestureButtons = backup.gestureButtons ?: original.sideGestureButtons,
            bottomGestureButtons = backup.bottomGestureButtons ?: original.bottomGestureButtons,
            topGestureButtons = backup.topGestureButtons?.map { button ->
                button.copy(
                    alignRegion = false,
                    excludeSystemGestureRects = false
                )
            } ?: original.topGestureButtons
        )
        validateButtons(
            buttons = target.sideGestureButtons,
            allowedPositions = setOf(Position.Left, Position.Right),
            label = "side"
        )
        validateButtons(
            buttons = target.bottomGestureButtons,
            allowedPositions = setOf(Position.Bottom),
            label = "bottom"
        )
        validateButtons(
            buttons = target.topGestureButtons,
            allowedPositions = setOf(Position.Top),
            label = "top"
        )
        return target
    }

    private fun validateButtons(
        buttons: List<GestureButton>,
        allowedPositions: Set<Position>,
        label: String
    ) {
        val keys = mutableSetOf<String>()
        buttons.forEach { button ->
            require(button.id.isNotBlank()) { "$label gesture button id is empty" }
            require(button.position in allowedPositions) {
                "$label gesture button has invalid position ${button.position}"
            }
            require(button.start in 0f..1f && button.end in 0f..1f && button.start <= button.end) {
                "$label gesture button ${button.id} has invalid bounds"
            }
            require(button.width > 0) { "$label gesture button ${button.id} has invalid width" }
            val key = "${button.id}|${button.position}"
            require(keys.add(key)) { "$label gesture button $key is duplicated" }
        }
    }

    private fun createJournal(
        context: Context,
        generation: Long,
        original: RestorePayload,
        target: RestorePayload,
        decoded: DecodedRestore
    ): RestoreJournal {
        val generationDirectory = File(journalRoot(context), generation.toString())
        val originalImagesDirectory = File(generationDirectory, "original_images")
        val targetImagesDirectory = File(generationDirectory, "target_images")
        resetDirectory(generationDirectory)
        copyDirectoryContents(File(Paths.Image), originalImagesDirectory)
        when {
            decoded.imageSource != null -> copyDirectoryContents(
                decoded.imageSource,
                targetImagesDirectory
            )
            decoded.clearImages -> resetDirectory(targetImagesDirectory)
            else -> copyDirectoryContents(File(Paths.Image), targetImagesDirectory)
        }
        val originalImageDigest = RestoreDigest.imageDigest(originalImagesDirectory)
        val targetImageDigest = RestoreDigest.imageDigest(targetImagesDirectory)
        return RestoreJournal(
            generation = generation,
            original = original,
            target = target,
            originalDigest = RestoreDigest.combinedDigest(
                RestoreDigest.payloadDigest(original),
                originalImageDigest
            ),
            targetDigest = RestoreDigest.combinedDigest(
                RestoreDigest.payloadDigest(target),
                targetImageDigest
            ),
            originalImageDigest = originalImageDigest,
            targetImageDigest = targetImageDigest,
            originalImagesDirectory = originalImagesDirectory.absolutePath,
            targetImagesDirectory = targetImagesDirectory.absolutePath,
            createdAt = System.currentTimeMillis()
        )
    }

    private suspend fun restartGeneration(journal: RestoreJournal, targetDigest: String) {
        restoreCoordination.updateData { current ->
            require(current.generation == journal.generation)
            RestoreProtocol.restart(current, targetDigest)
        }
    }

    private suspend fun executeGeneration(
        context: Context,
        journal: RestoreJournal,
        payload: RestorePayload,
        imagesDirectory: File
    ) {
        blockConsumers(context, journal.generation)
        restoreCoordination.updateData { current ->
            require(current.generation == journal.generation)
            RestoreProtocol.markWriting(current)
        }

        try {
            writePayload(payload)
            replaceImages(imagesDirectory, File(Paths.Image))
            val livePayload = RestoreDigest.readPayload()
            val liveDigest = RestoreDigest.digest(livePayload, File(Paths.Image))
            val expectedDigest = restoreCoordination.data.first().targetDigest
            check(liveDigest == expectedDigest) {
                "Restore verification failed: ${liveDigest.take(16)}"
            }
            restoreCoordination.updateData { current ->
                require(current.generation == journal.generation)
                RestoreProtocol.requestCommit(current)
            }
            val expectedSession = awaitCommitReady(context, journal.generation)
            restoreCoordination.updateData { current ->
                require(current.generation == journal.generation)
                RestoreProtocol.complete(current, expectedSession)
            }
            if (expectedSession != RestoreProtocol.NO_CONSUMER_SESSION) {
                awaitApplied(journal.generation, expectedSession)
            }
        } catch (error: Throwable) {
            recordRestoreFailure(
                generation = journal.generation,
                reason = "execute:${error.message.orEmpty()}"
            )
            throw error
        }
    }

    private suspend fun blockConsumers(context: Context, generation: Long) {
        if (!isServiceProcessRunning(context)) {
            restoreCoordination.updateData { current ->
                require(current.generation == generation)
                RestoreProtocol.markNoConsumerBlocked(current)
            }
            return
        }
        val blocked = withTimeout(RESTORE_ACK_TIMEOUT_MS) {
            restoreCoordination.data.first { current ->
                current.generation == generation &&
                    current.inProgress &&
                    current.blockedGenerationAck == generation &&
                    current.blockedServiceSession.isNotBlank() &&
                    current.blockedServiceSession == current.serviceSession
            }
        }
        restoreCoordination.updateData { current ->
            require(current.generation == generation)
            RestoreProtocol.markBlocked(current, blocked.blockedServiceSession)
        }
    }

    private suspend fun awaitCommitReady(context: Context, generation: Long): String {
        if (!isServiceProcessRunning(context)) {
            restoreCoordination.updateData { current ->
                require(current.generation == generation)
                RestoreProtocol.acknowledgeNoConsumerCommitReady(
                    current.copy(noConsumerPath = true)
                )
            }
            return RestoreProtocol.NO_CONSUMER_SESSION
        }
        val ready = withTimeout(RESTORE_ACK_TIMEOUT_MS) {
            restoreCoordination.data.first { current ->
                val session = current.serviceSession
                current.generation == generation &&
                    current.phase == RestorePhase.CommitRequested &&
                    session.isNotBlank() &&
                    current.blockedGenerationAck == generation &&
                    current.blockedServiceSession == session &&
                    current.commitReadyAck?.let { ack ->
                        ack.generation == generation &&
                            ack.serviceSession == session &&
                            ack.digest == current.targetDigest
                    } == true
            }
        }
        return ready.serviceSession
    }

    private suspend fun awaitApplied(generation: Long, serviceSession: String) {
        withTimeout(RESTORE_ACK_TIMEOUT_MS) {
            restoreCoordination.data.first { current ->
                current.generation == generation &&
                    !current.blocksRuntime &&
                    current.appliedAck?.let { ack ->
                        ack.generation == generation &&
                            ack.serviceSession == serviceSession &&
                            ack.digest == current.targetDigest
                    } == true
            }
        }
    }

    private suspend fun writePayload(payload: RestorePayload) {
        initialSettings.updateData { payload.initialSettings }
        advancedSettings.updateData { payload.advancedSettings }
        gestureSettings.updateData { payload.gestureSettings }
        actionSettings.updateData { payload.actionSettings }
        sideGestureButtons.updateData { payload.sideGestureButtons }
        bottomGestureButtons.updateData { payload.bottomGestureButtons }
        topGestureButtons.updateData { payload.topGestureButtons }
    }

    private suspend fun recordRestoreFailure(generation: Long, reason: String) {
        restoreCoordination.updateData { current ->
            if (current.generation != generation) current else RestoreProtocol.fail(current, reason)
        }
    }

    private suspend fun cleanupJournal(context: Context, journal: RestoreJournal) {
        restoreJournal.updateData { current ->
            if (current.generation == journal.generation) RestoreJournal() else current
        }
        val root = journalRoot(context).canonicalFile
        val generationDirectory = File(root, journal.generation.toString()).canonicalFile
        if (generationDirectory.parentFile == root) {
            generationDirectory.deleteRecursively()
        }
    }

    private fun isServiceProcessRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val serviceProcessName = "${context.packageName}:service"
        return activityManager.runningAppProcesses?.any { process ->
            process.processName == serviceProcessName
        } == true
    }

    private fun journalRoot(context: Context): File {
        return File(context.filesDir, RESTORE_JOURNAL_DIRECTORY).apply { mkdirs() }
    }

    private fun resetDirectory(directory: File) {
        directory.deleteRecursively()
        check(directory.mkdirs() || directory.isDirectory) {
            "Unable to create directory ${directory.absolutePath}"
        }
    }

    private fun copyDirectoryContents(source: File, destination: File) {
        resetDirectory(destination)
        if (!source.exists()) return
        source.walkTopDown().forEach { item ->
            if (item == source) return@forEach
            val relative = item.relativeTo(source)
            val target = File(destination, relative.path)
            if (item.isDirectory) {
                check(target.mkdirs() || target.isDirectory)
            } else {
                check(target.parentFile?.mkdirs() != false || target.parentFile?.isDirectory == true)
                item.copyTo(target, overwrite = true)
            }
        }
    }

    private fun replaceImages(source: File, destination: File) {
        check(source.isDirectory) { "Restore image staging directory is missing" }
        check(destination.mkdirs() || destination.isDirectory)
        destination.listFiles()?.forEach { item ->
            check(item.deleteRecursively()) { "Unable to clear restored image ${item.name}" }
        }
        source.walkTopDown().forEach { item ->
            if (item == source) return@forEach
            val relative = item.relativeTo(source)
            val target = File(destination, relative.path)
            if (item.isDirectory) {
                check(target.mkdirs() || target.isDirectory)
            } else {
                check(target.parentFile?.mkdirs() != false || target.parentFile?.isDirectory == true)
                item.copyTo(target, overwrite = true)
            }
        }
    }
}
