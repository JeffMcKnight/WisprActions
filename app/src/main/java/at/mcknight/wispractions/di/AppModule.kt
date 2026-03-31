package at.mcknight.wispractions.di

import at.mcknight.wispractions.AudioRecorder
import at.mcknight.wispractions.LiteRtRepo
import at.mcknight.wispractions.MainViewModel
import at.mcknight.wispractions.MicrophonePermissionHandler
import at.mcknight.wispractions.SherpaClient
import at.mcknight.wispractions.SpeechToTextRepo
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

/**
 * App-level Module definition for Koin dependency injection
 */
val appModule = module {
    // AssetManager
    single { androidContext().assets }
    // Root directory for this app
    single { androidContext().filesDir }
    factory { MicrophonePermissionHandler() }

    single<File>(named("Sherpa")) { File(androidContext().filesDir, SHERPA_MODEL_DIR) }
    single { SherpaClient(
        assetManager = get(),
        modelAssetDir = SHERPA_MODEL_DIR,
        modelDestinationDir = get(named("Sherpa"))
    ) }

    //Coroutine scope for AudioRecorder TODO: inject ViewModel scope using factories
    factory { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { AudioRecorder(get()) }

    single<File>(named("LiteRt")) { File(androidContext().filesDir, LITE_RT_MODEL_DIR) }
    single<String>(named("LiteRtPath")) { get<File>(named("LiteRt")).toString() }
    single { EngineConfig(modelPath = "${get<String>(named("LiteRtPath"))}/$LITE_RT_MODEL_NAME") }
    single { Engine(get()) }
    single { LiteRtRepo(
        engine = get(),
        assetManager = get(),
        modelAssetDirName = LITE_RT_MODEL_DIR,
        modelDir = get<File>(named("LiteRt"))
    ) }
    single { SpeechToTextRepo(get(), get()) }
    viewModel { MainViewModel( get(), get(), get()) }
}


//private const val LITE_RT_MODEL_NAME = "tiny_garden_q8_ekv1024.litertlm"
private const val LITE_RT_MODEL_DIR: String = "litert-community"
private const val LITE_RT_MODEL_NAME = "gemma3-1b-it-int4.litertlm"

private const val SHERPA_MODEL_DIR: String = "sherpa-onnx-streaming-zipformer-en-2023-06-21"
