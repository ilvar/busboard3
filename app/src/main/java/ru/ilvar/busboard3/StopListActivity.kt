package ru.ilvar.busboard3

import android.Manifest
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_stop_list.*
import kotlinx.android.synthetic.main.stop_list_content.view.*
import kotlinx.android.synthetic.main.stop_list_time.view.*
import kotlinx.android.synthetic.main.stop_list.*
import ru.ilvar.busboard3.apis.RailDataGetter
import java.util.*
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import ru.ilvar.busboard3.apis.BusDataGetter
import ru.ilvar.busboard3.apis.LuasDataGetter
import ru.ilvar.busboard3.updaters.BusTimesUpdater
import ru.ilvar.busboard3.updaters.LuasTimesUpdater
import ru.ilvar.busboard3.updaters.RailTimesUpdater


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [StopDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class StopListActivity : AppCompatActivity() {

    private val LOCATION_REQUEST_CODE = 58

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var stopsDao: StopDao? = null

    var stops: List<Stop>? = null

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stop_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        this.stopsDao = StopDatabase.getInstance(applicationContext).stopDao()

        RailDataGetter().getAllStations(this.stopsDao!!)
        LuasDataGetter().getAllStations(this.stopsDao!!)
        BusDataGetter().getAllStations(this.stopsDao!!)

        // Acquire a reference to the system Location Manager
        this.locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val activity = this
        this.locationListener = object : LocationListener {
            override fun onProviderEnabled(p0: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onProviderDisabled(p0: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            }

            override fun onLocationChanged(location: Location) {
                val distanceKms = 2.0
                val latDiff = distanceKms / 110
                val lngDiff = kotlin.math.abs(distanceKms / 111 / kotlin.math.cos(location.latitude))

                activity.stopsDao!!.listStopsAround(
                    location.latitude - latDiff,
                    location.longitude - lngDiff,
                    location.latitude + latDiff,
                    location.longitude + lngDiff
                ).observe(activity, Observer<List<Stop>> { stops: List<Stop>? ->
                    if (stops != null) {
                        activity.stops = stops
                        val stopAdapter = SimpleItemRecyclerViewAdapter(activity.stops!!, getString(R.string.time_in))
                        stop_list.setAdapter(stopAdapter)
                    }
                })
            }
        }

        // Here, thisActivity is the current activity
        val perm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (perm != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), LOCATION_REQUEST_CODE)

        } else {
            updateLocation()
        }

        val timerTask = StopsTimerTask(this)
        val timer = Timer(true)
        timer.scheduleAtFixedRate(timerTask, 0, 10 * 1000)

        this.updateTimes()
    }

    fun updateLocation() {
        val perm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (perm == PackageManager.PERMISSION_GRANTED) {
            this.locationManager?.requestSingleUpdate(LocationManager.GPS_PROVIDER, this.locationListener, null)
            this.locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 50f,
                this.locationListener)
        }
    }

    fun updateTimes() {
        this.stops?.forEach {
            when(it.type) {
                StopType.TRAIN -> RailTimesUpdater().getAllStations(this.stopsDao!!, it)
                StopType.LUAS -> LuasTimesUpdater().getAllStations(this.stopsDao!!, it)
                StopType.BUS -> BusTimesUpdater().getAllStations(this.stopsDao!!, it)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val activity = this
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    updateLocation()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    class SimpleItemRecyclerViewAdapter(
        private val values: List<Stop>,
        private val timeString: String?
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as Stop
                val intent = Intent(v.context, StopDetailActivity::class.java).apply {
                    putExtra(StopDetailFragment.ARG_ITEM_ID, item.id)
                }
                v.context.startActivity(intent)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.stop_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.contentView.text = item.name
            holder.timesRecycler.adapter = StopTimeRecyclerViewAdapter(item.times, timeString)

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val contentView: TextView = view.content
            val timesRecycler: RecyclerView = view.timesRecycler
        }
    }

    class StopTimeRecyclerViewAdapter(
        private val values: List<StopTime>?,
        private val timeString: String?
    ) :
        RecyclerView.Adapter<StopTimeRecyclerViewAdapter.ViewHolder>() {

        init {}

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.stop_list_time, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (values != null) {
                val item = values[position]
                holder.timeView.text = item.time.toString().split(" ")[3].substringBeforeLast(':')

                val timeDiff = (item.time.getTime() - Date().getTime()) / 1000 / 60
                holder.timeToView.text = String.format(timeString!!, timeDiff)

                holder.route.text = item.route
                holder.dest.text = item.dest
            }
        }

        override fun getItemCount() = if (values != null) values.size else 0

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val timeView: TextView = view.time
            val timeToView: TextView = view.timeTo
            val route: TextView = view.route
            val dest: TextView = view.dest
        }
    }
}
