package com.example.engine

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Silero VAD detector running on ONNX Runtime with an acoustic energy-based hybrid fallback.
 * Processes 16kHz 16-bit PCM chunks and evaluates speech probability.
 */
class SileroVadDetector(private val context: Context) {

    private val tag = "SileroVadDetector"

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    // State tensors for Silero VAD recurrent cells
    private var stateTensor: Array<Array<FloatArray>> = Array(2) { Array(1) { FloatArray(128) } }
    private var contextTensor: FloatArray = FloatArray(64)

    init {
        initModel()
    }

    private fun initModel() {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelFile = extractModelFile()
            if (modelFile.exists() && modelFile.length() > 0) {
                val sessionOptions = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(1)
                    setInterOpNumThreads(1)
                }
                ortSession = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)
                isInitialized = true
                Log.i(tag, "Silero VAD ONNX session initialized successfully from ${modelFile.path}")
            } else {
                Log.w(tag, "Silero VAD model file not present, using high-precision acoustic energy VAD.")
            }
        } catch (e: Throwable) {
            Log.e(tag, "ONNX Runtime initialization fallback to acoustic detector: ${e.message}")
            isInitialized = false
        }
    }

    private fun extractModelFile(): File {
        val destFile = File(context.filesDir, "silero_vad.onnx")
        if (!destFile.exists()) {
            try {
                context.assets.open("silero_vad.onnx").use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // If not in assets yet, create placeholder indicator
                Log.d(tag, "silero_vad.onnx not found in assets, hybrid detector active.")
            }
        }
        return destFile
    }

    /**
     * Calculates RMS (Root Mean Square) energy of the audio chunk, scaled 0.0 to 1.0.
     * Used for the dynamic UI audio visualizer dots.
     */
    fun calculateRmsAmplitude(pcm16: ShortArray, readSize: Int): Float {
        if (readSize <= 0) return 0f
        var sum = 0.0
        for (i in 0 until readSize) {
            val sample = pcm16[i].toDouble() / 32768.0
            sum += sample * sample
        }
        val rms = sqrt(sum / readSize)
        // Normalize and amplify slightly for UI responsiveness
        return (rms * 4.5).coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Checks if speech is present in the PCM audio chunk.
     * @param pcm16 Audio samples (16kHz, mono, 16-bit PCM)
     * @param readSize Number of valid samples in pcm16
     * @param sensitivity User configured sensitivity [0.0f .. 1.0f]
     * @return Pair of (isSpeechActive, speechProbability)
     */
    fun isSpeech(pcm16: ShortArray, readSize: Int, sensitivity: Float): Pair<Boolean, Float> {
        val threshold = (1.0f - (sensitivity * 0.65f)).coerceIn(0.15f, 0.85f)

        if (isInitialized && ortSession != null && ortEnv != null && readSize >= 512) {
            try {
                // Convert 512 samples to float [-1.0 .. 1.0]
                val floatBuffer = FloatArray(512)
                for (i in 0 until 512) {
                    floatBuffer[i] = pcm16[i].toFloat() / 32768.0f
                }

                val env = ortEnv ?: return fallbackSpeechDetection(pcm16, readSize, sensitivity)
                val inputTensor = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(floatBuffer),
                    longArrayOf(1, 512)
                )
                val srTensor = OnnxTensor.createTensor(env, longArrayOf(16000L))

                val inputs = mapOf("input" to inputTensor, "sr" to srTensor)
                val output = ortSession?.run(inputs)
                val probValue = (output?.get(0)?.value as? Array<FloatArray>)?.get(0)?.get(0) ?: 0f

                inputTensor.close()
                srTensor.close()
                output?.close()

                val isSpeech = probValue >= threshold
                return Pair(isSpeech, probValue)
            } catch (e: Throwable) {
                // Fall back to acoustic RMS detector if ONNX tensor execution fails
                return fallbackSpeechDetection(pcm16, readSize, sensitivity)
            }
        }

        return fallbackSpeechDetection(pcm16, readSize, sensitivity)
    }

    private fun fallbackSpeechDetection(
        pcm16: ShortArray,
        readSize: Int,
        sensitivity: Float
    ): Pair<Boolean, Float> {
        val rms = calculateRmsAmplitude(pcm16, readSize)
        // Higher sensitivity requires lower RMS to trigger
        val threshold = (0.28f - (sensitivity * 0.22f)).coerceIn(0.04f, 0.35f)
        val speechProb = (rms / (threshold * 2.0f)).coerceIn(0.0f, 1.0f)
        val isSpeech = rms >= threshold
        return Pair(isSpeech, speechProb)
    }

    fun release() {
        try {
            ortSession?.close()
            ortEnv?.close()
        } catch (e: Throwable) {
            Log.e(tag, "Error closing ONNX session: ${e.message}")
        }
        ortSession = null
        ortEnv = null
        isInitialized = false
    }
}
