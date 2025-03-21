package com.aaron.sidegesture

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.aaron.compose.component.UDFComponentDefaults
import com.aaron.sidegesture.defaults.UDFComponentDefaultsImpl
import me.weishu.reflection.Reflection

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/17
 */
class App : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context

        fun getContext(): Context {
            return context
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base)
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        UDFComponentDefaults.set(UDFComponentDefaultsImpl())
    }
}