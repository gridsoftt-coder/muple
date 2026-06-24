package com.gridsoftt.jinspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val serviceIntent = Intent(context, OverlayService::class.java)
            // 부팅 후에는 마지막 예약 정보가 없으므로 서비스만 시작
            // (추후 SharedPreferences로 마지막 예약 복원 가능)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
