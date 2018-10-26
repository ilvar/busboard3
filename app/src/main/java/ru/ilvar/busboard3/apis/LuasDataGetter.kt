package ru.ilvar.busboard3.apis

import okhttp3.OkHttpClient
import org.simpleframework.xml.Element
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Text
import org.simpleframework.xml.Root
import retrofit2.*
import retrofit2.http.GET
import ru.ilvar.busboard3.Coords
import ru.ilvar.busboard3.Stop
import ru.ilvar.busboard3.StopDao
import ru.ilvar.busboard3.StopType

class LuasDataGetter() {
    val baseUrl = "http://luasforecasts.rpa.ie/"

    @Root(name = "stop", strict = false)
    internal data class ObjLuasStop (
        @field:Attribute(name = "pronunciation") var fullName: String? = null,
        @field:Attribute(name = "lat") var lat: Double? = null,
        @field:Attribute(name = "long") var lng: Double? = null,
        @field:Attribute(name = "abrev") var code: String? = null,
        @field:Text() var name: String? = null
    ) {
        fun asStop(): Stop {
            return Stop(
                type = StopType.LUAS,
                code = this.code!!,
                coords = Coords(this.lat!!, this.lng!!),
                name = this.fullName!!
            )
        }
    }

    @Root(name = "line", strict = false)
    internal data class ObjLuasLine (
        @field:Attribute(name = "name") var name: String? = null,
        @field:ElementList(inline = true) var stops: List<ObjLuasStop>? = null
    )

    @Root(name = "stops", strict = false)
    data class LuasResponse internal constructor(
        @field:ElementList(inline = true) var lines: List<ObjLuasLine>? = null
    )

    internal interface LuasAPI {
        @get:GET("/xml/get.ashx?action=stops&encrypt=false")
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
                val ll = response.body().lines
                System.err.println("LUAS response: ${ll?.size} lines")
                ll!!.forEach {
                    System.err.println("LUAS line ${it.name}: ${it.stops?.size} stops")
                    it.stops!!.forEach {
                        stopsDao.addStop(it.asStop())
                    }

                }
                System.err.println("LUAS response processed")

            }

            override fun onFailure(call: Call<LuasResponse>, t: Throwable) {
                System.err.println(t.localizedMessage)
            }
        })
    }
}

