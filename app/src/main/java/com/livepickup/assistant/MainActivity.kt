package com.livepickup.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.livepickup.assistant.model.BrandCatalog
import com.livepickup.assistant.notification.LiveIslandManager
import com.livepickup.assistant.service.PickupAccessibilityService
import com.livepickup.assistant.util.PermissionMatrixHelper

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            syncStateToWeb()
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge immersive transparent status bar & navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        LiveIslandManager.ensureChannel(this)

        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
                textZoom = 100
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    syncStateToWeb()
                }
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(NativeBridge(), "AndroidBridge")
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onResume() {
        super.onResume()
        syncStateToWeb()
    }

    private fun syncStateToWeb() {
        if (!::webView.isInitialized) return
        val statusJson = PermissionMatrixHelper.getStatusJson(this)
        val customLogosJson = BrandCatalog.getCustomLogosJson(this)
        val activeOrdersJson = LiveIslandManager.getActiveOrdersJson()
        webView.post {
            webView.evaluateJavascript(
                "window.__onNativeStateUpdated && window.__onNativeStateUpdated($statusJson, $customLogosJson, $activeOrdersJson);",
                null
            )
        }
    }

    inner class NativeBridge {

        @JavascriptInterface
        fun getInitialState(): String {
            return PermissionMatrixHelper.getStatusJson(this@MainActivity)
        }

        @JavascriptInterface
        fun setStatusBarLightMode(isLightBackground: Boolean) {
            runOnUiThread {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = isLightBackground
                controller.isAppearanceLightNavigationBars = isLightBackground
            }
        }

        @JavascriptInterface
        fun toggleEngineChannel(channelKey: String, enabled: Boolean) {
            val prefs = getSharedPreferences(
                PickupAccessibilityService.PREFS_ENGINE,
                Context.MODE_PRIVATE
            )
            when (channelKey) {
                "accessibility_ocr" -> {
                    prefs.edit()
                        .putBoolean(PickupAccessibilityService.KEY_ACCESSIBILITY_OCR_ENABLED, enabled)
                        .apply()
                }
                "wechat_push" -> {
                    prefs.edit()
                        .putBoolean(PickupAccessibilityService.KEY_WECHAT_PUSH_ENABLED, enabled)
                        .apply()
                }
            }
            syncStateToWeb()
        }

        @JavascriptInterface
        fun triggerIslandOrder(brandName: String, pickupCode: String) {
            runOnUiThread {
                if (!PermissionMatrixHelper.isPostNotificationGranted(this@MainActivity) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@runOnUiThread
                }
                LiveIslandManager.postOrderToIsland(
                    this@MainActivity,
                    brandName,
                    pickupCode,
                    showToast = true
                )
                syncStateToWeb()
            }
        }

        @JavascriptInterface
        fun clearAllIslands() {
            runOnUiThread {
                LiveIslandManager.clearAllOrders(this@MainActivity, showToast = true)
                syncStateToWeb()
            }
        }

        @JavascriptInterface
        fun openPermissionSetting(type: String) {
            runOnUiThread {
                when (type) {
                    "accessibility" -> PermissionMatrixHelper.openAccessibilitySettings(this@MainActivity)
                    "battery" -> PermissionMatrixHelper.requestIgnoreBatteryOptimizations(this@MainActivity)
                    "notificationListener" -> PermissionMatrixHelper.openNotificationListenerSettings(this@MainActivity)
                    "postNotification" -> {
                        if (!PermissionMatrixHelper.isPostNotificationGranted(this@MainActivity) &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionMatrixHelper.openAppNotificationSettings(this@MainActivity)
                        }
                    }
                    "liveWindow" -> PermissionMatrixHelper.openPromotedLiveWindowSettings(this@MainActivity)
                }
            }
        }

        @JavascriptInterface
        fun saveCustomBrandLogo(brandId: String, dataUrl: String) {
            BrandCatalog.saveCustomLogoBase64(this@MainActivity, brandId, dataUrl)
            syncStateToWeb()
        }

        @JavascriptInterface
        fun resetCustomBrandLogo(brandId: String) {
            BrandCatalog.removeCustomLogo(this@MainActivity, brandId)
            syncStateToWeb()
        }
    }
}
