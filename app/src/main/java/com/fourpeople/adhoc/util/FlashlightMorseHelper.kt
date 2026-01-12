package com.fourpeople.adhoc.util

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Helper class for flashlight-based Morse code signaling.
 * 
 * Provides visual emergency signaling using the device's flashlight/LED.
 * Useful for long-range communication (up to 1km with line of sight).
 * Works as a backup when other communication methods fail.
 * 
 * Features:
 * - SOS signal (standard international distress signal)
 * - Custom emergency identification pattern
 * - Low power consumption
 * - No network required
 */
class FlashlightMorseHelper(private val context: Context) {

    companion object {
        private const val TAG = "FlashlightMorse"
        
        // Morse code timing constants (in milliseconds)
        private const val DOT_DURATION = 200L        // Short flash
        private const val DASH_DURATION = 600L       // Long flash (3x dot)
        private const val SYMBOL_GAP = 200L          // Gap between dots/dashes in same letter
        private const val LETTER_GAP = 600L          // Gap between letters
        private const val WORD_GAP = 1400L           // Gap between words
        
        // Signal patterns
        private const val PATTERN_SOS = "SOS"
        private const val PATTERN_4PEOPLE = "4PEOPLE"
        
        // Morse code mapping
        private val MORSE_CODE = mapOf(
            'A' to ".-",    'B' to "-...",  'C' to "-.-.",  'D' to "-..",
            'E' to ".",     'F' to "..-.",  'G' to "--.",   'H' to "....",
            'I' to "..",    'J' to ".---",  'K' to "-.-",   'L' to ".-..",
            'M' to "--",    'N' to "-.",    'O' to "---",   'P' to ".--.",
            'Q' to "--.-",  'R' to ".-.",   'S' to "...",   'T' to "-",
            'U' to "..-",   'V' to "...-",  'W' to ".--",   'X' to "-..-",
            'Y' to "-.--",  'Z' to "--..",
            '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
            '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
            '8' to "---..", '9' to "----.",
            ' ' to " "
        )
    }

    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isSignaling = false
    private var signalingRunnable: Runnable? = null
    private var signalingThread: Thread? = null

    init {
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize camera manager", e)
        }
    }

    /**
     * Start continuous SOS signaling.
     * SOS pattern: ... --- ... (3 short, 3 long, 3 short)
     */
    fun startSOSSignal() {
        if (isSignaling) {
            Log.w(TAG, "Already signaling")
            return
        }
        
        isSignaling = true
        signalingThread = Thread {
            try {
                while (isSignaling) {
                    sendSOSPattern()
                    // Repeat SOS every 5 seconds
                    Thread.sleep(5000)
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "SOS signaling thread interrupted")
            } catch (e: Exception) {
                Log.e(TAG, "Error in SOS signaling", e)
            }
        }
        signalingThread?.start()
        
        Log.i(TAG, "Started SOS signaling")
    }

    /**
     * Start continuous emergency identification pattern.
     * Signals "4PEOPLE" in Morse code to identify emergency network.
     */
    fun startEmergencyIdentificationSignal() {
        if (isSignaling) {
            Log.w(TAG, "Already signaling")
            return
        }
        
        isSignaling = true
        signalingThread = Thread {
            try {
                while (isSignaling) {
                    sendMorseText(PATTERN_4PEOPLE)
                    // Repeat every 10 seconds
                    Thread.sleep(10000)
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Emergency identification signaling thread interrupted")
            } catch (e: Exception) {
                Log.e(TAG, "Error in emergency identification signaling", e)
            }
        }
        signalingThread?.start()
        
        Log.i(TAG, "Started emergency identification signaling")
    }

    /**
     * Stop all flashlight signaling.
     */
    fun stopSignal() {
        if (!isSignaling) return
        
        isSignaling = false
        
        // Interrupt the signaling thread
        signalingThread?.interrupt()
        signalingThread?.join(1000)
        signalingThread = null
        
        // Ensure flashlight is turned off
        setFlashlight(false)
        
        Log.i(TAG, "Stopped signaling")
    }

    /**
     * Send a single SOS pattern.
     */
    private fun sendSOSPattern() {
        try {
            // S (...)
            flash(DOT_DURATION)
            delay(SYMBOL_GAP)
            flash(DOT_DURATION)
            delay(SYMBOL_GAP)
            flash(DOT_DURATION)
            delay(LETTER_GAP)
            
            // O (---)
            flash(DASH_DURATION)
            delay(SYMBOL_GAP)
            flash(DASH_DURATION)
            delay(SYMBOL_GAP)
            flash(DASH_DURATION)
            delay(LETTER_GAP)
            
            // S (...)
            flash(DOT_DURATION)
            delay(SYMBOL_GAP)
            flash(DOT_DURATION)
            delay(SYMBOL_GAP)
            flash(DOT_DURATION)
            delay(WORD_GAP)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SOS pattern", e)
        }
    }

    /**
     * Send text as Morse code.
     */
    private fun sendMorseText(text: String) {
        try {
            for (i in text.indices) {
                if (!isSignaling) break
                
                val char = text[i].uppercaseChar()
                val morseCode = MORSE_CODE[char]
                
                if (morseCode != null) {
                    if (morseCode == " ") {
                        delay(WORD_GAP)
                    } else {
                        sendMorseSymbols(morseCode)
                        if (i < text.length - 1) {
                            delay(LETTER_GAP)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending Morse text", e)
        }
    }

    /**
     * Send Morse code symbols (dots and dashes).
     */
    private fun sendMorseSymbols(symbols: String) {
        for (i in symbols.indices) {
            if (!isSignaling) break
            
            when (symbols[i]) {
                '.' -> flash(DOT_DURATION)
                '-' -> flash(DASH_DURATION)
            }
            
            if (i < symbols.length - 1) {
                delay(SYMBOL_GAP)
            }
        }
    }

    /**
     * Flash the LED for specified duration.
     */
    private fun flash(duration: Long) {
        setFlashlight(true)
        Thread.sleep(duration)
        setFlashlight(false)
    }

    /**
     * Delay between flashes.
     */
    private fun delay(duration: Long) {
        Thread.sleep(duration)
    }

    /**
     * Control flashlight on/off state.
     */
    private fun setFlashlight(on: Boolean) {
        try {
            cameraId?.let { id ->
                cameraManager?.setTorchMode(id, on)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error controlling flashlight", e)
        }
    }

    /**
     * Check if flashlight is available on this device.
     */
    fun isFlashlightAvailable(): Boolean {
        return try {
            cameraId != null && 
                   context.packageManager.hasSystemFeature("android.hardware.camera.flash")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stopSignal()
        cameraManager = null
        cameraId = null
    }
}
