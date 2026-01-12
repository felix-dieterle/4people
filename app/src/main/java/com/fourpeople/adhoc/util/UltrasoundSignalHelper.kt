package com.fourpeople.adhoc.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.sin
import kotlin.math.PI

/**
 * Helper class for ultrasound-based emergency signaling.
 * 
 * Uses high-frequency audio tones (18-22 kHz) for data transmission.
 * These frequencies are typically inaudible to humans but can be detected
 * by most smartphone microphones.
 * 
 * Features:
 * - Emergency beacon transmission
 * - Signal detection
 * - Works through walls (limited)
 * - Works with locked devices (detection only)
 * - Low power consumption
 * - Range: ~5-10 meters
 * 
 * Limitations:
 * - Low bandwidth
 * - Susceptible to noise interference
 * - Reduced effectiveness in noisy environments
 */
class UltrasoundSignalHelper {

    companion object {
        private const val TAG = "UltrasoundSignal"
        
        // Frequency configuration
        private const val BEACON_FREQUENCY = 19000 // Hz (ultrasound range)
        private const val SAMPLE_RATE = 44100      // Hz (standard audio sample rate)
        private const val SIGNAL_DURATION = 500    // ms (duration of each pulse)
        private const val SIGNAL_GAP = 500         // ms (gap between pulses)
        
        // Signal pattern for emergency beacon
        // Pattern: 3 pulses (similar to SOS concept)
        private const val BEACON_PULSE_COUNT = 3
        
        // Detection thresholds
        private const val DETECTION_THRESHOLD = 0.3 // Amplitude threshold for detection
    }

    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private var isTransmitting = false
    private var isListening = false
    private var transmitThread: Thread? = null
    private var listenThread: Thread? = null
    private var onSignalDetected: (() -> Unit)? = null

    /**
     * Start transmitting emergency beacon signal.
     * Sends a repeating pattern of ultrasound pulses.
     */
    fun startTransmitting() {
        if (isTransmitting) {
            Log.w(TAG, "Already transmitting")
            return
        }

        isTransmitting = true
        transmitThread = Thread {
            try {
                initializeAudioTrack()
                
                while (isTransmitting) {
                    sendBeaconPattern()
                    Thread.sleep(3000) // Repeat every 3 seconds
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in transmission thread", e)
            } finally {
                cleanupAudioTrack()
            }
        }
        transmitThread?.start()
        
        Log.i(TAG, "Started ultrasound transmission")
    }

    /**
     * Stop transmitting emergency beacon signal.
     */
    fun stopTransmitting() {
        if (!isTransmitting) return
        
        isTransmitting = false
        transmitThread?.join(1000)
        transmitThread = null
        
        Log.i(TAG, "Stopped ultrasound transmission")
    }

    /**
     * Start listening for emergency beacon signals.
     * Calls the callback when a signal is detected.
     */
    fun startListening(onDetected: () -> Unit) {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }

        onSignalDetected = onDetected
        isListening = true
        
        listenThread = Thread {
            try {
                initializeAudioRecord()
                
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                val audioBuffer = ShortArray(bufferSize)
                
                while (isListening) {
                    val readResult = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                    
                    if (readResult > 0) {
                        if (detectSignal(audioBuffer, readResult)) {
                            Log.i(TAG, "Emergency signal detected!")
                            onSignalDetected?.invoke()
                            // Small delay to avoid multiple rapid detections
                            Thread.sleep(2000)
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in listening thread", e)
            } finally {
                cleanupAudioRecord()
            }
        }
        listenThread?.start()
        
        Log.i(TAG, "Started ultrasound listening")
    }

    /**
     * Stop listening for emergency beacon signals.
     */
    fun stopListening() {
        if (!isListening) return
        
        isListening = false
        listenThread?.join(1000)
        listenThread = null
        onSignalDetected = null
        
        Log.i(TAG, "Stopped ultrasound listening")
    }

    /**
     * Send emergency beacon pattern (3 pulses).
     */
    private fun sendBeaconPattern() {
        repeat(BEACON_PULSE_COUNT) { pulseIndex ->
            if (!isTransmitting) return
            
            sendTone(BEACON_FREQUENCY, SIGNAL_DURATION.toLong())
            
            if (pulseIndex < BEACON_PULSE_COUNT - 1) {
                Thread.sleep(SIGNAL_GAP.toLong())
            }
        }
    }

    /**
     * Generate and play a tone at specified frequency and duration.
     */
    private fun sendTone(frequency: Int, durationMs: Long) {
        try {
            val numSamples = (durationMs * SAMPLE_RATE / 1000).toInt()
            val samples = generateSineWave(frequency, numSamples)
            
            audioTrack?.write(samples, 0, samples.size)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending tone", e)
        }
    }

    /**
     * Generate sine wave samples for given frequency.
     */
    private fun generateSineWave(frequency: Int, numSamples: Int): ShortArray {
        val samples = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i * frequency / SAMPLE_RATE
            samples[i] = (sin(angle) * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
        
        return samples
    }

    /**
     * Detect emergency signal in audio buffer using frequency analysis.
     * Simplified detection based on amplitude in ultrasound range.
     */
    private fun detectSignal(buffer: ShortArray, length: Int): Boolean {
        // Calculate average amplitude
        var sum = 0.0
        for (i in 0 until length) {
            sum += kotlin.math.abs(buffer[i].toDouble())
        }
        val avgAmplitude = sum / length
        
        // Normalize to 0-1 range
        val normalizedAmplitude = avgAmplitude / Short.MAX_VALUE
        
        // Simple threshold-based detection
        // In production, would use FFT for frequency-specific detection
        return normalizedAmplitude > DETECTION_THRESHOLD
    }

    /**
     * Initialize AudioTrack for sound generation.
     */
    private fun initializeAudioTrack() {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
        }
    }

    /**
     * Initialize AudioRecord for sound detection.
     */
    private fun initializeAudioRecord() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioRecord?.startRecording()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioRecord", e)
        }
    }

    /**
     * Clean up AudioTrack resources.
     */
    private fun cleanupAudioTrack() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioTrack", e)
        }
    }

    /**
     * Clean up AudioRecord resources.
     */
    private fun cleanupAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioRecord", e)
        }
    }

    /**
     * Clean up all resources.
     */
    fun cleanup() {
        stopTransmitting()
        stopListening()
    }

    /**
     * Check if device supports ultrasound (has microphone and speaker).
     */
    fun isSupported(): Boolean {
        return try {
            // Check if we can create audio track and record
            val trackBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val recordBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            trackBufferSize > 0 && recordBufferSize > 0
        } catch (e: Exception) {
            false
        }
    }
}
