package uk.ac.standrews.pescar

import android.arch.persistence.room.*
import android.content.Context
import uk.ac.standrews.pescar.track.Position
import uk.ac.standrews.pescar.track.TrackDao
import java.util.Date

/**
 * Defines the database for the app.
 *
 * To be used as a singleton via [AppDatabase.getAppDataBase]
 */
@Database(
    entities = [
        Position::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Database access object for location tracking data
     */
    abstract fun trackDao() : TrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns [AppDatabase] singleton
         */
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

        /**
         * Destroys [AppDatabase] singleton
         */
        fun destroyDataBase(){
            INSTANCE = null
        }
    }
}

/**
 * Converter for handling dates.
 *
 * Converts [Date] to [Long] timestamp and vice-versa. [Date] is used in the application code, but dates are stored as
 * [Long] in SQLite
 */
class DateTypeConverter {

    /**
     * Converts [Long] timestamp to [Date]
     *
     * @param ts timestamp to be converted
     * @return corresponding [Date]
     */
    @TypeConverter
    fun fromTimestamp(ts: Long?): Date? {
        if (ts != null) return Date(ts)
        else return null
    }

    /**
     * Converts [Date] to [Long] timestampe
     *
     * @param date [Date] to be converted
     * @return corresponding [Long] timestamp
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time;
    }

}
