package com.example.locationdemo.location

import android.location.Location
import com.google.android.gms.common.ConnectionResult

class LocationEvent(
    var locationResultStatus: LocationResultStatus?,
    var location: Location?,
    var connectionResult: ConnectionResult?
) {
    enum class LocationResultStatus {
        LOCATION, CONNECTION_RESULT, CONNECTION_SUSPENDED, CONNECTION_TIMEOUT, PERMISSION_DENIED
    }
}