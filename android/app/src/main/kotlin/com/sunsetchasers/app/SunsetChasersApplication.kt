package com.sunsetchasers.app

import android.app.Application
import com.sunsetchasers.feature.forecast.map.initializeOsmdroid
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SunsetChasersApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeOsmdroid(this)
    }
}
