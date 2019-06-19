package uk.ac.standrews.pescar.fishing

import androidx.room.*

class LandedWithSpecies {
    @Embedded
    lateinit var landed: Landed
    @Relation(parentColumn = "species_id", entityColumn = "id")
    lateinit var species: List<Species>
}