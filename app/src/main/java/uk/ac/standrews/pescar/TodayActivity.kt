package uk.ac.standrews.pescar

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_today.*
import android.widget.Switch
import android.widget.TextView
import uk.ac.standrews.pescar.fishing.FishingDao
import uk.ac.standrews.pescar.fishing.Landed
import uk.ac.standrews.pescar.fishing.Tow
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Home Activity. Where users toggle tracking and view/enter details of day's catch
 */
class TodayActivity : ArchiveActivity() {

    //Need to be bound to widget in onCreate
    private lateinit var tracker: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setDayAndTime()

        //Bind to layout
        //bindView()

        setUpTracker()
    }

    override fun bindView() {
        setContentView(R.layout.activity_today)
    }

    override fun setDayAndTime() {
        this.day = (this.application as PescarApplication).getPeriodBoundaries()
        val c = Calendar.getInstance()
        c.time = day.first
        c.add(Calendar.HOUR_OF_DAY, 12)
        timestamp = c.time
    }

    private fun setUpTracker() {
        //Bind tracker switch to widget and set listener
        tracker = findViewById(R.id.tracker)
        if ((this.application as PescarApplication).trackingLocation) {
            tracker.toggle()
        }
        tracker.setOnCheckedChangeListener { _, isChecked ->
            var app = this@TodayActivity.application as PescarApplication
            if (!isChecked) {
                app.stopTrackingLocation()
            }
            else if (isChecked && ContextCompat.checkSelfPermission(
                    this@TodayActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@TodayActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 568)
            }
            else if (isChecked && ContextCompat.checkSelfPermission(this@TodayActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                app.startTrackingLocation()
            }
            else {
                tracker.toggle()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 568) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                (this@TodayActivity.application as PescarApplication).startTrackingLocation()
            }
            else {
                tracker.toggle()
            }
            return
        }
    }
}
