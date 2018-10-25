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

class LuasDataGetter() {
    val baseUrl = "https://api.irishLuas.ie/"

    @Root(name = "objStation")
    internal data class ObjLuasStop (
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
    data class LuasResponse internal constructor(
        @field:ElementList(inline = true) var stations: List<ObjLuasStop>? = null
    )

    internal interface LuasAPI {
        @get:GET("/realtime/realtime.asmx/getAllStationsXML")
        val stations: Call<LuasResponse>
    }

    fun getAllStations(stopsDao: StopDao) {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()

        val api = retrofit.create<LuasAPI>(LuasAPI::class.java)

        val call = api.stations
        call.enqueue(object : Callback<LuasResponse> {
            override fun onResponse(call: Call<LuasResponse>, response: Response<LuasResponse>) {
                val kk = response.body().stations
                kk!!.forEach {
                    stopsDao.addStop(it.asStop())
                }

            }

            override fun onFailure(call: Call<LuasResponse>, t: Throwable) {
                println(t.localizedMessage)
            }
        })
    }
}

