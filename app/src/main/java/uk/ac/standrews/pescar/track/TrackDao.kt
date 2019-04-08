package uk.ac.standrews.pescar.track

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import java.util.Date

/**
 * Database Access Object for working with location tracking data
 */
@Dao
interface TrackDao {

    /**
     * Add a [Position] to the database
     *
     * @param pos [Position] to be inserted
     * @return id of the inserted row as [Long]
     */
    @Insert
    fun insertPosition(pos: Position): Long

    /**
     * Updates a position in the database with an uploaded [Date]
     *
     * @param timestamp the uploaded date to be recorded
     * @param id the id of the record to be updated
     */
    @Query("UPDATE position SET uploaded = :timestamp WHERE id = :id")
    fun markPositionUploaded(timestamp: Date, id: Long)

    /**
     * Gets the last inserted position
     *
     * @return the last inserted [Position]
     */
    @Query("SELECT * FROM position ORDER BY id DESC LIMIT 1")
    fun getLastPosition(): Position

    /**
     * Counts the number of recorded positions
     *
     * @return the number of recorded [Position]s
     */
    @Query("SELECT COUNT(*) FROM position")
    fun countPositions(): Int

}
