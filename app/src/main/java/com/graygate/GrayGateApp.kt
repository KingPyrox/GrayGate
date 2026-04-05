package com.graygate

import android.app.Application
import com.graygate.data.db.GrayGateDatabase

class GrayGateApp : Application() {

    val database: GrayGateDatabase by lazy {
        GrayGateDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GrayGateApp
            private set
    }
}
