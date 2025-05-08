package com.example.locationdemo.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.example.locationdemo.R
import com.example.locationdemo.location.LocationService.Companion.locationService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY
import java.util.concurrent.Callable
import androidx.core.app.ActivityCompat

class LocationTask : Callable<Void?>, LocationService.LocationServiceListener {
    private var mOnLocationReceivedListener: OnLocationReceived
    private val mActivity: Activity?
    private var mRequestCurrentLocationOnly = false
    private var mDontAskToEnableDeviceLocation = false
    val REQUEST_CHECK_SETTINGS: Int = 799

    constructor(activity: Activity?, dontAskToEnableDeviceLocation: Boolean, listener: OnLocationReceived) {
        mOnLocationReceivedListener = listener
        mActivity = activity
        mDontAskToEnableDeviceLocation = dontAskToEnableDeviceLocation
    }

    constructor(activity: Activity?, listener: OnLocationReceived, requestCurrentLocation: Boolean) {
        mOnLocationReceivedListener = listener
        mActivity = activity
        mRequestCurrentLocationOnly = requestCurrentLocation
        mDontAskToEnableDeviceLocation = false
    }

    override fun call(): Void? {
        if (mDontAskToEnableDeviceLocation) {
            val locationManager = mActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            val isLocationEnabled = (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false) ||
                    (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false)

            if (!isLocationEnabled) {
                onLocationServiceUpdate(
                    LocationEvent(
                        LocationEvent.LocationResultStatus.PERMISSION_DENIED,
                        null, null
                    )
                )

                return null
            }
        }

        val highAccuracy = LocationRequest.create()
        highAccuracy.interval = 2000
        highAccuracy.fastestInterval = 100
        highAccuracy.priority = PRIORITY_BALANCED_POWER_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(highAccuracy)
        val result = LocationServices.getSettingsClient(mActivity!!).checkLocationSettings(builder.build())

        result.addOnCompleteListener { task ->
            try {
                task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.
                locationService.registerAndStart(mActivity, this@LocationTask, mRequestCurrentLocationOnly)
            } catch (exception: ApiException) {
               Log.e("LocationService", "Error received ${exception.statusCode}")

                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->                             // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(mActivity, REQUEST_CHECK_SETTINGS)
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                            onLocationServiceUpdate(LocationEvent(LocationEvent.LocationResultStatus.CONNECTION_TIMEOUT, null, null))
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                            onLocationServiceUpdate(LocationEvent(LocationEvent.LocationResultStatus.CONNECTION_TIMEOUT, null, null))
                        }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
                        onLocationServiceUpdate(
                            LocationEvent(LocationEvent.LocationResultStatus.CONNECTION_TIMEOUT, null, null)
                        )

                    LocationSettingsStatusCodes.CANCELED -> onLocationServiceUpdate(
                        LocationEvent(
                            LocationEvent.LocationResultStatus.PERMISSION_DENIED,
                            null,
                            null
                        )
                    )

                    else -> onLocationServiceUpdate(LocationEvent(LocationEvent.LocationResultStatus.CONNECTION_TIMEOUT, null, null))
                }
            }
        }
        return null
    }

    override fun onLocationServiceUpdate(locationEvent: LocationEvent?) {
        val locationResultStatus = locationEvent!!.locationResultStatus

        locationService.unregister(this)
        locationService.stop()

        val mLocation: Location?
        when (locationResultStatus) {
            LocationEvent.LocationResultStatus.LOCATION -> {
                mLocation = locationEvent.location

                if (mLocation != null) {
                    mOnLocationReceivedListener.onReceived(mLocation)

                } else sendErrorResponse("Something seems to have gone wrong. Please try again later")

            }

            LocationEvent.LocationResultStatus.CONNECTION_RESULT -> {
                sendErrorResponse("There is some issue with your Google Play Services.\\n\\n Please update/install Google Play Services.")
            }

            LocationEvent.LocationResultStatus.CONNECTION_SUSPENDED -> {
                sendErrorResponse("There is some issue with GPS connectivity.\\n\\nPlease try again later.")
            }

            LocationEvent.LocationResultStatus.CONNECTION_TIMEOUT -> {
                mLocation = locationEvent.location
                if (mLocation != null)
                    mOnLocationReceivedListener.onReceived(mLocation)
                else {
                    if (mActivity == null) {
                        sendErrorResponse("We are unable to determine your location. Please try again later.")
                        return
                    }

                    val locationManager = mActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                    val networkLocation =
                        ActivityCompat.checkSelfPermission(
                            mActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED ||
                                ActivityCompat.checkSelfPermission(
                                    mActivity,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED

                    if (networkLocation) {
                        val mLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                        if (mLocation != null) {
                            mOnLocationReceivedListener.onReceived(mLocation)
                        } else
                            sendErrorResponse("Something seems to have gone wrong. Please try again later")
                    } else
                        sendErrorResponse("We are unable to determine your location. Please try again later.")
                }
            }

            LocationEvent.LocationResultStatus.PERMISSION_DENIED -> {
                sendErrorResponse("Location permission needed. Please allow in App Settings for additional functionality.")
            }

            else -> {
                sendErrorResponse("Something seems to have gone wrong. Please try again later")
            }
        }
    }

    private fun sendErrorResponse(errorMessage: String) {
        mOnLocationReceivedListener.onError(errorMessage)
    }

    interface OnLocationReceived {
        fun onReceived(location: Location?)
        fun onError(errorMessage: String?)
    }
}