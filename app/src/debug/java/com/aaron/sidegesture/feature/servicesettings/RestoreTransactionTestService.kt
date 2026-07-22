package com.aaron.sidegesture.feature.servicesettings

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class RestoreTransactionTestService : Service() {

    companion object {
        const val ACTION_STOP = "com.aaron.sidegesture.action.STOP_RESTORE_TEST_SERVICE"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val restoreGate = RestoreServiceGate(
        scope = scope,
        settingsStore = ServiceSettingsStore(
            scope = scope,
            coordinationSource = DataStoreHolder.restoreCoordination.data
        ),
        onBlocked = {},
        onApply = {}
    )
    private var killProcessOnDestroy = false

    override fun onCreate() {
        super.onCreate()
        restoreGate.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            killProcessOnDestroy = true
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        restoreGate.release()
        scope.cancel()
        super.onDestroy()
        if (killProcessOnDestroy) Process.killProcess(Process.myPid())
    }
}
