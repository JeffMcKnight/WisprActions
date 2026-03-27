package at.mcknight.wispractions.di

import at.mcknight.wispractions.MainViewModel
import at.mcknight.wispractions.MicrophonePermissionHandler
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * App-level Module definition for Koin dependency injection
 */
val appModule = module {
    factory { MicrophonePermissionHandler() }
    viewModel { MainViewModel( get()) }
}
