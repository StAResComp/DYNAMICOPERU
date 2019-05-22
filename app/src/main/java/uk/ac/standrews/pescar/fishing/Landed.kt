package uk.ac.standrews.pescar.fishing

import android.arch.persistence.room.*
import java.util.*

/**
 * Describes the amount of a species landed following a trip
 *
 * @property id numeric id, autoincremented by the database
 * @property speciesId the id of the [Species] landed
 * @property weight the weight landed in kg
 * @property timestamp when the landing was recorded
 * @property tripId the id of the [Trip] on which the tow was made
 * @property uploaded when the data was uploaded to the server
 */
@Entity(
    tableName = "landed",
    foreignKeys = [
        ForeignKey(
            entity = Species::class,
            parentColumns = ["id"],
            childColumns = ["species_id"]
        )
    ],
    indices = [
        Index("species_id")
    ]
)
data class Landed(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "species_id") var speciesId: Int,
    var weight: Double,
    var timestamp: Date,
    var uploaded: Date? = null
)