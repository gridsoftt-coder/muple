package com.gridsoftt.jinspace

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_OVERLAY = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            // 오버레이 권한 요청 화면으로 이동
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_OVERLAY)
        } else {
            launchService()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                launchService()
            } else {
                Toast.makeText(this, "오버레이 권한이 필요합니다.\n설정에서 허용해주세요.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun launchService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            // 기본값: 현재 시각 + 90분 (서버 연동 전 데모용)
            putExtra("endTimeMs", System.currentTimeMillis() + 90 * 60 * 1000L)
            putExtra("room", "드럼룸 A")
            putExtra("branch", "진스페이스 홍대점")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        finish()
    }
}
