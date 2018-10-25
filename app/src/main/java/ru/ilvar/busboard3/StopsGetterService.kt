package ru.ilvar.busboard3

import android.app.job.JobParameters
import android.app.job.JobService
import ru.ilvar.busboard3.apis.RailDataGetter

class StopsGetterService : JobService() {

    override fun onStartJob(parameters: JobParameters?): Boolean {
        val stopsDao = StopDatabase.getInstance(applicationContext).stopDao()
        RailDataGetter().getAllStations(stopsDao)

        // returning false means the work has been done, return true if the job is being run asynchronously
        return true
    }

    override fun onStopJob(parameters: JobParameters?): Boolean {
        // return true to restart the job
        return false
    }
}