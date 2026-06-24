package com.gridsoftt.jinspace

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var rootView: LinearLayout
    private lateinit var timerText: TextView
    private lateinit var labelText: TextView
    private lateinit var handler: Handler

    private var endTimeMs = 0L
    private var roomName = "드럼룸 A"

    private val CHANNEL_ID = "jinspace_timer"
    private val NOTIF_ID   = 1

    // ─────────────────────────────────────────────────────
    // 서비스 생명주기
    // ─────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            endTimeMs = it.getLongExtra("endTimeMs", System.currentTimeMillis() + 90 * 60 * 1000L)
            roomName  = it.getStringExtra("room") ?: "드럼룸 A"
        }
        // 이미 오버레이가 표시 중이면 시간만 갱신
        if (::timerText.isInitialized) return START_STICKY

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        setupOverlay()
        startTick()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::rootView.isInitialized) {
            runCatching { windowManager.removeView(rootView) }
        }
    }

    // ─────────────────────────────────────────────────────
    // 오버레이 뷰 구성
    // ─────────────────────────────────────────────────────

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 배경 (검정 반투명 + 황색 테두리)
        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#E5000000"))
            cornerRadius = dp(14f)
            setStroke(dp(1f).toInt(), Color.parseColor("#40FBC42C"))
        }

        // 루트 레이아웃
        rootView = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            gravity      = Gravity.CENTER
            setPadding(dp(12f).toInt(), dp(7f).toInt(), dp(12f).toInt(), dp(7f).toInt())
            background   = bg
            elevation    = dp(8f)
        }

        // 타이머 숫자
        timerText = TextView(this).apply {
            text      = "--:--"
            textSize  = 19f
            typeface  = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FBC42C"))
            gravity   = Gravity.CENTER
            letterSpacing = -0.04f
        }

        // "남은시간" 라벨
        labelText = TextView(this).apply {
            text      = "남은시간"
            textSize  = 8f
            setTextColor(Color.parseColor("#55FFFFFF"))
            gravity   = Gravity.CENTER
        }

        rootView.addView(timerText)
        rootView.addView(labelText)

        // WindowManager 파라미터
        val params = WindowManager.LayoutParams(
            dp(96f).toInt(),
            dp(54f).toInt(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x       = dp(20f).toInt()
            y       = dp(40f).toInt()
        }

        // ── 드래그 이동 처리 ──
        var startX = 0; var startY = 0
        var rawX = 0f; var rawY = 0f

        rootView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    rawX   = event.rawX; rawY  = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - rawX).toInt()
                    params.y = startY - (event.rawY - rawY).toInt()
                    windowManager.updateViewLayout(rootView, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(rootView, params)
    }

    // ─────────────────────────────────────────────────────
    // 1초 타이머
    // ─────────────────────────────────────────────────────

    private fun startTick() {
        handler = Handler(Looper.getMainLooper())

        val tick = object : Runnable {
            override fun run() {
                val remSec = ((endTimeMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)

                if (remSec <= 0L) {
                    timerText.text = "종료"
                    timerText.setTextColor(Color.parseColor("#FF4444"))
                    labelText.text = "이용종료"
                    updateBorder(Color.parseColor("#80FF4444"))
                    return // 틱 중지
                }

                // 포맷: 1시간 이상이면 H:MM:SS, 미만이면 MM:SS
                val h = remSec / 3600
                val m = (remSec % 3600) / 60
                val s = remSec % 60
                timerText.text = if (h > 0) "%d:%02d:%02d".format(h, m, s)
                                 else       "%02d:%02d".format(m, s)

                // 남은 시간에 따른 색상 경고
                when {
                    remSec <= 5 * 60  -> {  // 5분 미만 — 빨간 경고
                        timerText.setTextColor(Color.parseColor("#FF4444"))
                        labelText.text = "⚠ 곧 종료"
                        updateBorder(Color.parseColor("#80FF4444"))
                    }
                    remSec <= 10 * 60 -> {  // 10분 미만 — 주황
                        timerText.setTextColor(Color.parseColor("#FF8C00"))
                        labelText.text = "남은시간"
                        updateBorder(Color.parseColor("#60FF8C00"))
                    }
                    else -> {               // 정상 — 노란색
                        timerText.setTextColor(Color.parseColor("#FBC42C"))
                        labelText.text = "남은시간"
                        updateBorder(Color.parseColor("#40FBC42C"))
                    }
                }

                handler.postDelayed(this, 1000L)
            }
        }

        handler.post(tick)
    }

    private fun updateBorder(color: Int) {
        (rootView.background as? GradientDrawable)?.setStroke(dp(1.5f).toInt(), color)
    }

    // ─────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────

    private fun dp(v: Float) = v * resources.displayMetrics.density

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "JinSpace 남은시간",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "이용 중 남은시간을 화면에 표시합니다."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JinSpace")
            .setContentText("남은시간 표시 중")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
}
