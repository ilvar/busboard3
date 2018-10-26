package ru.ilvar.busboard3

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.*
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.Database
import android.content.Context
import android.location.Location


enum class StopType(val type: String) {
    BUS("bus"),
    LUAS("luas"),
    TRAIN("train")
}

data class Coords (val lat: Double, val lng: Double) {
    fun distanceFromLocation(l: Location): Double {
        val sqLat = Math.pow(((this.lat - l.latitude) * 110), 2.0)
        val sqLng = Math.pow(((this.lng - l.longitude) * Math.cos(l.latitude) * 111), 2.0)
        return Math.sqrt(sqLat + sqLng)
    }
}

data class StopTime(val time: Date, val route: String, val dest: String)

@Entity(tableName = "stops")
data class Stop(
    @ColumnInfo
    val type: StopType,

    @ColumnInfo
    val code: String,

    @ColumnInfo
    val name: String,

    @Embedded
    val coords: Coords,

    @ColumnInfo
    val times: List<StopTime>? = null,

    @ColumnInfo
    var favourite: Boolean = false,

    @ColumnInfo
    var blocked: Boolean = false,

    @PrimaryKey
    val id: String = "${type}_${code}"
) {
    override fun equals(other: Any?): Boolean {
        return other is Stop && this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }
}

class StopConverters {
    val gson = GsonBuilder().setPrettyPrinting().create()

    @TypeConverter
    fun fromListStopTimeToJson(value: List<StopTime>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun fromJsonToListStopTime(json: String?): List<StopTime>? {
        return gson.fromJson(json, object : TypeToken<List<StopTime>>() {}.type)
    }

    @TypeConverter
    fun fromStopTypeToString(value: StopType?): String? {
        return if (value == null) null else value.type
    }

    @TypeConverter
    fun fromStringToStopType(s: String?): StopType? {
        return if (s == null) null else StopType.valueOf(s.toUpperCase())
    }
}

@Dao
interface StopDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addStop(stop: Stop)

    @Update
    fun updateStop(stop: Stop)

    @Delete
    fun deleteStop(stop: Stop)

    @Query("SELECT * FROM stops WHERE lat > :lat1 AND lat < :lat2 AND lng > :lng1 AND lng < :lng2")
    fun listStopsAround(lat1: Double, lng1: Double, lat2: Double, lng2: Double): LiveData<List<Stop>>

    @Query("SELECT * FROM stops")
    fun allStops(): LiveData<List<Stop>>

    @Query("SELECT * FROM stops WHERE id = :id")
    fun getStopById(id: String): Stop

    @Query("SELECT * FROM stops WHERE type = :type AND code = :code")
    fun getStopByTypeCode(type: String, code: String): Stop
}


@Database(entities = arrayOf(Stop::class), version = 5)
@TypeConverters(StopConverters::class)
abstract class StopDatabase : RoomDatabase() {
    abstract fun stopDao(): StopDao

    companion object {
        @Volatile private var INSTANCE: StopDatabase? = null

        fun getInstance(context: Context): StopDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, StopDatabase::class.java, "stops.db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
    }
}