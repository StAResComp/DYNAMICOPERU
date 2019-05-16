package uk.ac.standrews.pescar.fishing

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import java.util.Date

/**
 * Database Access Object for working with fishing activity data
 */
@Dao
interface FishingDao {

    @Insert
    fun insertSpecies(species: Array<Species>)

    @Insert
    fun insertSpecies(species: Species)

    @Insert
    fun insertTow(tow: Tow): Long

    @Insert
    fun insertLanded(landed: Landed): Long

    @Query("SELECT * FROM species ORDER BY id ASC")
    fun getSpecies(): Array<Species>

    @Query("SELECT name FROM species ORDER BY id ASC")
    fun getSpeciesNames(): Array<String>

    @Query("SELECT * FROM trip ORDER BY id DESC LIMIT 1")
    fun getLastTrip(): Trip?

    @Insert
    fun insertTrip(trip: Trip)

    @Query("UPDATE trip SET finishedat = :timestamp WHERE id = :id")
    fun finishTrip(id: Int, timestamp: Date?)

    @Query("SELECT * FROM tow WHERE trip_id = :tripId ORDER BY id ASC")
    fun getTowsForTrip(tripId: Int): Array<Tow>

    @Query("UPDATE tow SET weight = :weight, timestamp = :timestamp WHERE id = :id")
    fun updateTow(id: Int, weight: Double, timestamp: Date)

    @Query("SELECT l.* FROM species s INNER JOIN landed l ON s.id = l.species_id WHERE l.trip_id = :tripId ORDER BY s.id ASC")
    fun getLandedsForTrip(tripId: Int): Array<LandedWithSpecies>

    @Query("UPDATE landed SET weight = :weight, timestamp = :timestamp WHERE id = :id")
    fun updateLanded(id: Int, weight: Double, timestamp: Date)


}