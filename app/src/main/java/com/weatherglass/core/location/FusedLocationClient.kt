package com.weatherglass.core.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.weatherglass.core.model.LocationPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class FusedLocationClient @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationClient {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationPoint? = suspendCancellableCoroutine { cont ->
        val provider = LocationServices.getFusedLocationProviderClient(context)
        provider.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    cont.resume(null)
                } else {
                    cont.resume(LocationPoint(location.latitude, location.longitude))
                }
            }
            .addOnFailureListener { cont.resume(null) }
    }
}
