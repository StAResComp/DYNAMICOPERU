package uk.ac.standrews.pescar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_today.*
import android.widget.Switch
import android.widget.TextView
import uk.ac.standrews.pescar.fishing.FishingDao
import uk.ac.standrews.pescar.fishing.Landed
import uk.ac.standrews.pescar.fishing.Tow
import uk.ac.standrews.pescar.fishing.Trip
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Home Activity. Where users toggle tracking and view/enter details of today's catch
 */
class TodayActivity : AppCompatActivity() {

    //Need to be bound to widget in onCreate
    lateinit var tracker: Switch
    lateinit var mapButton: Button
    lateinit var tows: Array<EditText>
    lateinit var landeds: Array<Pair<TextView, EditText>>
    lateinit var tripInfo: TextView
    lateinit var fishingDao: FishingDao
    private var mostRecentTrip: Trip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fishingDao = AppDatabase.getAppDataBase(this).fishingDao()

        //Bind to layout
        setContentView(R.layout.activity_today)

        mapButton = findViewById(R.id.map_button)

        tows = arrayOf(
            findViewById(R.id.tow_1),
            findViewById(R.id.tow_2),
            findViewById(R.id.tow_3),
            findViewById(R.id.tow_4),
            findViewById(R.id.tow_5)
        )

        landeds = arrayOf(
            Pair(findViewById(R.id.species_1_label), findViewById(R.id.species_1)),
            Pair(findViewById(R.id.species_2_label), findViewById(R.id.species_2)),
            Pair(findViewById(R.id.species_3_label), findViewById(R.id.species_3)),
            Pair(findViewById(R.id.species_4_label), findViewById(R.id.species_4))
        )

        tripInfo = findViewById(R.id.trip_info)

        this.setMostRecentTrip(null)

        tows.forEach { textField ->
            textField.tag = false
            textField.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    val field = view as EditText
                    if (field.text.matches("\\d+(\\.\\d+)?".toRegex())) {
                        if (mostRecentTrip == null) {
                            checkNewTrip()
                        }
                        if (field.tag is Number) {
                            Executors.newSingleThreadExecutor().execute {
                                fishingDao.updateTow((field.tag as Int), field.text.toString().toDouble(), Date())
                            }
                        } else {
                            Executors.newSingleThreadExecutor().execute {
                                field.tag = fishingDao.insertTow(
                                    Tow(
                                        tripId = mostRecentTrip!!.id,
                                        weight = field.text.toString().toDouble(),
                                        timestamp = Date()
                                    )
                                ).toInt()
                            }
                        }
                    }
                }
            }
        }

        landeds.forEach { pair ->
            val textField = pair.second
            textField.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    val field = view as EditText
                    if (field.text.matches("\\d+(\\.\\d+)?".toRegex())) {
                        if (mostRecentTrip == null) {
                            checkNewTrip()
                        }
                        if (field.getTag(R.id.landed_id_key) is Number) {
                            Executors.newSingleThreadExecutor().execute {
                                fishingDao.updateLanded(
                                    (field.getTag(R.id.landed_id_key) as Int), field.text.toString().toDouble(), Date()
                                )
                            }
                        }
                        else {
                            Executors.newSingleThreadExecutor().execute {
                                field.setTag(
                                    R.id.landed_id_key, fishingDao.insertLanded(
                                        Landed(
                                            tripId = mostRecentTrip!!.id,
                                            weight = field.text.toString().toDouble(),
                                            timestamp = Date(),
                                            speciesId = (field.getTag(R.id.species_id_key) as Int)
                                        )
                                    ).toInt()
                                )
                            }
                        }
                    }
                }
            }
        }

        mapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        setUpTracker()

        //Navigation
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    private fun setUpTracker() {
        //Bind tracker switch to widget and set listener
        tracker = findViewById(R.id.tracker)
        tracker.setOnCheckedChangeListener { _, isChecked ->
            var app = this@TodayActivity.application as PescarApplication
            if (!isChecked) {
                app.stopTrackingLocation()
                Executors.newSingleThreadExecutor().execute {
                    val lastTrip = fishingDao.getLastTrip()
                    if (lastTrip != null && lastTrip.finishedAt == null) {
                        lastTrip.finishedAt = Date()
                        fishingDao.finishTrip(lastTrip.id, lastTrip.finishedAt)
                        this@TodayActivity.runOnUiThread {
                            setMostRecentTrip(lastTrip)
                        }
                    }
                }
            }
            else if (isChecked && ContextCompat.checkSelfPermission(
                    this@TodayActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@TodayActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 568)
            }
            else if (isChecked && ContextCompat.checkSelfPermission(this@TodayActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                this.checkNewTrip()
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

    private fun checkNewTrip() {
        var needConfirmation = false
        val c = Callable {
            fishingDao.getLastTrip()
        }
        val lastTrip = Executors.newSingleThreadExecutor().submit(c).get()
        if (lastTrip != null) {
            val now = Date()
            val noon = Calendar.getInstance()
            noon.set(Calendar.HOUR_OF_DAY, 12)
            noon.set(Calendar.MINUTE, 0)
            noon.set(Calendar.SECOND, 0)
            noon.set(Calendar.MILLISECOND, 0)
            if (now.before(noon.time)) {
                noon.add(Calendar.DAY_OF_MONTH, -1)
            }
            if (lastTrip.finishedAt == null) {
                if (lastTrip.startedAt.after(noon.time)) {
                    needConfirmation = true
                }
                else {
                    lastTrip.finishedAt = noon.time
                }
            }
            else if ((lastTrip.finishedAt as Date).after(noon.time)) {
                needConfirmation = true
            }

        }
        if (needConfirmation) {
            AlertDialog.Builder(this)
                .setTitle(R.string.new_trip_title)
                .setMessage(R.string.new_trip_question)
                .setPositiveButton(R.string.yes) { _,_ ->
                    Executors.newSingleThreadExecutor().execute {
                        fishingDao.insertTrip(Trip(startedAt = Date()))
                        if (lastTrip != null && lastTrip.finishedAt == null) {
                            fishingDao.finishTrip(lastTrip.id, Date())
                        }
                    }
                    setMostRecentTrip(null)
                }
                .setNegativeButton(R.string.no) { _,_ ->
                    if (lastTrip?.finishedAt != null) {
                        lastTrip.finishedAt = null
                        Executors.newSingleThreadExecutor().execute {
                            fishingDao.finishTrip(lastTrip.id, lastTrip.finishedAt)
                        }
                        setMostRecentTrip(lastTrip)
                    }
                }
                .show()
        }
        else {
            Executors.newSingleThreadExecutor().execute {
                fishingDao.insertTrip(Trip(startedAt = Date()))
                if (lastTrip != null && lastTrip.finishedAt == null) {
                    fishingDao.finishTrip(lastTrip.id, Date())
                }
                setMostRecentTrip(null)
            }
        }
    }

    private fun setMostRecentTrip(trip: Trip?) {
        val c = Callable {
            fishingDao.getLastTrip()
        }
        mostRecentTrip = trip ?: Executors.newSingleThreadExecutor().submit(c).get()
        if (mostRecentTrip != null) {
            val c = Callable {
                fishingDao.getTowsForTrip((mostRecentTrip as Trip).id)
            }
            val towsList = Executors.newSingleThreadExecutor().submit(c).get()
            towsList.forEachIndexed { index, tow ->
                tows[index].tag = tow.id
                tows[index].setText(tow.weight.toString())
            }
            val ca = Callable {
                fishingDao.getLandedsForTrip((mostRecentTrip as Trip).id)
            }
            val existingLandeds = Executors.newSingleThreadExecutor().submit(ca).get()
            if (existingLandeds.isNotEmpty()) {
                existingLandeds.forEachIndexed { index, landedWithSpecies ->
                    landeds[index].first.setTag(R.id.species_id_key, landedWithSpecies.species.first().id)
                    landeds[index].first.text = landedWithSpecies.species.first().name
                    landeds[index].second.setTag(R.id.landed_id_key, landedWithSpecies.landed.id)
                    landeds[index].second.setText(landedWithSpecies.landed.weight.toString())
                }
            }
            else {
                doSpeciesLabels()
            }
        }
        else {
            doSpeciesLabels()
        }
        tripInfo.text = "${mostRecentTrip?.startedAt?.toLocaleString()} - ${mostRecentTrip?.finishedAt?.toLocaleString()}"
    }

    private fun doSpeciesLabels() {
        val c = Callable {
            fishingDao.getSpecies()
        }
        val speciesList= Executors.newSingleThreadExecutor().submit(c).get()
        speciesList.forEachIndexed { index, species ->
            landeds[index].first.text = species.name
            landeds[index].second.setTag(R.id.landed_id_key, false)
            landeds[index].second.setTag(R.id.species_id_key, species.id)
        }
    }
}
