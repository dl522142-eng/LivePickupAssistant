package com.livepickup.assistant.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.livepickup.assistant.MainActivity
import com.livepickup.assistant.R
import com.livepickup.assistant.model.BrandCatalog
import com.livepickup.assistant.receiver.ConfirmPickupReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class ActiveOrder(
    val notificationId: Int,
    val brandName: String,
    val code: String,
    val timestamp: Long
)

object LiveIslandManager {
    const val CHANNEL_ID = "live_pickup_island_channel_v1"
    private const val CHANNEL_NAME = "实况取餐岛 · 实时胶囊通知"
    private const val MAX_CONCURRENT_ISLANDS = 6
    private const val BASE_NOTIFICATION_ID = 20260

    private val activeOrders = ConcurrentHashMap<Int, ActiveOrder>()

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于展示取餐码状态栏实况胶囊、锁屏 Now Bar 及实时取餐提醒"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun postOrderToIsland(
        context: Context,
        brandNameInput: String,
        codeInput: String,
        showToast: Boolean = true
    ): Int {
        val brandName = brandNameInput.trim().ifEmpty { "测试商户" }
        val code = codeInput.trim().ifEmpty { "A088" }

        ensureChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Find existing notification for same brand+code or allocate one of the 6 slots
        val existingEntry = activeOrders.values.firstOrNull {
            it.brandName == brandName && it.code.equals(code, ignoreCase = true)
        }

        val slotId = when {
            existingEntry != null -> existingEntry.notificationId
            activeOrders.size < MAX_CONCURRENT_ISLANDS -> {
                val usedIds = activeOrders.keys
                (0 until MAX_CONCURRENT_ISLANDS)
                    .map { BASE_NOTIFICATION_ID + it }
                    .firstOrNull { it !in usedIds }
                    ?: (BASE_NOTIFICATION_ID + (activeOrders.size % MAX_CONCURRENT_ISLANDS))
            }
            else -> {
                // Evict oldest order if already 6 active islands
                val oldest = activeOrders.values.minByOrNull { it.timestamp }
                oldest?.notificationId ?: BASE_NOTIFICATION_ID
            }
        }

        val matchedBrand = BrandCatalog.findBrandByNameOrText(brandName)
        val accentColor = matchedBrand?.primaryColor ?: Color.parseColor("#0D6EFD")
        val largeIcon = BrandCatalog.renderBrandBitmap(context, brandName)

        // Content intent -> open MainActivity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            slotId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action intent -> "确认取餐"
        val confirmIntent = Intent(context, ConfirmPickupReceiver::class.java).apply {
            action = ConfirmPickupReceiver.ACTION_CONFIRM_PICKUP
            putExtra(ConfirmPickupReceiver.EXTRA_NOTIFICATION_ID, slotId)
            putExtra(ConfirmPickupReceiver.EXTRA_BRAND_NAME, brandName)
            putExtra(ConfirmPickupReceiver.EXTRA_PICKUP_CODE, code)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            slotId + 1000,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentSubtitle = "$brandName · 请前往柜台核对领取"

        // Extras for Android 16 Promoted Ongoing Notification + Samsung One UI 8/9 Live Notification / Now Bar
        val liveExtras = Bundle().apply {
            // Android 16 standard Live Update / Promoted Ongoing Notification extras
            putBoolean("android.requestPromotedOngoing", true)
            putString("android.shortCriticalText", code)
            // Samsung One UI 8+ / 9.0 Now Bar & Live Notification capsule extras
            putInt("android.ongoingActivityNoti.style", 1)
            putString("android.ongoingActivityNoti.primaryInfo", code)
            putString("android.ongoingActivityNoti.secondaryInfo", contentSubtitle)
            putString("android.ongoingActivityNoti.chipText", code)
            putInt("android.ongoingActivityNoti.chipBgColor", accentColor)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pickup_bag)
            .setLargeIcon(largeIcon)
            .setContentTitle(code)
            .setContentText(contentSubtitle)
            .setSubText(brandName)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(accentColor)
            .setColorized(false)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setShortCriticalText(code)
            .setRequestPromotedOngoing(true)
            .addExtras(liveExtras)
            .setContentIntent(contentPendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "确认取餐",
                    confirmPendingIntent
                ).build()
            )

        val notification = builder.build()
        // Ensure promoted ongoing flags and extras are preserved on the built Notification
        notification.extras.putBoolean("android.requestPromotedOngoing", true)
        notification.extras.putString("android.shortCriticalText", code)

        try {
            nm.notify(slotId, notification)
            activeOrders[slotId] = ActiveOrder(
                notificationId = slotId,
                brandName = brandName,
                code = code,
                timestamp = System.currentTimeMillis()
            )
            if (showToast) {
                Toast.makeText(
                    context.applicationContext,
                    "已触发 [$brandName: $code]，支持多笔并行上岛",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                context.applicationContext,
                "请先授予通知发布权限后再试",
                Toast.LENGTH_SHORT
            ).show()
        }

        return slotId
    }

    fun confirmOrder(context: Context, notificationId: Int, brandName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
        activeOrders.remove(notificationId)
        Toast.makeText(
            context.applicationContext,
            "$brandName 取餐已确认",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun clearAllOrders(context: Context, showToast: Boolean = true) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        for (id in activeOrders.keys) {
            nm.cancel(id)
        }
        for (i in 0 until MAX_CONCURRENT_ISLANDS) {
            nm.cancel(BASE_NOTIFICATION_ID + i)
        }
        activeOrders.clear()
        if (showToast) {
            Toast.makeText(
                context.applicationContext,
                "已清除全部取餐状态 (全部下岛)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun getActiveOrdersJson(): String {
        val arr = JSONArray()
        activeOrders.values.sortedByDescending { it.timestamp }.forEach { order ->
            val obj = JSONObject()
            obj.put("id", order.notificationId)
            obj.put("brand", order.brandName)
            obj.put("code", order.code)
            arr.put(obj)
        }
        return arr.toString()
    }
}
