package com.ustad.personalassistant.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ustad.personalassistant.messaging.MessagingNotificationStore

class UstadNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) { MessagingNotificationStore.ingest(sbn) }
    override fun onNotificationRemoved(sbn: StatusBarNotification) { /* bounded in-memory store intentionally retains recent message notifications */ }
}
