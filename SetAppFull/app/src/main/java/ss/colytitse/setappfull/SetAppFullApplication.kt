package ss.colytitse.setappfull

import android.app.Application
import ss.colytitse.setappfull.data.AppRepository

class SetAppFullApplication : Application() {
    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(this)
    }
}
