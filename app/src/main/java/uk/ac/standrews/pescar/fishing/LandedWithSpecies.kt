package uk.ac.standrews.pescar.fishing

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation

class LandedWithSpecies {
    @Embedded
    lateinit var landed: Landed
    @Relation(parentColumn = "species_id", entityColumn = "id")
    lateinit var species: List<Species>
}