package com.aaron.sidegesture.utils

import android.content.Context
import android.net.Uri
import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.constant.Paths
import com.aaron.sidegesture.entity.global.Backup
import com.aaron.sidegesture.entity.global.forceCrosshairMoveScreenStyle
import com.aaron.sidegesture.utils.DataStoreHolder.actionSettings
import com.aaron.sidegesture.utils.DataStoreHolder.advancedSettings
import com.aaron.sidegesture.utils.DataStoreHolder.bottomGestureButtons
import com.aaron.sidegesture.utils.DataStoreHolder.gestureSettings
import com.aaron.sidegesture.utils.DataStoreHolder.initialSettings
import com.aaron.sidegesture.utils.DataStoreHolder.sideGestureButtons
import com.blankj.utilcode.util.EncodeUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ZipUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/7/1
 */
object BackupHelper {

    private const val ZIP_BACKUP = "backup"
    private const val ZIP_IMAGES = "images"

    private val backupDir = "${Paths.AppCache}/backup"
    private val restoreDir = "${Paths.AppCache}/restore"

    private val backupItemFilePath = "$backupDir/$ZIP_BACKUP"
    private val zipImagePath = "$backupDir/$ZIP_IMAGES"
    private val zipFilePath = "$backupDir/zip"
    private val restoreFilePath = "$restoreDir/restore"

    suspend fun backup(context: Context, saveTo: Uri) {
        try {
            // 创建临时目录
            FileUtils.createOrExistsDir(backupDir)

            // 写入BackupItem，等待压缩
            val backupItemBytes = getBackupItemBytes()
            val backupItemFile = File(backupItemFilePath).also {
                FileUtils.createFileByDeleteOldFile(it)
                it.appendBytes(backupItemBytes)
            }

            // 压缩图片
            val zipImageDirFile = File(zipImagePath).also {
                FileUtils.createFileByDeleteOldFile(it)
            }
            val imageFiles = FileUtils.listFilesInDir(Paths.Image)
            ZipUtils.zipFiles(imageFiles, zipImageDirFile)

            // 将Backup和图片一起压缩
            val zipFile = File(zipFilePath).also {
                FileUtils.createFileByDeleteOldFile(it)
            }
            ZipUtils.zipFiles(listOf(backupItemFile, zipImageDirFile), zipFile)

            // 写入到用户路径
            context.contentResolver.openOutputStream(saveTo)!!.use { outputStream ->
                val zipFileBytes = FileIOUtils.readFile2BytesByStream(zipFile)
                outputStream.write(zipFileBytes)
                outputStream.flush()
            }
        } catch (ex: Exception) {
            throw ex
        } finally {
            // 删除临时目录
            FileUtils.deleteAllInDir(backupDir)
        }
    }

    suspend fun restore(context: Context, restoreFrom: Uri) {
        try {
            context.contentResolver.openInputStream(restoreFrom)!!.use { inputStream ->
                val input = inputStream.readBytes()
                try {
                    // 兼容旧版备份文件恢复
                    restoreBackupFromBytes(input)
                    FileUtils.deleteAllInDir(Paths.Image)
                } catch (ignored: Exception) {
                    val restoreDirFile = File(restoreDir).also {
                        // 创建临时目录
                        FileUtils.createOrExistsDir(it)
                    }
                    val restoreFile = File(restoreFilePath).also {
                        FileUtils.createFileByDeleteOldFile(it)
                        it.appendBytes(input)
                    }
                    ZipUtils.unzipFile(restoreFile, restoreDirFile).forEach { file ->
                        if (file.name == ZIP_BACKUP) {
                            restoreBackupFromBytes(file.readBytes())
                        } else if (file.name == ZIP_IMAGES) {
                            FileUtils.deleteAllInDir(Paths.Image)
                            FileUtils.createOrExistsDir(Paths.Image)
                            ZipUtils.unzipFile(file, restoreDirFile).forEach { imageFile ->
                                val destFile = File("${Paths.Image}/${imageFile.name}")
                                FileUtils.copy(imageFile, destFile)
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            throw ex
        } finally {
            // 删除临时目录
            FileUtils.deleteAllInDir(restoreDir)
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
                timestamp = System.currentTimeMillis(),
                version = BuildConfig.VERSION_NAME
            )
        }
        val json = JsonHelper.encodeToString(backup)
        return EncodeUtils.base64Encode(json.toByteArray())
    }

    private suspend fun restoreBackupFromBytes(bytes: ByteArray) {
        // 兼容旧版备份文件恢复
        val decoded = EncodeUtils.base64Decode(bytes)
        val backup = JsonHelper.decodeFromString<Backup>(String(decoded))
        coroutineScope {
            listOf(
                async {
                    initialSettings.updateData {
                        backup.initialSettings ?: it
                    }
                },
                async {
                    advancedSettings.updateData {
                        backup.advancedSettings ?: it
                    }
                },
                async {
                    gestureSettings.updateData {
                        backup.gestureSettings ?: it
                    }
                },
                async {
                    actionSettings.updateData {
                        backup.actionSettings?.forceCrosshairMoveScreenStyle() ?: it
                    }
                },
                async {
                    sideGestureButtons.updateData {
                        backup.gestureButtons ?: it
                    }
                },
                async {
                    bottomGestureButtons.updateData {
                        backup.bottomGestureButtons ?: it
                    }
                }
            ).awaitAll()
        }
    }
}
