package com.example.locationdemo

import android.app.Application
import com.example.locationdemo.location.LocationService.Companion.locationService

class BaseApplication : Application() {
    lateinit var locationService1: LocationService1
        private set



    override fun onCreate() {
        super.onCreate()
        // Initialize location service when app starts
        locationService1 = LocationService1(applicationContext)
        locationService.init(applicationContext)

    }
}