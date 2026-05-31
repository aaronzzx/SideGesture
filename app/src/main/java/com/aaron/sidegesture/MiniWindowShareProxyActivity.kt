package com.aaron.sidegesture

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle

class MiniWindowShareProxyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetIntent = getTargetIntent()
        if (targetIntent != null) {
            runCatching {
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(targetIntent)
            }
        }
        finish()
    }

    private fun getTargetIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
        }
    }
}
