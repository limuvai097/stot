package com.tigerworkshop.sms2telegram.sms

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.tigerworkshop.sms2telegram.data.PendingMessageOutbox
import com.tigerworkshop.sms2telegram.data.SettingsRepository
import com.tigerworkshop.sms2telegram.data.StatusUpdateBus
import com.tigerworkshop.sms2telegram.data.TelegramDeliveryWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        
        // Google Messages, Samsung Messages বা যেকোনো ডিফল্ট মেসেজ অ্যাপের নোটিফিকেশন ফিল্টার
        if (packageName.contains("messaging") || packageName.contains("sms") || packageName.contains("dialer")) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: return
            val text = extras.getCharSequence("android.text")?.toString() ?: return

            val appContext = applicationContext
            val repository = SettingsRepository(appContext)

            if (!repository.isForwardingEnabled() || repository.loadSettings() == null) return

            val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US)
            val formattedMessage = buildString {
                appendLine("From: $title (Notification)")
                appendLine("Time: ${timeFormatter.format(Date())}")
                appendLine()
                append(text)
            }

            val outbox = PendingMessageOutbox(appContext)
            outbox.enqueue(sender = title, message = formattedMessage)
            
            StatusUpdateBus.notifyUpdated()
                TelegramDeliveryWorker.enqueue(appContext)
        }
    }
}
