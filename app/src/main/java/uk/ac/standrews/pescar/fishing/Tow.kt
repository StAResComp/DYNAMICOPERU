package uk.ac.standrews.pescar.fishing

import android.arch.persistence.room.*
import java.util.*

/**
 * Describes a tow/haul of fish
 *
 * @property id numeric id, autoincremented by the database
 * @property weight the weight hauled in kg
 * @property timestamp when the tow was recorded
 * @property uploaded when the data was uploaded to the server
 */
@Entity(
    tableName = "tow"
)
data class Tow(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var weight: Double,
    var timestamp: Date,
    var uploaded: Date? = null
)