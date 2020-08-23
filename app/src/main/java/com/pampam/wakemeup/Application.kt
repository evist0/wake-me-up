package com.pampam.wakemeup

import android.app.Application
import androidx.room.Room
import com.google.android.libraries.places.api.Places
import com.pampam.wakemeup.data.DestinationRepository
import com.pampam.wakemeup.data.MyLocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.db.AppDatabase
import com.pampam.wakemeup.ui.MainActivityViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        Places.initialize(this, getString(R.string.places_sdk_api_key))

        val appDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase::class.simpleName.toString()
        ).apply {
            allowMainThreadQueries()
        }.build()

        val appModule = module {
            single { Places.createClient(this@Application) }
            single { appDatabase.getLastDestinationDao() }
            single { DestinationRepository(get(), get()) }

            single { MyLocationRepository() }

            single { SessionRepository() }

            single { MainActivityViewModel(get(), get(), get()) }
        }

        startKoin {
            androidContext(this@Application)
            modules(appModule)
        }
    }
}