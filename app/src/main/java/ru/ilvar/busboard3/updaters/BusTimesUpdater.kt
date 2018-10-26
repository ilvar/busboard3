package ru.ilvar.busboard3.updaters

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import ru.ilvar.busboard3.*
import java.net.URL
import java.text.SimpleDateFormat


class BusTimesUpdater() {
    val gson = GsonBuilder().setPrettyPrinting().create()

    internal data class ObjBusTime (
        val departuredatetime: String? = null,
        val destination: String? = null,
        val route: String? = null
    ) {
        fun asStopTime(): StopTime {
            val parser = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            return StopTime(
                time = parser.parse(this.departuredatetime!!),
                route = this.route!!,
                dest = this.destination!!
            )
        }
    }

    internal data class BusResponse (
        val results: List<ObjBusTime>
    )

    data class DataThread(val stopsDao: StopDao, val url: String, val gson: Gson, val stop: Stop) : Thread() {

        override fun run() {
            val json = URL(url).readText()
            val response = gson.fromJson<BusResponse>(json, object : TypeToken<BusResponse>() {}.type)

            val kk = response.results
            val stopTimes = kk.map {
                it.asStopTime()
            }
            stopsDao.updateStop(Stop(
                type = stop.type,
                code = stop.code,
                name = stop.name,
                coords = stop.coords,
                times = stopTimes.sortedBy { it.time },
                favourite = stop.favourite,
                blocked = stop.blocked,
                id = stop.id
            ))

        }
    }

    fun getAllStations(stopsDao: StopDao, stop: Stop) {
        val baseUrl = "https://data.smartdublin.ie/cgi-bin/rtpi/realtimebusinformation?stopid=${stop.code}&format=json"
        val p = DataThread(stopsDao, baseUrl, gson, stop)
        Thread(p).start()
    }
}

