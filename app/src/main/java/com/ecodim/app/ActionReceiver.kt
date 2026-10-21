package com.ecodim.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the buttons on the dimming notification. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_OFF -> Dimmer.setActive(false)
            ACTION_LIGHTER -> Dimmer.nudge(-Dimmer.STEP)
            ACTION_DARKER -> Dimmer.nudge(Dimmer.STEP)
        }
    }

    companion object {
        const val ACTION_OFF = "com.ecodim.app.action.OFF"
        const val ACTION_LIGHTER = "com.ecodim.app.action.LIGHTER"
        const val ACTION_DARKER = "com.ecodim.app.action.DARKER"
    }
}
