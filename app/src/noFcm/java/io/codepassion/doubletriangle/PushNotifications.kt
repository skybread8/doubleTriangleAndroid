package io.codepassion.doubletriangle

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/** No push transport is available until google-services.json is supplied. */
internal object CheckInPushTransport {
    fun start(context: Context) = Unit
}

class CheckInMessagingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
