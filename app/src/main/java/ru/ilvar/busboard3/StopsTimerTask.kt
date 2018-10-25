package ru.ilvar.busboard3

import java.util.*

class StopsTimerTask(val main: StopListActivity) : TimerTask() {

    override fun run() {
        System.out.println("Timer task started at:" + Date());
        completeTask();
        System.out.println("Timer task finished at:" + Date());
    }

    private fun completeTask() {
        main.updateTimes()
    }
}