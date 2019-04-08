package uk.ac.standrews.pescar.fishing

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.*

/**
 * Describes a fishing trip
 *
 * @property id numeric id, autoincremented by the database
 * @property startedAt the timestamp for the start of the fishing trip
 * @property finishedAt the timestamp for the end of the fishing trip
 * @property uploaded when the data was uploaded to the server
 */
@Entity(tableName = "trip")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startedAt: Date,
    var finishedAt: Date? = null,
    var uploaded: Date? = null
)