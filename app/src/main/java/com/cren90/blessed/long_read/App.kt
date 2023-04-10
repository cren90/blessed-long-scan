package com.cren90.blessed.long_read

import android.app.Application
import com.renfrowtech.kmp.log.LogLevel
import com.renfrowtech.kmp.log.Logger
import com.renfrowtech.kmp.log.strategy.LogcatStrategy

class App: Application() {

    override fun onCreate() {
        super.onCreate()

        Logger.addStrategies(LogcatStrategy(LogLevel.TRACE))
    }
}