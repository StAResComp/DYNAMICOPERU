package uk.ac.standrews.pescar

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import uk.ac.standrews.pescar.track.Position
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.ceil

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat
        this.title = "${df.format(Date(intent.getLongExtra("started_at",0)))} - ${df.format(Date(intent.getLongExtra("finished_at",0)))}"
        val actionBar = this.supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val startedAt = Date(intent.getLongExtra("started_at", 0))
        val finishedAt = Date(intent.getLongExtra("finished_at", Date().time))
        val trackDao = AppDatabase.getAppDataBase(this).trackDao()
        val c = Callable {
            val firstPos = trackDao.getFirstPositionForPeriod(startedAt, finishedAt)
            val secondPos = trackDao.getLastPositionForPeriod(startedAt, finishedAt)
            if (firstPos != null && secondPos != null) {
                val diff = secondPos.id - firstPos.id
                val max_points = 100
                if (diff <= max_points) {
                    trackDao.getPositionsForPeriod(startedAt, finishedAt)
                }
                else {
                    val interval = (ceil(diff.toDouble()/max_points)).toInt()   
                    val ids = ArrayList<Int>()
                    for (i in firstPos.id..secondPos.id step interval) {
                        ids.add(i)
                    }
                    trackDao.getPositions(ids)
                }
            }
            else {
                ArrayList<Position>()
            }
        }
        val positions = Executors.newSingleThreadExecutor().submit(c).get()
        Log.e("MAP", positions.size.toString())

        var first = true
        positions.forEach {
            addMarkerToMap(it, first)
            first = false
        }
    }

    private fun addMarkerToMap(position: Position, first: Boolean = false) {
        mMap.addMarker(MarkerOptions().position(LatLng(position.latitude, position.longitude)).title("${String.format("%.2f", position.latitude)}, ${String.format("%.2f", position.longitude)} / ${position.timestamp.hours}:${position.timestamp.minutes}:${position.timestamp.seconds}"))
        if (first) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(position.latitude, position.longitude), 8.0f))
        }
    }
}
