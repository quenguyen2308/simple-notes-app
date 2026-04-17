package com.yourname.simplenotes.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.yourname.simplenotes.data.remote.DriveDataSource
import com.yourname.simplenotes.data.repository.NoteRepository
import com.yourname.simplenotes.sync.SyncWorker

/**
 * Custom WorkerFactory so Koin-managed dependencies can be injected into SyncWorker.
 * Registered in NotesApp instead of using the default WorkManager initializer.
 */
class SyncWorkerFactory(
    private val repository: NoteRepository,
    private val driveDataSource: DriveDataSource
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == SyncWorker::class.java.name) {
            SyncWorker(appContext, workerParameters, repository, driveDataSource)
        } else null
    }
}
