package ru.ilvar.busboard3.updaters

import okhttp3.OkHttpClient
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.*
import retrofit2.http.GET
import retrofit2.http.Query
import ru.ilvar.busboard3.*
import java.util.*



class LuasTimesUpdater() {
    val baseUrl = "https://api.irishrail.ie/"

    @Root(name = "objStationData", strict = false)
    internal data class ObjTimeData (
        @field:Element(name = "Destination") var dest: String? = null,
        @field:Element(name = "Expdepart") var timeStr: String? = null,
        @field:Element(name = "Traincode") var route: String? = null
    ) {
        fun asStopTime(): StopTime {
            val hoursMinutes = this.timeStr!!.split(':')
            val trainTime = Date()
            trainTime.hours = hoursMinutes[0].toInt()
            trainTime.minutes = hoursMinutes[1].toInt()
            trainTime.seconds = 0

            return StopTime(
                dest = this.dest!!,
                route = this.route!!,
                time = trainTime
            )
        }
    }

    @Root(name = "ArrayOfObjStationData", strict = false)
    data class RailTimeResponse internal constructor(
        @field:ElementList(inline = true) var times: List<ObjTimeData>? = null
    )

    internal interface RailTimesAPI {
        @GET("/realtime/realtime.asmx/getStationDataByCodeXML")
        fun timesForStation(@Query("StationCode") station: String): Call<RailTimeResponse>
    }

    fun getAllStations(stopsDao: StopDao, stop: Stop) {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()

        val api = retrofit.create<RailTimesAPI>(RailTimesAPI::class.java)

        val call = api.timesForStation(stop.code)
        call.enqueue(object : Callback<RailTimeResponse> {
            override fun onResponse(call: Call<RailTimeResponse>, response: Response<RailTimeResponse>) {
                val kk = response.body().times
                val stopTimes = kk!!.map {
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

            override fun onFailure(call: Call<RailTimeResponse>, t: Throwable) {
                System.err.println(t.localizedMessage)
            }
        })
    }
}

