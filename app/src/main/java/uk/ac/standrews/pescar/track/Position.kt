package uk.ac.standrews.pescar.track

import androidx.room.*
import java.util.Date

/**
 * Describes a position record
 *
 * @property id numeric id, autoincremented by the database
 * @property latitude latitude value of the position
 * @property longitude longitude value of the position
 * @property accuracy accuracy of the recorded position as per [android.location.Location.getAccuracy]
 * @property timestamp when the location was recorded
 * @property uploaded when the data was uploaded to the server
 */
@Entity(tableName = "position")
data class Position(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Date,
    var uploaded: Date? = null
)
