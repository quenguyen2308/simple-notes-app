package com.yourname.simplenotes.di

import com.yourname.simplenotes.data.local.CategorySyncPrefs
import com.yourname.simplenotes.data.local.NoteDatabase
import com.yourname.simplenotes.data.local.NoteSyncPrefs
import com.yourname.simplenotes.data.remote.DriveAuthManager
import com.yourname.simplenotes.data.remote.DriveDataSource
import com.yourname.simplenotes.data.repository.CategoryRepository
import com.yourname.simplenotes.data.repository.CategoryRepositoryImpl
import com.yourname.simplenotes.data.repository.NoteRepository
import com.yourname.simplenotes.data.repository.NoteRepositoryImpl
import com.yourname.simplenotes.data.repository.SearchRepository
import com.yourname.simplenotes.data.repository.SearchRepositoryImpl
import com.yourname.simplenotes.sync.SyncScheduler
import com.yourname.simplenotes.ui.editor.NoteEditorViewModel
import com.yourname.simplenotes.ui.folder.FolderViewModel
import com.yourname.simplenotes.ui.notes.NoteListViewModel
import com.yourname.simplenotes.ui.search.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // --- Data layer ---
    single { NoteDatabase.create(androidContext()) }
    single { get<NoteDatabase>().noteDao() }
    single { get<NoteDatabase>().categoryDao() }
    single { get<NoteDatabase>().noteSearchDao() }
    single { get<NoteDatabase>().searchHistoryDao() }

    // --- Drive / remote ---
    single { DriveAuthManager(androidContext()) }
    single { DriveDataSource(get()) }

    // --- Sync ---
    single { SyncScheduler(androidContext()) }
    single { SyncWorkerFactory(get(), get(), get()) }

    // --- Repositories ---
    single { NoteSyncPrefs(androidContext()) }
    single<NoteRepository> { NoteRepositoryImpl(get(), get(), get(), get()) }
    single { CategorySyncPrefs(androidContext()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get(), get()) }
    single<SearchRepository> { SearchRepositoryImpl(get(), get(), get()) }

    // --- ViewModels ---
    viewModel { NoteListViewModel(get(), get(), get(), androidContext()) }
    viewModel { NoteEditorViewModel(get(), get()) }
    viewModel { FolderViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
}
