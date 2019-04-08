package uk.ac.standrews.pescar

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import androidx.test.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import uk.ac.standrews.pescar.track.TrackService
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TrackServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()
    lateinit var locationManager: LocationManager
    var locationProvider = LocationManager.GPS_PROVIDER
    lateinit var db: AppDatabase

    @Before
    fun setup() {
        locationManager = InstrumentationRegistry.getTargetContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.addTestProvider(locationProvider, false, false, false,
            false, true, true, true, 0,
            5)
        locationManager.setTestProviderEnabled(locationProvider, true)
        db = AppDatabase.getAppDataBase(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testLocationLogging() {
        (InstrumentationRegistry.getTargetContext().applicationContext as PescarApplication).startTrackingLocation()
        var l = Location(locationProvider)
        val lat = (Math.random() - 0.5) * 2 * 90
        val lon = (Math.random() - 0.5) * 2 * 180
        val time = System.currentTimeMillis()
        val acc = (Math.random()  * 1000).toFloat()
        l.latitude = lat
        l.longitude = lon
        l.time = time
        l.accuracy = acc
        l.elapsedRealtimeNanos = System.nanoTime()
        locationManager.setTestProviderLocation(locationProvider, l)
        TimeUnit.SECONDS.sleep(10)
        Executors.newSingleThreadExecutor().execute {
            var pos = db.trackDao().getLastPosition()
            assert(lat == pos.latitude)
            assert(lon == pos.longitude)
            assert(time == pos.timestamp.time)
            assert(acc == pos.accuracy)
        }
    }

}
