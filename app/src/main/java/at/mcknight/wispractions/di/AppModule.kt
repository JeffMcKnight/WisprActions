package at.mcknight.wispractions.di

import at.mcknight.wispractions.AudioRecorder
import at.mcknight.wispractions.MainViewModel
import at.mcknight.wispractions.MicrophonePermissionHandler
import at.mcknight.wispractions.PromptRepo
import at.mcknight.wispractions.SherpaClient
import at.mcknight.wispractions.SpeechToTextRepo
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * App-level Module definition for Koin dependency injection
 */
val appModule = module {
    // AssetManager
    single { androidContext().assets }
    // Root directory for this app
    single { androidContext().filesDir }
    factory { MicrophonePermissionHandler() }
    single { SherpaClient(get(), get(), MODEL_DIR_NAME) }
    //Coroutine scope for AudioRecorder TODO: inject ViewModel scope using factories
    factory { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { AudioRecorder(get()) }
    single { Generation.getClient() }
    single { PromptRepo(get()) }
    single { SpeechToTextRepo(get(), get()) }
    viewModel { MainViewModel( get(), get(), get()) }
}

const val MODEL_DIR_NAME: String = "sherpa-onnx-streaming-zipformer-en-2023-06-21"
