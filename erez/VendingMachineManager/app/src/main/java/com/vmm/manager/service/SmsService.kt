package com.vmm.manager.service

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun sendSms(to: String, text: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                context.getSystemService(SmsManager::class.java)
            else @Suppress("DEPRECATION") SmsManager.getDefault()

            // Encode as UTF-8 to support Hebrew/non-ASCII characters
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(to, null, parts, null, null)
            Timber.i("SMS sent to $to [${text.length} chars]")
        } catch (e: Exception) {
            Timber.e(e, "SMS failed to $to")
        }
    }
}
