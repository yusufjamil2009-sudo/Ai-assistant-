package com.ustad.personalassistant.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.provider.Telephony
import com.ustad.personalassistant.messaging.MessagingNotificationStore

class UstadNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        MessagingNotificationStore.ingest(sbn, Telephony.Sms.getDefaultSmsPackage(this))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) { /* bounded in-memory store intentionally retains recent message notifications */ }
}
