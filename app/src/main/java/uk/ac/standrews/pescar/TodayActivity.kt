package uk.ac.standrews.pescar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.activity_today.*
import android.widget.Switch

/**
 * Home Activity. Where users toggle tracking and view/enter details of today's catch
 */
class TodayActivity : AppCompatActivity() {

    //Needs to be bound to widget in onCreate
    lateinit var tracker: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Bind to layout
        setContentView(R.layout.activity_today)

        //Bind tracker switch to widget and set listener
        tracker = findViewById(R.id.tracker)
        tracker.setOnCheckedChangeListener { _, isChecked ->
            var app = this@TodayActivity.application as PescarApplication
            if (isChecked && ContextCompat.checkSelfPermission(
                    this@TodayActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@TodayActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 568)
            }
            if (isChecked) {
                app.startTrackingLocation()
            }
            else {
                app.stopTrackingLocation()
            }
        }
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
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
