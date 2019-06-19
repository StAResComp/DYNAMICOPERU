package uk.ac.standrews.pescar.fishing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM tow WHERE timestamp >= :startedAt AND timestamp < :finishedAt ORDER BY id ASC")
    fun getTowsForPeriod(startedAt: Date, finishedAt: Date): Array<Tow>

    @Query("UPDATE tow SET weight = :weight, timestamp = :timestamp WHERE id = :id")
    fun updateTow(id: Int, weight: Double, timestamp: Date)

    @Transaction
    @Query("SELECT l.* FROM species s INNER JOIN landed l ON s.id = l.species_id WHERE l.timestamp >= :startedAt AND l.timestamp < :finishedAt ORDER BY s.id ASC")
    fun getLandedsForPeriod(startedAt: Date, finishedAt: Date): Array<LandedWithSpecies>

    @Query("UPDATE landed SET weight = :weight, timestamp = :timestamp WHERE id = :id")
    fun updateLanded(id: Int, weight: Double, timestamp: Date)

    @Query("SELECT * FROM tow WHERE uploaded IS NULL AND timestamp >= :startedAt AND timestamp < :finishedAt")
    fun getUnuploadedTowsForPeriod(startedAt: Date, finishedAt: Date): List<Tow>

    @Transaction
    @Query("SELECT * FROM landed WHERE uploaded IS NULL AND timestamp >= :startedAt AND timestamp < :finishedAt")
    fun getUnuploadedLandedsForPeriod(startedAt: Date, finishedAt: Date): List<LandedWithSpecies>

    @Query("UPDATE tow SET uploaded = :timestamp WHERE id IN (:ids)")
    fun markTowsUploaded(ids: List<Int>, timestamp: Date)

    @Query("UPDATE landed SET uploaded = :timestamp WHERE id IN (:ids)")
    fun markLandedsUploaded(ids: List<Int>, timestamp: Date)
}