package com.vmm.manager.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vmm.manager.R
import com.vmm.manager.data.db.entities.Customer
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsService: SmsService
) {
    companion object {
        const val CHANNEL_ALERTS = "vmm_alerts"
        const val CHANNEL_ORDERS = "vmm_orders"
    }

    // All strings resolved from strings.xml (UTF-8 declared XML)
    private fun str(id: Int)              = context.getString(id)
    private fun str(id: Int, vararg a: Any) = context.getString(id, *a)

    init { createChannels() }

    private fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Machine Alerts", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ORDERS, "Order Updates", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun showLocalAlert(title: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    suspend fun notifyCustomerPurchase(customer: Customer, productName: String, amount: Double) {
        val title = str(R.string.notif_purchase_approved)
        val body  = "$productName ב-₪${"%.2f".format(amount)}" // "ב-₪X.XX"

        if (customer.pushEnabled && customer.fcmToken.isNotBlank()) {
            sendFcmToToken(token = customer.fcmToken, title = title, body = body)
        }
        if (customer.smsEnabled && customer.phone.isNotBlank()) {
            // SMS text: "VMM: <product> ב-₪<amount> אושרה. תודה!"
            val thanks = "אושרה. תודה!" // "אושרה. תודה!"
            smsService.sendSms(to = customer.phone, text = "VMM: $body $thanks")
        }
    }

    suspend fun sendFcmToToken(token: String, title: String, body: String) {
        try {
            Timber.i("FCM -> $token: $title / $body")
            // Production: call your server -> FCM HTTP v1 API
        } catch (e: Exception) {
            Timber.e(e, "FCM send failed")
        }
    }

    fun notifyLowStock(machineName: String, productName: String, remaining: Int) {
        showLocalAlert(
            title   = str(R.string.notif_low_stock_title, machineName),
            message = str(R.string.notif_low_stock_body, productName, remaining)
        )
    }

    fun notifyMachineError(machineName: String, errorCode: Byte) {
        showLocalAlert(
            title   = str(R.string.notif_machine_error_title, machineName),
            message = str(R.string.notif_machine_error_body, errorCode.toInt())
        )
    }
}
