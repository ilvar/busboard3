package ru.ilvar.busboard3.apis

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import ru.ilvar.busboard3.Coords
import ru.ilvar.busboard3.Stop
import ru.ilvar.busboard3.StopDao
import ru.ilvar.busboard3.StopType
import java.net.URL

class BusDataGetter() {
    val baseUrl = "https://data.smartdublin.ie/cgi-bin/rtpi/busstopinformation?stopid&format=json"
    val gson = GsonBuilder().setPrettyPrinting().create()

    internal data class ObjBusStop (
        val stopid: String? = null,
        val fullname: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) {
        fun asStop(): Stop {
            return Stop(
                type = StopType.BUS,
                code = this.stopid!!,
                coords = Coords(this.latitude!!, this.longitude!!),
                name = this.fullname!!
            )
        }
    }

    internal data class BusResponse (
        val results: List<ObjBusStop>
    )

    data class DataThread(val stopsDao: StopDao, val url: String, val gson: Gson) : Thread() {

        override fun run() {
            val json = URL(url).readText()
            val response = gson.fromJson<BusResponse>(json, object : TypeToken<BusResponse>() {}.type)

            val kk = response.results
            System.err.println("Bus response: ${kk.size} stops")
            kk.forEach {
                stopsDao.addStop(it.asStop())
            }
            System.err.println("Bus response processed")

        }
    }

    fun getAllStations(stopsDao: StopDao) {
        val p = DataThread(stopsDao, baseUrl, gson)
        Thread(p).start()
    }
}

