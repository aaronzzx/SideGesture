package com.aaron.sidegesture.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.aaron.sidegesture.App
import com.aaron.sidegesture.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object ShizukuShellManager {

    private const val RequestCode = 41051
    private const val ServiceTag = "sidegesture_shell"
    private val ShizukuCompatPackages = listOf(
        "moe.shizuku.privileged.api",
        "roro.stellar.manager"
    )

    private val bindMutex = Mutex()
    private val autoPermissionMutex = Mutex()
    private val permissionMutex = Mutex()
    private val statusMutableStateFlow = MutableStateFlow(snapshot())

    @Volatile
    private var service: IShizukuShellService? = null

    @Volatile
    private var serviceReady: CompletableDeferred<IShizukuShellService>? = null

    @Volatile
    private var permissionResult: CompletableDeferred<Boolean>? = null

    @Volatile
    private var autoPermissionRequested = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(App.getContext(), ShizukuShellUserService::class.java)
        )
            .processNameSuffix("shizuku_shell")
            .tag(ServiceTag)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val target = IShizukuShellService.Stub.asInterface(binder)
            service = target
            serviceReady?.complete(target)
            updateStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            serviceReady = null
            updateStatus()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        serviceReady = null
        updateStatus()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != RequestCode) {
                return@OnRequestPermissionResultListener
            }
            permissionResult?.complete(grantResult == PackageManager.PERMISSION_GRANTED)
            permissionResult = null
            updateStatus()
        }

    init {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        updateStatus()
    }

    val statusFlow: StateFlow<ShizukuStatus> = statusMutableStateFlow.asStateFlow()

    fun currentStatus(): ShizukuStatus = statusMutableStateFlow.value

    fun updateStatus() {
        statusMutableStateFlow.value = snapshot()
    }

    suspend fun autoRequestPermissionIfNeeded(): Boolean {
        updateStatus()
        if (!shouldAutoRequest(currentStatus())) {
            return false
        }
        return autoPermissionMutex.withLock {
            updateStatus()
            val status = currentStatus()
            if (!shouldAutoRequest(status)) {
                return@withLock false
            }
            autoPermissionRequested = true
            requestPermission()
        }
    }

    suspend fun requestPermission(): Boolean {
        return permissionMutex.withLock {
            if (!isBinderAlive()) {
                updateStatus()
                return@withLock false
            }
            if (hasPermission()) {
                updateStatus()
                return@withLock true
            }
            val deferred = CompletableDeferred<Boolean>()
            permissionResult = deferred
            Shizuku.requestPermission(RequestCode)
            val granted = deferred.await()
            updateStatus()
            granted
        }
    }

    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (command.isBlank()) {
            return@withContext ShellResult(errorMessage = "Command is blank")
        }
        if (!isBinderAlive()) {
            return@withContext ShellResult(errorMessage = "Shizuku binder unavailable")
        }
        if (!hasPermission()) {
            return@withContext ShellResult(errorMessage = "Shizuku permission denied")
        }
        runCatching {
            ensureService().execute(command)
        }.getOrElse { throwable ->
            service = null
            serviceReady = null
            updateStatus()
            ShellResult(errorMessage = throwable.message ?: throwable.javaClass.simpleName)
        }
    }

    suspend fun releaseService() {
        bindMutex.withLock {
            runCatching {
                service?.destroy()
            }
            runCatching {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
            service = null
            serviceReady = null
            updateStatus()
        }
    }

    private suspend fun ensureService(): IShizukuShellService {
        service?.let { return it }
        return bindMutex.withLock {
            val currentService = service
            if (currentService != null) {
                return@withLock currentService
            }
            val waiter = serviceReady ?: CompletableDeferred<IShizukuShellService>().also { deferred ->
                serviceReady = deferred
                Shizuku.bindUserService(userServiceArgs, serviceConnection)
            }
            waiter.await()
        }
    }

    private fun snapshot(): ShizukuStatus {
        val context = App.getContext()
        val binderAlive = isBinderAlive()
        val permissionGranted = binderAlive && hasPermission()
        val uid = if (binderAlive) runCatching { Shizuku.getUid() }.getOrNull() else null
        val installed = ShizukuCompatPackages.any { pkg ->
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
            }.isSuccess
        }
        return ShizukuStatus(
            installed = installed,
            binderAlive = binderAlive,
            permissionGranted = permissionGranted,
            serviceBound = service != null,
            uid = uid
        )
    }

    private fun isBinderAlive(): Boolean {
        return runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    }

    private fun hasPermission(): Boolean {
        return runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    private fun shouldAutoRequest(status: ShizukuStatus): Boolean {
        return status.installed &&
            status.binderAlive &&
            !status.permissionGranted &&
            !autoPermissionRequested
    }

    data class ShizukuStatus(
        val installed: Boolean,
        val binderAlive: Boolean,
        val permissionGranted: Boolean,
        val serviceBound: Boolean,
        val uid: Int?
    ) {
        val executorLabel: String
            get() = when (uid) {
                0 -> "uid=0"
                2000 -> "uid=2000"
                null -> "-"
                else -> "uid=$uid"
            }
    }
}
