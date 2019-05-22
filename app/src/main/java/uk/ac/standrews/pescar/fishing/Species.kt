package uk.ac.standrews.pescar.fishing

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Describes a species
 *
 * @property id numeric id, autoincremented by the database
 * @property name the name of the species
 */
@Entity(tableName = "species")
data class Species(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
) {

    companion object {
        fun getInitialData(): Array<Species> {
            return arrayOf(
                Species(1, "Langostino"),
                Species(2, "Langostino jumbo"),
                Species(3, "Carajito"),
                Species(4, "Lenguado"),
                Species(5, "Calamar"),
                Species(6, "Guitarra")
            )
        }
    }

}