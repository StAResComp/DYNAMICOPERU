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

}
