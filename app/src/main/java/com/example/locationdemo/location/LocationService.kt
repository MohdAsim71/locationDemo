package com.example.locationdemo.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import androidx.core.app.ActivityCompat


class LocationService : LocationListener {

    private var mContext: Context? = null
    private var mLastLocationUpdated: Long = 0
    private var mLocationRequest: LocationRequest? = null
    private var mLastLocation: Location? = null
    @Volatile
    private var isFindingLocation = false
    private var mLocationServiceListeners: MutableSet<LocationServiceListener>? = null
    private var mActivity: Activity? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    companion object {
        private const val LOCATION_CHANGE_DIFF: Long = 0 /*5 * 60 * 1000*/
        val locationService by lazy { LocationService() }
        private var sLocationManager: LocationManager? = null
    }

    @Synchronized
    fun init(context: Context?) {
        mContext = context
        mLastLocationUpdated = 0
        mLastLocation = null
        isFindingLocation = false
        mLocationServiceListeners = HashSet()
       Log.e("LocationService", "init")

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.e("LocationService", "live updated location ${location.latitude}  ${location.longitude}")
                    onLocationChanged(location)
                }
            }
        }
    }

    fun setLastLocationUpdated(lastLocationUpdated: Long) {
        mLastLocationUpdated = lastLocationUpdated
    }


    private fun createLocationRequestLowAcc() {
        mLocationRequest = LocationRequest.create().apply {
            interval = 700
            fastestInterval = 50
            priority = PRIORITY_HIGH_ACCURACY
        }
    }

    @Synchronized
    fun registerAndStart(
        activity: Activity?,
        locationServiceListener: LocationServiceListener,
        mRequestCurrentLocationOnly: Boolean
    ) {
        sLocationManager = mContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mActivity = activity

        createLocationRequestLowAcc()
        mLocationServiceListeners?.add(locationServiceListener)

        start(mRequestCurrentLocationOnly)
    }

    @Synchronized
    fun unregister(locationServiceListener: LocationServiceListener) {
        mLocationServiceListeners?.remove(locationServiceListener)
        stop()
    }

    @Synchronized
    fun start(mRequestCurrentLocationOnly: Boolean) {
        Log.e("LocationService", "start $mLastLocationUpdated")
        if (System.currentTimeMillis() - mLastLocationUpdated < LOCATION_CHANGE_DIFF) {
            notifyListeners(
                LocationEvent(
                    LocationEvent.LocationResultStatus.LOCATION,
                    mLastLocation,
                    null
                )
            )
            return
        }
        if (!isFindingLocation) {
            isFindingLocation = true
            getLocation(mRequestCurrentLocationOnly)
        }
    }

    private fun getLocation(mRequestCurrentLocationOnly: Boolean) {
        if (mActivity == null) return

        if (ActivityCompat.checkSelfPermission(
                mActivity!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                mActivity!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity!!)
        if(!mRequestCurrentLocationOnly){
            getLastLocation()
        }else{
            startLocationUpdates()
            Handler(Looper.getMainLooper()).postDelayed({
                onTimeOut()
            }, 11000)
        }

    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient?.lastLocation?.addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                onLocationChanged(lastLocation)
                Log.e("LocationService", "lastLocation:: $lastLocation")

            } else {
                startLocationUpdates()
                Handler(Looper.getMainLooper()).postDelayed({
                    onTimeOut()
                }, 11000)

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback?.let {
            mLocationRequest?.let { it1 ->
                fusedLocationClient?.requestLocationUpdates(
                    it1,
                    it,
                    Looper.getMainLooper()
                )
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            Log.e("LocationService", "post:: $mLastLocationUpdated")

            if (mLastLocationUpdated == 0L && mLastLocation == null) {
                Log.e("LocationService", "lastoption:: $mLastLocationUpdated")

                getLastLocation()
            }
        }, 11000)
    }

    @Synchronized
    fun stop() {
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) };
        isFindingLocation = false
    }

    @Synchronized
    override fun onLocationChanged(location: Location) {
        mLastLocation = location
        Log.e("LocationService", "onLocationChanged $location")
        mLastLocationUpdated = System.currentTimeMillis()

        notifyListeners(
            LocationEvent(
                LocationEvent.LocationResultStatus.LOCATION,
                mLastLocation,
                null
            )
        )
        stop()
    }

    @Synchronized
    fun onTimeOut() {
        if (isFindingLocation) {
            notifyListeners(
                LocationEvent(
                    LocationEvent.LocationResultStatus.CONNECTION_TIMEOUT,
                    mLastLocation,
                    null
                )
            )
            stop()
        }
    }

    interface LocationServiceListener {
        fun onLocationServiceUpdate(locationEvent: LocationEvent?)
    }

    private fun notifyListeners(locationEvent: LocationEvent) {
        for (locationServiceListener in mLocationServiceListeners!!) {
            val handler = Handler(Looper.getMainLooper())
            handler.post { locationServiceListener.onLocationServiceUpdate(locationEvent) }
        }
        mLocationServiceListeners!!.clear()
    }
}