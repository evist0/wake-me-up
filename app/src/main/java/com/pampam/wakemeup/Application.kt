package com.pampam.wakemeup

import android.app.Application
import androidx.room.Room
import com.pampam.wakemeup.data.DestinationRepository
import com.pampam.wakemeup.data.MyLocationRepository
import com.pampam.wakemeup.data.db.AppDatabase
import com.pampam.wakemeup.ui.MainActivityViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        val appDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase::class.simpleName.toString()
        ).apply {
            allowMainThreadQueries()
        }.build()

        val appModule = module {

            single { appDatabase.getLastDestinationDao() }
            single { DestinationRepository(get()) }

            single { MyLocationRepository() }

            single { MainActivityViewModel(get(), get()) }
        }

        startKoin {
            androidContext(this@Application)
            modules(appModule)
        }
    }
}