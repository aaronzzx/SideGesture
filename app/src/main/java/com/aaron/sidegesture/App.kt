package com.aaron.sidegesture

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

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

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}