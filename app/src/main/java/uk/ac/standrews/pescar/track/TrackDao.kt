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
     * Updates positions in the database with an uploaded [Date]
     *
     * @param timestamp the uploaded date to be recorded
     * @param ids the ids of the records to be updated
     */
    @Query("UPDATE position SET uploaded = :timestamp WHERE id IN (:ids)")
    fun markPositionsUploaded(ids: List<Int>, timestamp: Date)

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

    @Query(" SELECT * FROM position WHERE timestamp >= :startedAt AND timestamp < :finishedAt")
    fun getPositionsForPeriod(startedAt: Date, finishedAt: Date): List<Position>

    @Query(" SELECT * FROM position WHERE timestamp >= :startedAt AND timestamp < :finishedAt ORDER BY id ASC LIMIT 1")
    fun getFirstPositionForPeriod(startedAt: Date, finishedAt: Date): Position?

    @Query(" SELECT * FROM position WHERE timestamp >= :startedAt AND timestamp < :finishedAt ORDER BY id DESC LIMIT 1")
    fun getLastPositionForPeriod(startedAt: Date, finishedAt: Date): Position?

    @Query(" SELECT * FROM position WHERE id IN (:ids)")
    fun getPositions(ids: List<Int>): List<Position>

    @Query("SELECT * FROM position WHERE uploaded IS NULL")
    fun getUnuploadedPositions(): List<Position>
}
