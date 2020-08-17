package com.pampam.wakemeup.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.Location
import com.pampam.wakemeup.data.model.LocationStatus
import com.pampam.wakemeup.ui.MainActivity
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class MyLocationService : Service() {

    private val locationRepository: MyLocationRepository by inject()
    private lateinit var shouldListenToLocationObserver: Observer<Boolean>

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var serviceRunningInForeground = false
    private lateinit var notificationManager: NotificationManager

    private var configurationChange = false

    private val localBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        Log.d(MyLocationService::class.simpleName, "onCreate()")

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest().apply {
            interval = TimeUnit.SECONDS.toMillis(5)
            fastestInterval = TimeUnit.SECONDS.toMillis(5)
            maxWaitTime = TimeUnit.SECONDS.toMillis(5)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)

                Log.d(this::class.simpleName, "onLocationResult(): $result")

                result.lastLocation?.apply {

                    val myLocationEntity =
                        Location(
                            status =
                            if (locationRepository.myLastLocation.value == null)
                                LocationStatus.FirstAvailable
                            else
                                LocationStatus.Available,
                            latLng = LatLng(latitude, longitude)
                        )

                    locationRepository.myLastLocation.value = myLocationEntity
                }
            }

            override fun onLocationAvailability(result: LocationAvailability) {
                super.onLocationAvailability(result)

                Log.d(this::class.simpleName, "onLocationAvailability(): $result")

                if (!result.isLocationAvailable) {

                    val myLastLocation = locationRepository.myLastLocation.value
                    val myLocationEntity = Location(
                        status = LocationStatus.Unavailable,
                        latLng = myLastLocation?.latLng
                    )

                    locationRepository.myLastLocation.value = myLocationEntity
                }
            }
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        shouldListenToLocationObserver = Observer { listen ->

            if (listen) {
                subscribeToLocation()
            } else {
                unsubscribeToLocation()
            }
        }
        locationRepository.isListenToLocation.observeForever(shouldListenToLocationObserver)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        Log.d(MyLocationService::class.simpleName, "onStartCommand()")

        val cancelLocationTrackingFromNotification = intent.getBooleanExtra(
            EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false
        )
        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocation()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {

        Log.d(MyLocationService::class.simpleName, "onBind()")

        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent?) {

        Log.d(MyLocationService::class.simpleName, "onRebind()")

        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
    }

    override fun onUnbind(intent: Intent?): Boolean {

        Log.d(MyLocationService::class.simpleName, "onUnbind()")

        if (!configurationChange) {
            Log.d(MyLocationService::class.simpleName, "Start foreground service")

            val notification = generateNotification()
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        return true
    }

    override fun onDestroy() {

        Log.d(MyLocationService::class.simpleName, "onDestroy()")

        locationRepository.isListenToLocation.removeObserver(shouldListenToLocationObserver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Log.d(MyLocationService::class.simpleName, "onConfigurationChanged()")

        configurationChange = true
    }

    private fun subscribeToLocation() {

        Log.d(MyLocationService::class.simpleName, "subscribeToLocation()")

        startService(Intent(applicationContext, MyLocationService::class.java))
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.myLooper()
            )
        } catch (e: SecurityException) {
            Log.e(MyLocationService::class.simpleName, "Lost location permission $e")
        }
    }

    private fun unsubscribeToLocation() {

        Log.d(MyLocationService::class.simpleName, "unsubscribeToLocation()")

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }

    private fun generateNotification(): Notification {

        Log.d(MyLocationService::class.simpleName, "generateNotification()")

        val mainNotificationText = getString(R.string.notification_main_text)
        val titleText = getString(R.string.app_name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_LOW
            )

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, MyLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0
        )

        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.drawable.ic_baseline_access_time_24)
            .setDefaults(0)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_baseline_launch_24,
                getString(R.string.notification_open),
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_baseline_cancel_24,
                getString(R.string.notification_close),
                servicePendingIntent
            )
            .build()
    }

    inner class LocalBinder : Binder() {
        internal val service: MyLocationService
            get() = this@MyLocationService
    }

    companion object {
        private const val PACKAGE_NAME = "com.pampam.wakemeup"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "wake_me_up_channel_01"
    }
}

