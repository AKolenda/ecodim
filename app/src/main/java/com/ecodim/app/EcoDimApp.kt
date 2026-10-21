package com.ecodim.app

import android.app.Application

class EcoDimApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Dimmer.init(this)
        Notifications.createChannel(this)
    }
}
