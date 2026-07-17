package com.fginc.weldo

import android.app.Application
import com.fginc.weldo.data.WeldoRepository
import com.fginc.weldo.data.local.WeldoSession
import com.fginc.weldo.data.remote.ApiProvider
import com.fginc.weldo.notifications.NudgeScheduler

/**
 * Manual dependency container (no Hilt/KSP — keeps the build annotation-processor-free).
 * Everything hangs off the singleton [WeldoApp.graph].
 */
class WeldoApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        INSTANCE = this
        NudgeScheduler.createChannel(this)
    }

    companion object {
        private lateinit var INSTANCE: WeldoApp
        val graph: AppGraph get() = INSTANCE.graph
    }
}

class AppGraph(app: WeldoApp) {
    val session: WeldoSession = WeldoSession(app.applicationContext)
    val apiProvider: ApiProvider = ApiProvider(session)
    val repository: WeldoRepository = WeldoRepository(apiProvider, session)
    val nudgeScheduler: NudgeScheduler = NudgeScheduler(app.applicationContext)
}
