package com.livepickup.assistant.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.livepickup.assistant.notification.LiveIslandManager
import com.livepickup.assistant.parser.PickupCodeParser
import com.livepickup.assistant.parser.SemanticNode
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class PickupAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: PickupAccessibilityService? = null
            private set

        const val PREFS_ENGINE = "pickup_engine_prefs"
        const val KEY_ACCESSIBILITY_OCR_ENABLED = "accessibility_ocr_enabled"
        const val KEY_WECHAT_PUSH_ENABLED = "wechat_push_enabled"

        fun isAccessibilityOcrEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_ENGINE, Context.MODE_PRIVATE)
                .getBoolean(KEY_ACCESSIBILITY_OCR_ENABLED, true)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val screenshotExecutor = Executors.newSingleThreadExecutor()
    private var lastScanTimestamp = 0L
    private var lastPostedKey = ""
    private var lastPostedTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!isAccessibilityOcrEnabled(this)) return

        val now = System.currentTimeMillis()
        if (now - lastScanTimestamp < 900L) return
        lastScanTimestamp = now

        val pkg = event.packageName?.toString() ?: ""
        // Ignore our own app to avoid scanning the simulation buttons automatically
        if (pkg == packageName || pkg.contains("systemui")) return

        val root = rootInActiveWindow ?: return
        val nodes = mutableListOf<SemanticNode>()
        collectNodes(root, nodes)

        val result = PickupCodeParser.parseFromNodes(nodes, allowLargeFontFallback = false)
        if (result != null) {
            postDedupOrder(result.brandName, result.pickupCode)
        } else if (shouldTriggerOnDemandOcr(pkg, nodes)) {
            triggerVisualOcrScan(manualTileTrigger = false)
        }
    }

    /**
     * Invoked either automatically when a mini-program pickup page hides node text in Canvas/WebView,
     * or manually when the user taps the "取餐码扫描" Quick Settings Tile in Control Center.
     */
    fun triggerVisualOcrScan(manualTileTrigger: Boolean) {
        val root = rootInActiveWindow
        if (root != null) {
            val nodes = mutableListOf<SemanticNode>()
            collectNodes(root, nodes)
            val nodeResult = PickupCodeParser.parseFromNodes(nodes, allowLargeFontFallback = manualTileTrigger)
            if (nodeResult != null) {
                postDedupOrder(nodeResult.brandName, nodeResult.pickupCode, forceToast = manualTileTrigger)
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                screenshotExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        try {
                            val hwBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val hwBitmap = Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                            hwBuffer.close()
                            if (hwBitmap != null) {
                                val softBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                hwBitmap.recycle()
                                dispatchBitmapToOcrProcess(softBitmap, manualTileTrigger)
                            }
                        } catch (_: Exception) {
                        }
                    }

                    override fun onFailure(errorCode: Int) {}
                }
            )
        }
    }

    private fun dispatchBitmapToOcrProcess(bitmap: Bitmap, manualTileTrigger: Boolean) {
        try {
            val cacheFile = File(cacheDir, "ocr_frame_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()

            val intent = Intent(this, OcrOnDemandService::class.java).apply {
                putExtra(OcrOnDemandService.EXTRA_IMAGE_PATH, cacheFile.absolutePath)
                putExtra(OcrOnDemandService.EXTRA_MANUAL_TRIGGER, manualTileTrigger)
            }
            startService(intent)
        } catch (_: Exception) {
        }
    }

    private fun shouldTriggerOnDemandOcr(pkg: String, nodes: List<SemanticNode>): Boolean {
        val isMiniProgramHost = pkg.contains("tencent.mm") || pkg.contains("AlipayGphone")
        if (!isMiniProgramHost) return false
        val joined = nodes.joinToString(" ") { it.text }
        return joined.contains("取餐") || joined.contains("取茶") || joined.contains("点餐") || joined.contains("制作中")
    }

    private fun postDedupOrder(brand: String, code: String, forceToast: Boolean = false) {
        val key = "$brand:$code"
        val now = System.currentTimeMillis()
        if (!forceToast && key == lastPostedKey && (now - lastPostedTime) < 45_000L) {
            return
        }
        lastPostedKey = key
        lastPostedTime = now
        mainHandler.post {
            LiveIslandManager.postOrderToIsland(this, brand, code, showToast = true)
        }
    }

    private fun collectNodes(node: AccessibilityNodeInfo?, out: MutableList<SemanticNode>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (!text.isNullOrEmpty() && rect.width() > 0 && rect.height() > 0) {
            out.add(SemanticNode(text, rect))
        } else if (!desc.isNullOrEmpty() && rect.width() > 0 && rect.height() > 0) {
            out.add(SemanticNode(desc, rect))
        }

        val count = node.childCount
        for (i in 0 until count) {
            collectNodes(node.getChild(i), out)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }
}
