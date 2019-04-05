package uk.ac.standrews.pescar

import android.arch.persistence.room.*
import android.content.Context
import uk.ac.standrews.pescar.track.Position
import uk.ac.standrews.pescar.track.TrackDao
import java.util.Date

@Database(
    entities = [
        Position::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao() : TrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getAppDataBase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pescar"
                ).build()
                INSTANCE = instance
                return instance
            }
        }

        fun destroyDataBase(){
            INSTANCE = null
        }
    }
}

class DateTypeConverter() {

    @TypeConverter
    fun fromTimestamp(ts: Long?): Date? {
        if (ts != null) return Date(ts)
        else return null
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time;
    }

}
