package com.pampam.wakemeup

import android.app.Application
import android.content.Context
import android.location.LocationManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Application : Application() {
    private val appModule = module {
        single { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
        single { LocationService(get(), get()) }
        single { LocationRepository(get()) }
        single { MainActivityViewModel(get()) }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@Application)
            modules(appModule)
        }
    }
}