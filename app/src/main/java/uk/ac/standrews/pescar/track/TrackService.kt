package uk.ac.standrews.pescar.track

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.util.Log
import uk.ac.standrews.pescar.AppDatabase
import uk.ac.standrews.pescar.R
import uk.ac.standrews.pescar.TodayActivity
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.Executors

/**
 * Listens for location changes and writes them to the database
 */
class TrackService : Service() {

    private val LOCATION_INTERVAL: Long = 30000
    private val LOCATION_DISTANCE: Float = 5f
    private val TRACKING_NOTIFICATION_ID: Int = 568
    private val PESCAR_TRACKING_CHANNEL: String = "pescar_tracking_channel"
    private val PROVIDER: String = LocationManager.GPS_PROVIDER
    private val TAG: String = "TRACK"

    //These need to be set in onCreate
    private lateinit var locationManager: LocationManager
    private lateinit var notificationManager: NotificationManager

    private val trackBinder: TrackBinder = TrackBinder()

    private val locationListener = object : LocationListener {

        var lastLocation: Location = Location(PROVIDER)

        override fun onLocationChanged(location: Location?) {
            if(location?.accuracy != 0f) {
                lastLocation.set(location)
                writeLocation()
            }
        }

        //Attempt to persist location when provider is disabled
        override fun onProviderDisabled(provider: String) {
            this.writeLocation()
        }

        //Attempt to persist location immediately when provider is enabled
        override fun onProviderEnabled(provider: String) {
            this.writeLocation()
        }

        override fun onStatusChanged(
            provider: String, status: Int, extras: Bundle) {}

        private fun writeLocation() {
            if (lastLocation.latitude != 0.0 || lastLocation.longitude != 0.0 || lastLocation.accuracy != 0.0f) {
                val db = AppDatabase.getAppDataBase(
                    this@TrackService.applicationContext
                )
                Executors.newSingleThreadExecutor().execute {
                    var cal = Calendar.getInstance()
                    var pos = Position(
                        latitude = lastLocation.latitude,
                        longitude = lastLocation.longitude,
                        accuracy = lastLocation.accuracy,
                        timestamp = cal.time
                    )
                    db.trackDao().insertPosition(pos)
                }
            }
        }

    }

    /**
     * Runs on service creation. Initialises managers, requests location updates and notifies that location tracking has
     * started
     */
    override fun onCreate() {
        super.onCreate()

        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE)
                as LocationManager
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        try {
            locationManager.requestLocationUpdates(
                PROVIDER,
                LOCATION_INTERVAL,
                LOCATION_DISTANCE,
                locationListener
            )
        }
        catch(e: java.lang.SecurityException) {
            Log.e(TAG, "Security exception requesting location updates", e)
        }
        catch(e: IllegalArgumentException) {
            Log.e(TAG, "Error requesting location updates", e)
        }

        startForeground(TRACKING_NOTIFICATION_ID, this.getNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun getNotification(): Notification {
        var notificationIntent = Intent(
            this.applicationContext, TodayActivity::class.java)
        var pendingIntent = PendingIntent.getActivity(
            this.applicationContext, 0,
            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    PESCAR_TRACKING_CHANNEL,
                    getString(
                        R.string.tracking_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            return Notification.Builder(this, PESCAR_TRACKING_CHANNEL)
                .setContentTitle(getString(R.string.tracking_notification_title))
                .setContentText(getString(R.string.tracking_notification_text))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setTicker(getString(R.string.tracking_notification_text))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build()
        }
        else {
            return NotificationCompat.Builder(this, PESCAR_TRACKING_CHANNEL)
                .setContentTitle(getString(R.string.tracking_notification_title))
                .setContentText(getString(R.string.tracking_notification_text))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setTicker(getString(R.string.tracking_notification_text))
                .build()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return trackBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
            locationManager.removeUpdates(locationListener)
        }
        catch (e: Exception) {
            Log.e(TAG, "Error destroying service", e)
        }
    }

    inner class TrackBinder : Binder() {
        fun getService(): TrackService {
            return this@TrackService
        }
    }
}
