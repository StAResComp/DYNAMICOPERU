package uk.ac.standrews.pescar.fishing

import android.arch.persistence.room.*
import java.util.*

/**
 * Describes a tow/haul of fish
 *
 * @property id numeric id, autoincremented by the database
 * @property weight the weight hauled in kg
 * @property timestamp when the tow was recorded
 * @property tripId the id of the [Trip] on which the tow was made
 * @property uploaded when the data was uploaded to the server
 */
@Entity(
    tableName = "tow",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"]
        )
    ],
    indices = [Index("trip_id")]
)
data class Tow(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var weight: Double,
    var timestamp: Date,
    @ColumnInfo(name = "trip_id") var tripId: Int,
    var uploaded: Date? = null
)