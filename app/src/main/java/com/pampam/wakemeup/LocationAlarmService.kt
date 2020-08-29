package com.pampam.wakemeup

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.google.maps.android.SphericalUtil
import com.pampam.wakemeup.data.LocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.extensions.toLatLng
import com.pampam.wakemeup.ui.MainActivity
import com.pampam.wakemeup.ui.alarm.AlarmActivity
import com.pampam.wakemeup.utils.WeakLocationCallback
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class LocationAlarmServiceBinder : Binder()

class LocationAlarmService : Service() {
    private val locationRepository: LocationRepository by inject()
    private val sessionRepository: SessionRepository by inject()

    private lateinit var hasLocationPermissionObserver: Observer<Boolean>

    private lateinit var sessionObserver: Observer<Session?>

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var weakLocationCallback: WeakLocationCallback

    private lateinit var mediaPlayer: MediaPlayer

    private var serviceRunningInForeground = false
    private lateinit var notificationManager: NotificationManagerCompat
    private var notificationCategory = ""

    private var serviceBound = false
    private val localBinder = LocationAlarmServiceBinder()

    override fun onCreate() {
        super.onCreate()
        Log.d(LocationAlarmService::class.simpleName, "onCreate()")

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest().apply {
            interval = TimeUnit.SECONDS.toMillis(5)
            fastestInterval = TimeUnit.SECONDS.toMillis(2)
            maxWaitTime = TimeUnit.SECONDS.toMillis(0)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)

                Log.d(this::class.simpleName, "onLocationResult(): $result")

                locationRepository.location.value = result.lastLocation

                if (serviceRunningInForeground) {
                    updateForeground()
                }
            }

            override fun onLocationAvailability(result: LocationAvailability) {
                super.onLocationAvailability(result)

                Log.d(this::class.simpleName, "onLocationAvailability(): $result")

                locationRepository.isLocationAvailable.value = result.isLocationAvailable
            }
        }
        weakLocationCallback =
            WeakLocationCallback(locationCallback)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            setDataSource(
                applicationContext,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )
            isLooping = true
            prepare()
        }

        notificationManager = NotificationManagerCompat.from(applicationContext)

        hasLocationPermissionObserver = Observer { hasLocationPermission ->
            if (hasLocationPermission) {
                subscribeToLocation()
            } else {
                unsubscribeToLocation()
            }
        }
        locationRepository.hasLocationPermission.observeForever(hasLocationPermissionObserver)

        sessionObserver = Observer { session: Session? ->
            if (session?.status == SessionStatus.Active) {
                updateForeground()
            } else {
                if (serviceRunningInForeground) {
                    stopForeground()
                    if (!serviceBound) {
                        stopSelf()
                    }
                }
            }
        }
        sessionRepository.currentSession.observeForever(sessionObserver)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        Log.d(LocationAlarmService::class.simpleName, "onStartCommand()")

        val cancelLocationTrackingFromNotification = intent.getBooleanExtra(
            EXTRA_CANCEL_SESSION_FROM_NOTIFICATION, false
        )
        if (cancelLocationTrackingFromNotification) {
            sessionRepository.currentSession.value = null
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(LocationAlarmService::class.simpleName, "onBind()")

        serviceBound = true

        return localBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(LocationAlarmService::class.simpleName, "onUnbind()")

        serviceBound = false

        if (!serviceRunningInForeground) {
            stopSelf()
        }

        return true
    }

    override fun onRebind(intent: Intent?) {
        Log.d(LocationAlarmService::class.simpleName, "onRebind()")

        serviceBound = true
    }

    override fun onDestroy() {
        Log.d(LocationAlarmService::class.simpleName, "onDestroy()")

        stopForeground()
        mediaPlayer.release()
        unsubscribeToLocation()
        locationRepository.hasLocationPermission.removeObserver(hasLocationPermissionObserver)
        sessionRepository.currentSession.removeObserver(sessionObserver)
    }

    private fun startForeground() {
        Log.d(LocationAlarmService::class.simpleName, "startForeground()")

        val notification = generateNotification()
        notificationCategory = notification.category
        if (notificationCategory == NotificationCompat.CATEGORY_ALARM) {
            startForeground(NOTIFICATION_ALARM_ID, notification)
            mediaPlayer.prepare()
            mediaPlayer.start()

        } else if (notificationCategory == NotificationCompat.CATEGORY_NAVIGATION) {
            startForeground(NOTIFICATION_INFO_ID, notification)
            mediaPlayer.stop()
        }

        serviceRunningInForeground = true
    }

    private fun stopForeground() {
        Log.d(LocationAlarmService::class.simpleName, "stopForeground()")

        mediaPlayer.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }

        serviceRunningInForeground = false
    }

    private fun updateForeground() {
        Log.d(LocationAlarmService::class.simpleName, "updateForeground()")

        val notification = generateNotification()
        if (notification.category != notificationCategory || !serviceRunningInForeground) {
            stopForeground()
            startForeground()
        } else {
            notificationManager.notify(
                if (notificationCategory == NotificationCompat.CATEGORY_ALARM)
                    NOTIFICATION_ALARM_ID
                else
                    NOTIFICATION_INFO_ID,

                notification
            )
        }
    }

    private fun subscribeToLocation() {
        Log.d(LocationAlarmService::class.simpleName, "subscribeToLocation()")

        startService(Intent(applicationContext, LocationAlarmService::class.java))
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, weakLocationCallback, Looper.myLooper()
            ).addOnCompleteListener {
                Log.d(LocationAlarmService::class.simpleName, "requestLocationUpdates() complete")
            }.addOnFailureListener {
                Log.e(
                    LocationAlarmService::class.simpleName,
                    "requestLocationUpdates() failure: $it"
                )
            }
        } catch (e: SecurityException) {
            Log.e(LocationAlarmService::class.simpleName, "Lost location permission $e")
            locationRepository.hasLocationPermission.value = false
        }
    }

    private fun unsubscribeToLocation() {
        Log.d(LocationAlarmService::class.simpleName, "unsubscribeToLocation()")

        fusedLocationProviderClient.removeLocationUpdates(weakLocationCallback)
            .addOnSuccessListener {
                Log.d(LocationAlarmService::class.simpleName, "removeLocationUpdates() complete")
            }.addOnFailureListener {
                Log.e(
                    LocationAlarmService::class.simpleName,
                    "removeLocationUpdates() failure: $it"
                )
            }
    }

    private fun generateInfoNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_INFO_ID,
                getString(R.string.notification_channel_info_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val session = sessionRepository.currentSession.value!!
        val details = session.details!!
        val title = details.primaryText

        val destinationLocation = details.latLng
        val currentLocation = locationRepository.location.value!!.toLatLng()
        val distance = SphericalUtil.computeDistanceBetween(destinationLocation, currentLocation)
        val text = String.format(
            getString(R.string.notification_main_text),
            distance,
            getString(R.string.meters_short)
        )

        val contentIntent = {
            val intent = Intent(this, MainActivity::class.java)
            PendingIntent.getActivity(this, 0, intent, 0)
        }()

        val actionCancelIntent = {
            val cancelIntent = Intent(this, LocationAlarmService::class.java).apply {
                putExtra(EXTRA_CANCEL_SESSION_FROM_NOTIFICATION, true)
            }
            PendingIntent.getService(
                this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }()

        val builder = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_INFO_ID
        ).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(title)
            setContentText(text)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            priority = NotificationCompat.PRIORITY_LOW
            setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            setContentIntent(contentIntent)
            setOngoing(true)
            setShowWhen(false)
            setDefaults(NotificationCompat.DEFAULT_ALL)
            addAction(
                R.drawable.ic_baseline_cancel_24,
                getString(R.string.notification_cancel),
                actionCancelIntent
            )
        }
        return builder.build()
    }

    private fun generateAlertNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ALARM_ID,
                getString(R.string.notification_channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val session = sessionRepository.currentSession.value!!
        val details = session.details!!
        val title = details.primaryText
        val text = getString(R.string.notification_near)

        val fullScreenIntent = {
            val intent = Intent(this, AlarmActivity::class.java)
            PendingIntent.getActivity(this, 0, intent, 0)
        }()

        val actionCancelIntent = {
            val cancelIntent = Intent(this, LocationAlarmService::class.java).apply {
                putExtra(EXTRA_CANCEL_SESSION_FROM_NOTIFICATION, true)
            }
            PendingIntent.getService(
                this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }()

        val builder = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ALARM_ID
        ).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(title)
            setContentText(text)
            priority = NotificationCompat.PRIORITY_HIGH
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setOnlyAlertOnce(true)
            setShowWhen(false)
            setOngoing(true)
            addAction(
                R.drawable.ic_baseline_cancel_24,
                getString(R.string.notification_cancel),
                actionCancelIntent
            )
            setFullScreenIntent(fullScreenIntent, true)
        }
        return builder.build()
    }

    private fun generateNotification(): Notification {
        Log.d(LocationAlarmService::class.simpleName, "generateNotification()")

        val session = sessionRepository.currentSession.value!!
        val details = session.details!!
        val destinationLocation = details.latLng

        val currentLocation = {
            val location = locationRepository.location.value!!
            location.toLatLng()
        }()

        val distance = SphericalUtil.computeDistanceBetween(destinationLocation, currentLocation)

        return if (distance > session.range.toMeters()) {
            generateInfoNotification()
        } else {
            generateAlertNotification()
        }
    }


    companion object {
        private const val PACKAGE_NAME = "com.pampam.wakemeup"

        private const val EXTRA_CANCEL_SESSION_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_INFO_ID = 373592748
        private const val NOTIFICATION_CHANNEL_INFO_ID = "wake_me_up_channel_info"

        private const val NOTIFICATION_ALARM_ID = 373592749
        private const val NOTIFICATION_CHANNEL_ALARM_ID = "wake_me_up_channel_alarm"
    }
}

