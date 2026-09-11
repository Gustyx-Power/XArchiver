package id.xms.xarchiver

import android.app.Application

class XArchiverApp : Application() {
    companion object {
        lateinit var instance: XArchiverApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
