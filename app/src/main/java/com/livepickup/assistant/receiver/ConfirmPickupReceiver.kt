package com.livepickup.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.livepickup.assistant.notification.LiveIslandManager

class ConfirmPickupReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CONFIRM_PICKUP = "com.livepickup.assistant.ACTION_CONFIRM_PICKUP"
        const val ACTION_CLEAR_ALL_PICKUP = "com.livepickup.assistant.ACTION_CLEAR_ALL_PICKUP"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_BRAND_NAME = "extra_brand_name"
        const val EXTRA_PICKUP_CODE = "extra_pickup_code"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_CONFIRM_PICKUP -> {
                val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                val brand = intent.getStringExtra(EXTRA_BRAND_NAME) ?: "商户"
                if (id != -1) {
                    LiveIslandManager.confirmOrder(context, id, brand)
                }
            }
            ACTION_CLEAR_ALL_PICKUP -> {
                LiveIslandManager.clearAllOrders(context, showToast = true)
            }
        }
    }
}
