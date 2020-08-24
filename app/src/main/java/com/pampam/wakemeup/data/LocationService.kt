package com.pampam.wakemeup.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.ui.MainActivity
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    private val locationRepository: LocationRepository by inject()
    private val sessionRepository: SessionRepository by inject()

    private lateinit var hasLocationPermissionObserver: Observer<Boolean>

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var serviceRunningInForeground = false
    private lateinit var notificationManager: NotificationManager

    private val localBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        Log.d(LocationService::class.simpleName, "onCreate()")

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest().apply {
            interval = TimeUnit.SECONDS.toMillis(10)
            fastestInterval = TimeUnit.SECONDS.toMillis(5)
            maxWaitTime = TimeUnit.SECONDS.toMillis(0)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)

                Log.d(this::class.simpleName, "onLocationResult(): $result")

                locationRepository.location.value = result.lastLocation
            }

            override fun onLocationAvailability(result: LocationAvailability) {
                super.onLocationAvailability(result)

                Log.d(this::class.simpleName, "onLocationAvailability(): $result")

                locationRepository.isLocationAvailable.value = result.isLocationAvailable
            }
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        hasLocationPermissionObserver = Observer { listen ->
            if (listen) {
                subscribeToLocation()
            } else {
                unsubscribeToLocation()
            }
        }
        locationRepository.hasLocationPermission.observeForever(hasLocationPermissionObserver)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        Log.d(LocationService::class.simpleName, "onStartCommand()")

        val cancelLocationTrackingFromNotification = intent.getBooleanExtra(
            EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false
        )
        if (cancelLocationTrackingFromNotification) {
            sessionRepository.currentSession.value = null
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {

        Log.d(LocationService::class.simpleName, "onBind()")

        stopForeground(true)
        serviceRunningInForeground = false
        return localBinder
    }

    override fun onRebind(intent: Intent?) {

        Log.d(LocationService::class.simpleName, "onRebind()")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        serviceRunningInForeground = false
    }

    override fun onUnbind(intent: Intent?): Boolean {

        Log.d(LocationService::class.simpleName, "onUnbind()")

        val session = sessionRepository.currentSession.value
        if (session != null && session.status == SessionStatus.Active) {
            Log.d(LocationService::class.simpleName, "Start foreground service")

            val notification = generateNotification()
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        } else {
            stopSelf()
        }

        return true
    }

    override fun onDestroy() {

        Log.d(LocationService::class.simpleName, "onDestroy()")

        locationRepository.hasLocationPermission.removeObserver(hasLocationPermissionObserver)
        unsubscribeToLocation()
    }

    private fun subscribeToLocation() {

        Log.d(LocationService::class.simpleName, "subscribeToLocation()")

        startService(Intent(applicationContext, LocationService::class.java))
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.myLooper()
            )
        } catch (e: SecurityException) {
            Log.e(LocationService::class.simpleName, "Lost location permission $e")
            locationRepository.hasLocationPermission.value = false
        }
    }

    private fun unsubscribeToLocation() {

        Log.d(LocationService::class.simpleName, "unsubscribeToLocation()")

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }

    private fun generateNotification(): Notification {

        Log.d(LocationService::class.simpleName, "generateNotification()")

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

        val launchActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP.or(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val cancelIntent = Intent(this, LocationService::class.java)
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
        internal val service: LocationService
            get() = this@LocationService
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

