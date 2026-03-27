package at.mcknight.wispractions

import android.app.Application
import at.mcknight.wispractions.di.appModule
import org.koin.core.context.startKoin        // startKoin {}
import org.koin.android.ext.koin.androidContext // androidContext()
import org.koin.android.ext.koin.androidLogger  // androidLogger()
import org.koin.core.logger.Level

/**
 * Initialize dependency injection
 */
class WisprApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WisprApp)
            androidLogger(Level.DEBUG) // Level.NONE in prod
            modules(appModule)
        }
    }
}
