package ru.ilvar.busboard3

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_stop_detail.*
import kotlinx.android.synthetic.main.stop_detail.view.*
import kotlinx.android.synthetic.main.stop_list_time.view.*
import org.ocpsoft.prettytime.PrettyTime

/**
 * A fragment representing a single Stop detail screen.
 * This fragment is either contained in a [StopListActivity]
 * in two-pane mode (on tablets) or a [StopDetailActivity]
 * on handsets.
 */
class StopDetailFragment : Fragment() {
    private var item: Stop? = null
    private var stopsDao: StopDao? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment = this
        this.stopsDao = StopDatabase.getInstance(this.activity?.applicationContext!!).stopDao()

        arguments?.let {
            val type = it.getString(ARG_ITEM_TYPE)
            val code = it.getString(ARG_ITEM_CODE)
            if (type != null && code != null) {
                item = fragment.stopsDao?.getStopByTypeCode(type, code)
                activity?.toolbar_layout?.title = item?.name
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.stop_detail, container, false)
        val fragment = this

        item?.let {
            rootView.stop_detail.text = it.name
            rootView.timesRecycler.adapter = StopTimeRecyclerViewAdapter(it.times)

            if (it.favourite) {
                rootView.button_favourite.setText("Unfavourite")
            } else {
                rootView.button_favourite.setText("Favourite")
            }

            if (it.blocked) {
                rootView.button_block.setText("Unblock")
            } else {
                rootView.button_block.setText("Block")
            }
        }

        with(rootView.button_favourite) {
            tag = item
            setOnClickListener { v ->
                val item = v.tag as Stop
                item.favourite = true
                fragment.stopsDao?.updateStop(item)

                if (item.favourite) {
                    rootView.button_favourite.setText("Unfavourite")
                } else {
                    rootView.button_favourite.setText("Favourite")
                }
            }
        }

        with(rootView.button_block) {
            tag = item
            setOnClickListener { v ->
                val item = v.tag as Stop
                item.blocked = true
                fragment.stopsDao?.updateStop(item)

                if (item.blocked) {
                    rootView.button_block.setText("Unblock")
                } else {
                    rootView.button_block.setText("Block")
                }
            }
        }

        return rootView
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_TYPE= "stop_type"
        const val ARG_ITEM_CODE = "stop_code"
    }

    internal class StopTimeRecyclerViewAdapter(private val values: List<StopTime>?) :
        RecyclerView.Adapter<StopTimeRecyclerViewAdapter.ViewHolder>() {

        var pt = PrettyTime()

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

                holder.timeToView.text = "in " + pt.format(item.time).replace(" from now", "")

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
