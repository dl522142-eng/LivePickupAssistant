package com.livepickup.assistant.util

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.livepickup.assistant.service.PickupAccessibilityService
import com.livepickup.assistant.service.PickupNotificationListenerService
import org.json.JSONObject

object PermissionMatrixHelper {

    fun isAccessibilityEnabled(context: Context): Boolean {
        if (PickupAccessibilityService.instance != null) return true
        val expected = ComponentName(context, PickupAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isNotificationListenerConnected(context: Context): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return packages.contains(context.packageName)
    }

    fun isPostNotificationGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun isPromotedLiveWindowEnabled(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return try {
            // Android 16 (API 36) NotificationManager#canPostPromotedNotifications()
            val method = nm.javaClass.getMethod("canPostPromotedNotifications")
            (method.invoke(nm) as? Boolean) ?: isPostNotificationGranted(context)
        } catch (_: Exception) {
            isPostNotificationGranted(context)
        }
    }

    fun getStatusJson(context: Context): String {
        val prefs = context.getSharedPreferences(
            PickupAccessibilityService.PREFS_ENGINE,
            Context.MODE_PRIVATE
        )
        val obj = JSONObject().apply {
            put("accessibility", isAccessibilityEnabled(context))
            put("battery", isBatteryUnrestricted(context))
            put("notificationListener", isNotificationListenerConnected(context))
            put("postNotification", isPostNotificationGranted(context))
            put("liveWindow", isPromotedLiveWindowEnabled(context))
            put(
                "engineAccessibilityOcr",
                prefs.getBoolean(PickupAccessibilityService.KEY_ACCESSIBILITY_OCR_ENABLED, true)
            )
            put(
                "engineWechatPush",
                prefs.getBoolean(PickupAccessibilityService.KEY_WECHAT_PUSH_ENABLED, true)
            )
        }
        return obj.toString()
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openPromotedLiveWindowSettings(context: Context) {
        try {
            // Android 16 dedicated Promoted Notification settings intent
            val intent = Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS").apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppNotificationSettings(context)
        }
    }
}
