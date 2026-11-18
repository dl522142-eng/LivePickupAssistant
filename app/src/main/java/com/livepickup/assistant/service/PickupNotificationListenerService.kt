package com.livepickup.assistant.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.livepickup.assistant.notification.LiveIslandManager
import com.livepickup.assistant.parser.PickupCodeParser

class PickupNotificationListenerService : NotificationListenerService() {

    companion object {
        fun isWechatPushEnabled(context: Context): Boolean {
            return context.getSharedPreferences(
                PickupAccessibilityService.PREFS_ENGINE,
                Context.MODE_PRIVATE
            ).getBoolean(PickupAccessibilityService.KEY_WECHAT_PUSH_ENABLED, true)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!isWechatPushEnabled(this)) return

        val pkg = sbn.packageName ?: return
        // Ignore notifications posted by ourselves
        if (pkg == packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val content = if (bigText.length > text.length) bigText else text

        if (title.isBlank() && content.isBlank()) return

        val parsed = PickupCodeParser.parseFromNotificationText(title, content, pkg)
        if (parsed != null) {
            LiveIslandManager.postOrderToIsland(
                this,
                parsed.brandName,
                parsed.pickupCode,
                showToast = true
            )
        }
    }
}
