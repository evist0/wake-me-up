package com.pampam.wakemeup

import android.app.Application
import androidx.room.Room
import com.google.android.libraries.places.api.Places
import com.pampam.wakemeup.data.DestinationPredictionRepository
import com.pampam.wakemeup.data.LocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.db.AppDatabase
import com.pampam.wakemeup.ui.AlarmActivityViewModel
import com.pampam.wakemeup.ui.MainActivityViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
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
        ).build()

        val appModule = module {
            single { Places.createClient(this@Application) }
            single { appDatabase.getLastDestinationDao() }
            single { DestinationPredictionRepository(get(), get()) }

            single { LocationRepository() }

            single { SessionRepository() }

            viewModel { MainActivityViewModel(get(), get(), get()) }
            viewModel { AlarmActivityViewModel(get()) }
        }

        startKoin {
            androidContext(this@Application)
            modules(appModule)
        }
    }
}