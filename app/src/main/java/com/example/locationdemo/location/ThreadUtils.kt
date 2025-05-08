package com.example.locationdemo.location

import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ThreadUtils {
    private val NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()
    @JvmStatic
    val defaultExecutorService: ExecutorService = ThreadPoolExecutor(
        NUMBER_OF_CORES,  // Initial
        // pool
        // size
        NUMBER_OF_CORES,  // Max
        // pool
        // size
        1, TimeUnit.SECONDS,
        LinkedBlockingQueue()
    )
        get() {
            Log.e("Number Of Cores", "" + NUMBER_OF_CORES)
            return field
        }
}