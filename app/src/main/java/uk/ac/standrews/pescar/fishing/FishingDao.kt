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

    /**
     * Add an array of [Species] to the database. For use in initially populating the database
     *
     * @param species [Array] of [Species] to be inserted
     */
    @Insert
    fun insertSpecies(species: Array<Species>)

    /**
     * Add a [Species] to the database. For use in initially populating the database
     *
     * @param species [Species] to be inserted
     */
    @Insert
    fun insertSpecies(species: Species)

    /**
     * Get array of all [Species] names from the database.
     *
     * @return [Array] of all [Species] names in the database
     */
    @Query("SELECT name FROM species")
    fun getSpecies(): Array<String>

    /**
     * Get most recent [Trip] from the database.
     *
     * @return most recent [Trip]
     */
    @Query("SELECT * FROM trip ORDER BY id DESC LIMIT 1")
    fun getLastTrip(): Trip?

    /**
     * Add a [Trip] to the database.
     *
     * @param trip [Trip] to be inserted
     */
    @Insert
    fun insertTrip(trip: Trip)

    /**
     * Add a [Trip] to the database.
     *
     * @param trip [Trip] to be inserted
     */
    @Query("UPDATE trip SET finishedat = :timestamp WHERE id = :id")
    fun finishTrip(id: Int, timestamp: Date?)

}