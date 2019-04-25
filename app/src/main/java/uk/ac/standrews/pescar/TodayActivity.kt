package uk.ac.standrews.pescar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_today.*
import android.widget.Switch
import uk.ac.standrews.pescar.fishing.FishingDao

/**
 * Home Activity. Where users toggle tracking and view/enter details of today's catch
 */
class TodayActivity : AppCompatActivity() {

    //Need to be bound to widget in onCreate
    lateinit var tracker: Switch
    lateinit var mapButton: Button
    lateinit var tows: Array<EditText>
    lateinit var species: Array<EditText>
    lateinit var submitButton: Button
    lateinit var fishingDao: FishingDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Bind to layout
        setContentView(R.layout.activity_today)

        //Bind tracker switch to widget and set listener
        tracker = findViewById(R.id.tracker)
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

        mapButton = findViewById(R.id.map_button)
        tows = arrayOf(
            findViewById(R.id.tow_1),
            findViewById(R.id.tow_2),
            findViewById(R.id.tow_3),
            findViewById(R.id.tow_4),
            findViewById(R.id.tow_5)
        )
        species = arrayOf(
            findViewById(R.id.species_1),
            findViewById(R.id.species_2),
            findViewById(R.id.species_3),
            findViewById(R.id.species_4)
        )
        submitButton = findViewById(R.id.submit_button)

        fishingDao = AppDatabase.getAppDataBase(this).fishingDao()

        //Navigation
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
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

    //Handle navigation
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }
}
