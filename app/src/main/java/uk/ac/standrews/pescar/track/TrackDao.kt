package uk.ac.standrews.pescar.track

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import java.util.Date

@Dao
interface TrackDao {

    @Insert
    fun insertPosition(pos: Position): Long

    @Query("UPDATE position SET uploaded = :timestamp WHERE id = :id")
    fun markPositionUploaded(timestamp: Date, id: Long)

}
