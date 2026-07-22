package com.aaron.sidegesture.feature.servicesettings

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.constant.Paths
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.Backup
import com.aaron.sidegesture.entity.global.RestoreJournal
import com.aaron.sidegesture.entity.global.RestorePayload
import com.aaron.sidegesture.entity.global.RestorePhase
import com.aaron.sidegesture.utils.BackupHelper
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.JsonHelper
import com.blankj.utilcode.util.EncodeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRestoreTransactionInstrumentedTest {

    @Test
    fun noConsumerRestoreCommitsTopThroughExplicitLocalHandshake() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("service process must be stopped for this test", isServiceProcessRunning(context))
        withRestoredEnvironment(context) {
            val target = topButton("no-consumer")

            val result = restoreRawBackup(context, Backup(topGestureButtons = listOf(target)))
            val coordination = DataStoreHolder.restoreCoordination.data.first()

            assertTrue(result.topConfigurationIncluded)
            assertEquals(listOf(target), DataStoreHolder.topGestureButtons.data.first())
            assertEquals(RestorePhase.Complete, coordination.phase)
            assertFalse(coordination.inProgress)
            assertTrue(coordination.noConsumerPath)
            assertEquals(
                RestoreProtocol.NO_CONSUMER_SESSION,
                coordination.commitReadyAck?.serviceSession
            )
            assertNull(coordination.appliedAck)
            assertEquals(0L, DataStoreHolder.restoreJournal.data.first().generation)
        }
    }

    @Test
    fun oldBackupWithoutTopPreservesCurrentTopExactly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("service process must be stopped for this test", isServiceProcessRunning(context))
        withRestoredEnvironment(context) {
            val current = topButton("preserved").copy(
                alignRegion = true,
                excludeSystemGestureRects = true
            )
            DataStoreHolder.topGestureButtons.updateData { listOf(current) }

            val result = restoreRawBackup(context, Backup())

            assertFalse(result.topConfigurationIncluded)
            assertEquals(listOf(current), DataStoreHolder.topGestureButtons.data.first())
        }
    }

    @Test
    fun explicitEmptyTopBackupClearsCurrentTop() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("service process must be stopped for this test", isServiceProcessRunning(context))
        withRestoredEnvironment(context) {
            DataStoreHolder.topGestureButtons.updateData { listOf(topButton("clear")) }

            val result = restoreRawBackup(context, Backup(topGestureButtons = emptyList()))

            assertTrue(result.topConfigurationIncluded)
            assertTrue(DataStoreHolder.topGestureButtons.data.first().isEmpty())
        }
    }

    @Test
    fun invalidTopBackupIsRejectedBeforeCoordinationStarts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("service process must be stopped for this test", isServiceProcessRunning(context))
        withRestoredEnvironment(context) {
            val current = topButton("current")
            DataStoreHolder.topGestureButtons.updateData { listOf(current) }
            val coordinationBefore = DataStoreHolder.restoreCoordination.data.first()

            val result = runCatching {
                restoreRawBackup(
                    context,
                    Backup(
                        topGestureButtons = listOf(
                            topButton("invalid").copy(position = Position.Bottom)
                        )
                    )
                )
            }

            assertTrue(result.isFailure)
            assertEquals(listOf(current), DataStoreHolder.topGestureButtons.data.first())
            assertEquals(coordinationBefore, DataStoreHolder.restoreCoordination.data.first())
            assertEquals(0L, DataStoreHolder.restoreJournal.data.first().generation)
        }
    }

    @Test
    fun startupRecoveryReplaysCompleteTargetFromPersistentJournal() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("service process must be stopped for this test", isServiceProcessRunning(context))
        withRestoredEnvironment(context) {
            val original = RestoreDigest.readPayload()
            val target = original.copy(topGestureButtons = listOf(topButton("replayed")))
            val fixture = prepareIncompleteRecovery(context, original, target)
            try {
                BackupHelper.recoverIncompleteRestore(context)

                assertEquals(target, RestoreDigest.readPayload())
                assertEquals(RestorePhase.Complete, DataStoreHolder.restoreCoordination.data.first().phase)
                assertEquals(0L, DataStoreHolder.restoreJournal.data.first().generation)
            } finally {
                fixture.deleteRecursively()
            }
        }
    }

    @Test
    fun startupRecoveryRollsBackWhenTargetJournalDigestIsInvalid() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("service process must be stopped for this test", isServiceProcessRunning(context))
        withRestoredEnvironment(context) {
            val original = RestoreDigest.readPayload()
            val target = original.copy(topGestureButtons = listOf(topButton("invalid-target")))
            val fixture = prepareIncompleteRecovery(context, original, target)
            try {
                File(fixture, "target_images/corrupt").writeText("corrupt")
                DataStoreHolder.topGestureButtons.updateData { listOf(topButton("partial")) }

                BackupHelper.recoverIncompleteRestore(context)

                assertEquals(original, RestoreDigest.readPayload())
                val coordination = DataStoreHolder.restoreCoordination.data.first()
                assertEquals(RestorePhase.Complete, coordination.phase)
                assertEquals(
                    RestoreDigest.digest(original, File(Paths.Image)),
                    coordination.targetDigest
                )
                assertEquals(0L, DataStoreHolder.restoreJournal.data.first().generation)
            } finally {
                fixture.deleteRecursively()
            }
        }
    }

    @Test
    fun runningServiceRestoresOnlyAfterCommitReadyAndWritesAppliedAck() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previousSession = DataStoreHolder.restoreCoordination.data.first().serviceSession
        val serviceIntent = Intent(context, RestoreTransactionTestService::class.java)
        try {
            context.startService(serviceIntent)
            awaitCondition("service process did not start") {
                isServiceProcessRunning(context)
            }
            awaitCondition("service session was not refreshed") {
                DataStoreHolder.restoreCoordination.data.first().serviceSession.let { session ->
                    session.isNotBlank() && session != previousSession
                }
            }

            withRestoredEnvironment(context) {
                val target = topButton("service")

                restoreRawBackup(context, Backup(topGestureButtons = listOf(target)))
                val coordination = DataStoreHolder.restoreCoordination.data.first()

                assertEquals(listOf(target), DataStoreHolder.topGestureButtons.data.first())
                assertFalse(coordination.noConsumerPath)
                assertEquals(RestorePhase.Complete, coordination.phase)
                assertFalse(coordination.inProgress)
                assertEquals(
                    coordination.serviceSession,
                    coordination.commitReadyAck?.serviceSession
                )
                assertEquals(
                    coordination.serviceSession,
                    coordination.appliedAck?.serviceSession
                )
                assertEquals(coordination.targetDigest, coordination.appliedAck?.digest)
            }
        } finally {
            context.startService(
                serviceIntent.setAction(RestoreTransactionTestService.ACTION_STOP)
            )
            awaitCondition("test service process did not stop") {
                !isServiceProcessRunning(context)
            }
        }
    }

    private suspend fun withRestoredEnvironment(
        context: Context,
        block: suspend () -> Unit
    ) {
        val originalPayload = RestoreDigest.readPayload()
        val imageBackup = File(context.cacheDir, "restore-test-images")
        copyDirectory(File(Paths.Image), imageBackup)
        try {
            block()
        } finally {
            writePayload(originalPayload)
            copyDirectory(imageBackup, File(Paths.Image))
            imageBackup.deleteRecursively()
        }
    }

    private suspend fun restoreRawBackup(context: Context, backup: Backup): BackupHelper.RestoreResult {
        val encoded = EncodeUtils.base64Encode(
            JsonHelper.encodeToString(backup).encodeToByteArray()
        )
        val file = File(context.cacheDir, "restore-transaction-test.backup")
        file.writeBytes(encoded)
        return try {
            BackupHelper.restore(context, Uri.fromFile(file))
        } finally {
            file.delete()
        }
    }

    private suspend fun writePayload(payload: RestorePayload) {
        DataStoreHolder.initialSettings.updateData { payload.initialSettings }
        DataStoreHolder.advancedSettings.updateData { payload.advancedSettings }
        DataStoreHolder.gestureSettings.updateData { payload.gestureSettings }
        DataStoreHolder.actionSettings.updateData { payload.actionSettings }
        DataStoreHolder.sideGestureButtons.updateData { payload.sideGestureButtons }
        DataStoreHolder.bottomGestureButtons.updateData { payload.bottomGestureButtons }
        DataStoreHolder.topGestureButtons.updateData { payload.topGestureButtons }
    }

    private suspend fun prepareIncompleteRecovery(
        context: Context,
        original: RestorePayload,
        target: RestorePayload
    ): File {
        val fixture = File(context.cacheDir, "restore-recovery-fixture")
        val originalImages = File(fixture, "original_images")
        val targetImages = File(fixture, "target_images")
        copyDirectory(File(Paths.Image), originalImages)
        copyDirectory(File(Paths.Image), targetImages)
        val originalDigest = RestoreDigest.digest(original, originalImages)
        val targetDigest = RestoreDigest.digest(target, targetImages)
        val current = DataStoreHolder.restoreCoordination.data.first()
        val generation = RestoreProtocol.nextGeneration(current)
        DataStoreHolder.restoreJournal.updateData {
            RestoreJournal(
                generation = generation,
                original = original,
                target = target,
                originalDigest = originalDigest,
                targetDigest = targetDigest,
                originalImageDigest = RestoreDigest.imageDigest(originalImages),
                targetImageDigest = RestoreDigest.imageDigest(targetImages),
                originalImagesDirectory = originalImages.absolutePath,
                targetImagesDirectory = targetImages.absolutePath,
                createdAt = System.currentTimeMillis()
            )
        }
        DataStoreHolder.restoreCoordination.updateData {
            RestoreProtocol.markWriting(
                RestoreProtocol.markNoConsumerBlocked(
                    RestoreProtocol.begin(current, generation, targetDigest)
                )
            )
        }
        return fixture
    }

    private fun topButton(id: String): GestureButton {
        return GestureButton(
            id = id,
            position = Position.Top,
            enabled = true,
            start = 0.2f,
            end = 0.8f,
            width = 48,
            slideActions = GestureActions(),
            longSlideActions = GestureActions(),
            color = 0xff336699.toInt(),
            alignRegion = false,
            excludeSystemGestureRects = false,
            limitMaxExcludeSystemGestureLength = true
        )
    }

    private fun isServiceProcessRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processName = "${context.packageName}:service"
        return activityManager.runningAppProcesses?.any { it.processName == processName } == true
    }

    private suspend fun awaitCondition(message: String, condition: suspend () -> Boolean) {
        val completed = withTimeoutOrNull(10_000L) {
            while (!condition()) delay(50L)
            true
        }
        assertTrue(message, completed == true)
    }

    private fun copyDirectory(source: File, destination: File) {
        destination.deleteRecursively()
        destination.mkdirs()
        if (!source.exists()) return
        source.walkTopDown().forEach { item ->
            if (item == source) return@forEach
            val target = File(destination, item.relativeTo(source).path)
            if (item.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                item.copyTo(target, overwrite = true)
            }
        }
    }

}
