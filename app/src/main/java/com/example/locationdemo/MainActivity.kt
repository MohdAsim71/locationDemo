package com.example.locationdemo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.locationdemo.location.LocationTask
import com.example.locationdemo.location.ThreadUtils
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var locationService: LocationService1

    // Permission request launcher
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                getLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Approximate location access granted
                getLocation()
            }
            else -> {
                // No location access granted
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ThreadUtils.defaultExecutorService
                .submit(LocationTask(this, object : LocationTask.OnLocationReceived {
                    override fun onReceived(location: Location?) {

                        if (location != null) {
                            showLocation(location)
                        }
                    }

                    override fun onError(errorMessage: String?) {

                    }
                }, true))
        }

        // Get location service from application
    //    locationService = (application as BaseApplication).locationService

        // Check and request permissions
     //   checkLocationPermissions()

    }

    private fun checkLocationPermissions() {
        when {
            locationService.hasLocationPermission() -> {
                // Permission already granted
                getLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Explain why you need the permission
                Toast.makeText(
                    this,
                    "Location permission is needed to show nearby places",
                    Toast.LENGTH_SHORT
                ).show()
                requestLocationPermission()
            }
            else -> {
                // Directly request the permission
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun getLocation() {
        if (!locationService.isLocationEnabled()) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Get current location
            val location = locationService.getCurrentLocation()
            location?.let {
                showLocation(it)
            } ?: run {
                Toast.makeText(this@MainActivity, "Unable to get location", Toast.LENGTH_SHORT).show()
            }

            // Alternatively, get continuous updates
            /*locationService.getLocationUpdates()
                .onEach { location -> showLocation(location) }
                .launchIn(lifecycleScope)*/
        }
    }

    private fun showLocation(location: Location) {
        Toast.makeText(
            this,
            "Location: ${location.latitude}, ${location.longitude}",
            Toast.LENGTH_LONG
        ).show()

        // Update your UI with the location here
    }
}