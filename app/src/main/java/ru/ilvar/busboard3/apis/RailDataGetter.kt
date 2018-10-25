package ru.ilvar.busboard3.apis

import okhttp3.OkHttpClient
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.*
import retrofit2.http.GET
import ru.ilvar.busboard3.Coords
import ru.ilvar.busboard3.Stop
import ru.ilvar.busboard3.StopDao
import ru.ilvar.busboard3.StopType

class RailDataGetter() {
    val baseUrl = "https://api.irishrail.ie/"

    @Root(name = "objStation")
    internal data class ObjStation (
        @field:Element(name = "StationId") var id: String? = null,
        @field:Element(name = "StationDesc") var fullName: String? = null,
        @field:Element(name = "StationLatitude") var lat: Double? = null,
        @field:Element(name = "StationLongitude") var lng: Double? = null,
        @field:Element(name = "StationCode") var code: String? = null,
        @field:Element(name = "StationAlias", required = false) var alias: String? = null
    ) {
        fun asStop(): Stop {
            return Stop(
                type = StopType.TRAIN,
                code = this.code!!,
                coords = Coords(this.lat!!, this.lng!!),
                name = this.fullName!!
            )
        }
    }

    @Root(name = "ArrayOfObjStation", strict = false)
    data class RailResponse internal constructor(
        @field:ElementList(inline = true) var stations: List<ObjStation>? = null
    )

    internal interface RailAPI {
        @get:GET("/realtime/realtime.asmx/getAllStationsXML")
        val stations: Call<RailResponse>
    }

    fun getAllStations(stopsDao: StopDao) {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()

        val api = retrofit.create<RailAPI>(RailAPI::class.java)

        val call = api.stations
        call.enqueue(object : Callback<RailResponse> {
            override fun onResponse(call: Call<RailResponse>, response: Response<RailResponse>) {
                val kk = response.body().stations
                kk!!.forEach {
                    stopsDao.addStop(it.asStop())
                }

            }

            override fun onFailure(call: Call<RailResponse>, t: Throwable) {
                println(t.localizedMessage)
            }
        })
    }
}

