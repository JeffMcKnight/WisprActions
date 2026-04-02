# WisprActions
Wisper Android SWE Challenge 
----
Launches an Android timer via speech commands.  The implementation on `main` is slow on the emulator because of the LiteRT model performance.  The spoken command is transcribed and displayed on-screen in ~500ms, but the LiteRT response to the transcribed text takes roughly 3 seconds.  There is an implementation on an alternate branch (`TinyGarden`) that uses the Tiny Garden model, which is much faster (~500ms response), but with much lower parameter accuracy.  The most performant solution is probably to create a custom model that fine-tunes the FunctionGemma-270M model that Tiny Garden is based on.

Uses two different on-device LLM's to turn the spoken command into a launchable Android Intent:
* Sherpa-ONNX converts audio samples into text (STT)
* LiteRT extracts timer parameters from the transcribed text.
------
**Setup:**  
* You will need to download the LLM models and manually copy them to the appropriate Asset directory because the models exceed the GitHub 100MB file size limit.
  * Sherpa-ONNX Model -- sherpa-onnx-streaming-zipformer-en-2023-06-21:
    * Download: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/online-transducer/zipformer-transducer-models.html#id27
    * Asset Directory: `assets/sherpa-onnx-streaming-zipformer-en-2023-06-21`
  * LiteRT Model -- gemma3-1b-it-int4.litertlm:  
    * Download: https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm?download=true  
    * Asset Directory: `assets/litert-community`
* When using an emulator:
  * Enable "Host Microphone Access" in the emulator settings
  * Android API 36 required to run LiteRT.

----
To run the implmentation on the `TinyGarden` branch, you will need the "Tiny Garden" LiteRT model:   
  * LiteRT Model -- gemma3-1b-it-int4.litertlm:  
    * Download: https://huggingface.co/litert-community/functiongemma-270m-ft-tiny-garden/resolve/main/tiny_garden_q8_ekv1024.litertlm
    * Asset Directory: `assets/litert-community`
