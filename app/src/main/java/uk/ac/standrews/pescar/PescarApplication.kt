package uk.ac.standrews.pescar

import android.app.Application
import android.content.Intent
import android.widget.Toast
import uk.ac.standrews.pescar.track.TrackService
import java.util.*

/**
 * Extends [android.app.Application] to handle tracking independently of any activity
 *
 * @constructor creates an instance with tracking location off
 */
class PescarApplication : Application() {

    val VERSION: String = "0.1"
    val TIME_ZONE: TimeZone = TimeZone.getTimeZone("TIME_ZONE")

    var trackingLocation: Boolean = false

    /**
     * Starts location tracking
     */
    fun startTrackingLocation() {
        startService(Intent(this,TrackService::class.java))
        trackingLocation = true
    }

    /**
     * Stops location tracking, with user notification via Toast
     */
    fun stopTrackingLocation() {
        stopService(Intent(this,TrackService::class.java))
        Toast.makeText(baseContext, R.string.stopped_tracking_location, Toast.LENGTH_LONG).show()
        trackingLocation = false
    }

}
