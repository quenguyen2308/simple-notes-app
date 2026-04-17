package com.yourname.simplenotes

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.yourname.simplenotes.di.SyncWorkerFactory
import com.yourname.simplenotes.di.appModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NotesApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start Koin DI
        startKoin {
            androidContext(this@NotesApp)
            modules(appModule)
        }

        // Initialize WorkManager with our custom factory so SyncWorker gets DI deps
        val workerFactory: SyncWorkerFactory by inject()
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        )
    }
}
