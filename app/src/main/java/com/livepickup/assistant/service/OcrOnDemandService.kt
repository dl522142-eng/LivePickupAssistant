package com.livepickup.assistant.service

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.livepickup.assistant.notification.LiveIslandManager
import com.livepickup.assistant.parser.PickupCodeParser
import com.livepickup.assistant.parser.SemanticNode
import java.io.File
import kotlin.system.exitProcess

/**
 * Runs in independent process ":ocr".
 * Automatically unloads and exits process after 30s idle so 0MB RAM is used when idle.
 */
class OcrOnDemandService : Service() {

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_MANUAL_TRIGGER = "extra_manual_trigger"
        private const val IDLE_UNLOAD_DELAY_MS = 30_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val unloadRunnable = Runnable {
        stopSelf()
        exitProcess(0)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resetIdleUnloadTimer()

        val imagePath = intent?.getStringExtra(EXTRA_IMAGE_PATH)
        val manualTrigger = intent?.getBooleanExtra(EXTRA_MANUAL_TRIGGER, false) ?: false

        if (!imagePath.isNullOrEmpty()) {
            processImageFile(File(imagePath), manualTrigger)
        }

        return START_NOT_STICKY
    }

    private fun processImageFile(file: File, manualTrigger: Boolean) {
        if (!file.exists()) return
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        file.delete()
        if (bitmap == null) return

        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val nodes = mutableListOf<SemanticNode>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val rect = line.boundingBox ?: Rect(0, 0, 100, 60)
                        nodes.add(SemanticNode(line.text, rect))
                    }
                }
                val parsed = PickupCodeParser.parseFromNodes(nodes, allowLargeFontFallback = true)
                if (parsed != null) {
                    LiveIslandManager.postOrderToIsland(
                        applicationContext,
                        parsed.brandName,
                        parsed.pickupCode,
                        showToast = true
                    )
                } else if (manualTrigger) {
                    Toast.makeText(
                        applicationContext,
                        "当前屏幕未检测到有效取餐码",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                bitmap.recycle()
                recognizer.close()
                resetIdleUnloadTimer()
            }
            .addOnFailureListener {
                bitmap.recycle()
                recognizer.close()
                resetIdleUnloadTimer()
            }
    }

    private fun resetIdleUnloadTimer() {
        handler.removeCallbacks(unloadRunnable)
        handler.postDelayed(unloadRunnable, IDLE_UNLOAD_DELAY_MS)
    }
}
