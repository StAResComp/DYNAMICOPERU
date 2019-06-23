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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
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

    private val _locationInterval: Long = 120000
    private val _locationDistance: Float = 5f
    private val _trackingNotificationId: Int = 568
    private val _pescarTrackingChannel: String = "pescar_tracking_channel"
    private val _provider: String = LocationManager.GPS_PROVIDER
    private val _tag: String = "TRACK"

    //These need to be set in onCreate
    private lateinit var locationManager: LocationManager
    private lateinit var notificationManager: NotificationManager

    private val trackBinder: TrackBinder = TrackBinder()

    var locationListener = object : LocationListener {

        var lastLocation: Location = Location(_provider)

        override fun onLocationChanged(location: Location?) {
            lastLocation.set(location)
            this.writeLocation()
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
            provider: String, status: Int, extras: Bundle) {
        }

        fun writeLocation() {
            if (lastLocation.latitude != 0.0 || lastLocation.longitude != 0.0 || lastLocation.accuracy != 0.0f) {
                val db = AppDatabase.getAppDataBase(
                    this@TrackService.applicationContext
                )
                Executors.newSingleThreadExecutor().execute {
                    val cal = Calendar.getInstance()
                    val pos = Position(
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
                _provider,
                _locationInterval,
                _locationDistance,
                locationListener
            )
        }
        catch(e: SecurityException) {
            Log.e(_tag, "Security exception requesting location updates", e)
        }
        catch(e: IllegalArgumentException) {
            Log.e(_tag, "Error requesting location updates", e)
        }

        startForeground(_trackingNotificationId, this.getNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun getNotification(): Notification {
        val notificationIntent = Intent(
            this.applicationContext, TodayActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this.applicationContext, 0,
            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    _pescarTrackingChannel,
                    getString(
                        R.string.tracking_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            return Notification.Builder(this, _pescarTrackingChannel)
                .setContentTitle(getString(R.string.tracking_notification_title))
                .setContentText(getString(R.string.tracking_notification_text))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setTicker(getString(R.string.tracking_notification_text))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build()
        }
        else {
            return NotificationCompat.Builder(this, _pescarTrackingChannel)
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
            Log.e(_tag, "Error destroying service", e)
        }
    }

    inner class TrackBinder : Binder() {
        fun getService(): TrackService {
            return this@TrackService
        }
    }
}
