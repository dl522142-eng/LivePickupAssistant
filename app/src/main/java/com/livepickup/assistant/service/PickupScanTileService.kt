package com.livepickup.assistant.service

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.livepickup.assistant.MainActivity

class PickupScanTileService : TileService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "取餐码扫描"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = "轻点识屏上岛"
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val a11yService = PickupAccessibilityService.instance
        if (a11yService != null) {
            Toast.makeText(
                applicationContext,
                "正在执行高字号视觉扫描...",
                Toast.LENGTH_SHORT
            ).show()
            // Wait briefly for the Quick Settings shade to collapse before capturing
            handler.postDelayed({
                a11yService.triggerVisualOcrScan(manualTileTrigger = true)
            }, 550L)
        } else {
            Toast.makeText(
                applicationContext,
                "请先开启「无障碍智能识屏服务」权限",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }
}
