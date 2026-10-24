package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.assistant.AssistantStatus
import com.example.assistant.MimiAssistant
import com.example.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class FloatingMimiService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var assistant: MimiAssistant? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isExpanded = false
    private var statusTextView: TextView? = null
    private var controlsLayout: LinearLayout? = null
    private var muteIconView: ImageView? = null
    private var orbCanvasView: OrbView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            startForegroundServiceNotification()
            assistant = MimiAssistant.activeInstance ?: MimiAssistant(this, SettingsRepository(this))
            setupFloatingView()
            observeAssistant()
        } catch (e: Throwable) {
            Log.e("FloatingMimiService", "Error during service onCreate", e)
            stopSelf()
        }
    }

    private fun startForegroundServiceNotification() {
        try {
            val channelId = "rin_ai_floating_service_channel"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "RIN AI Mimi Floating Assistant",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps Mimi floating orb available on home screen for quick voice commands"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("RIN AI — Mimi Floating Orb")
                .setContentText("Mimi is active on screen. Tap orb for voice commands.")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Throwable) {
            Log.e("FloatingMimiService", "Failed starting foreground notification", e)
        }
    }

    private fun setupFloatingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w("FloatingMimiService", "Overlay permission not granted; cannot add view to window manager")
            return
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 30
                y = 350
            }
            params = lp

            val root = FrameLayout(this)

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            // Orb Canvas
            val orb = OrbView(this).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(76), dpToPx(76))
            }
            orbCanvasView = orb

            // Expanded Card
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                setBackgroundColor(Color.parseColor("#E60D1424"))
            }
            controlsLayout = card

            val tvStatus = TextView(this).apply {
                text = "Mimi Ready"
                setTextColor(Color.parseColor("#00E5FF"))
                textSize = 12f
                setPadding(0, 0, 0, dpToPx(4))
            }
            statusTextView = tvStatus
            card.addView(tvStatus)

            // Row of mini action buttons: Mic, Mute, Open App, Close
            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            // Mic Button
            val micBtn = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setColorFilter(Color.parseColor("#00E5FF"))
                setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                setOnClickListener {
                    assistant?.let { ast ->
                        if (ast.status.value == AssistantStatus.LISTENING) {
                            ast.stopListening()
                        } else {
                            ast.startListening()
                        }
                    }
                }
            }
            btnRow.addView(micBtn)

            // Mute Button
            val muteBtn = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_lock_silent_mode)
                setColorFilter(Color.parseColor("#A855F7"))
                setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                setOnClickListener {
                    assistant?.let { ast ->
                        val muted = ast.toggleMute()
                        updateMuteIcon(muted)
                    }
                }
            }
            muteIconView = muteBtn
            btnRow.addView(muteBtn)

            // Open App Button
            val appBtn = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_agenda)
                setColorFilter(Color.WHITE)
                setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                setOnClickListener {
                    val intent = Intent(this@FloatingMimiService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }
            btnRow.addView(appBtn)

            // Close Floating Button
            val closeBtn = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.parseColor("#F43F5E"))
                setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                setOnClickListener {
                    stopSelf()
                }
            }
            btnRow.addView(closeBtn)

            card.addView(btnRow)

            container.addView(orb)
            container.addView(card)
            root.addView(container)

            // Touch listener for dragging & tapping
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            orb.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = lp.x
                        initialY = lp.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        lp.x = initialX + (event.rawX - initialTouchX).toInt()
                        lp.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager?.updateViewLayout(root, lp)
                        } catch (e: Exception) {
                            // View might not be attached
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = abs(event.rawX - initialTouchX)
                        val diffY = abs(event.rawY - initialTouchY)
                        if (diffX < 15 && diffY < 15) {
                            onOrbTapped()
                        }
                        true
                    }
                    else -> false
                }
            }

            floatingView = root
            windowManager?.addView(root, lp)
            isRunning = true
        } catch (e: Throwable) {
            Log.e("FloatingMimiService", "Failed to add floating view", e)
        }
    }

    private fun onOrbTapped() {
        assistant?.let { ast ->
            if (ast.status.value == AssistantStatus.LISTENING) {
                ast.stopListening()
            } else if (ast.status.value == AssistantStatus.SPEAKING) {
                ast.stopSpeaking()
            } else {
                ast.startListening()
            }
        }
        isExpanded = !isExpanded
        controlsLayout?.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun updateMuteIcon(isMuted: Boolean) {
        muteIconView?.setColorFilter(
            if (isMuted) Color.parseColor("#F43F5E") else Color.parseColor("#A855F7")
        )
    }

    private fun observeAssistant() {
        val ast = assistant ?: return

        scope.launch {
            ast.status.collectLatest { st ->
                orbCanvasView?.setStatus(st)
                val statusStr = when (st) {
                    AssistantStatus.LISTENING -> "Mimi: Listening..."
                    AssistantStatus.THINKING -> "Mimi: Thinking..."
                    AssistantStatus.EXECUTING -> "Mimi: Executing..."
                    AssistantStatus.SPEAKING -> "Mimi: Speaking..."
                    AssistantStatus.IDLE -> "Mimi: Tap to talk"
                }
                statusTextView?.text = statusStr
                if (st == AssistantStatus.LISTENING || st == AssistantStatus.SPEAKING) {
                    controlsLayout?.visibility = View.VISIBLE
                    isExpanded = true
                }
            }
        }

        scope.launch {
            ast.isMuted.collectLatest { muted ->
                updateMuteIcon(muted)
            }
        }

        scope.launch {
            ast.activeTranscript.collectLatest { text ->
                if (text.isNotBlank()) {
                    statusTextView?.text = text
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e("FloatingMimiService", "Error removing floating view", e)
            }
        }
        scope.cancel()
    }

    companion object {
        var isRunning = false
            private set

        fun start(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    Log.w("FloatingMimiService", "Cannot start floating service: overlay permission not granted")
                    return
                }
                val intent = Intent(context, FloatingMimiService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e("FloatingMimiService", "Failed to start FloatingMimiService safely", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, FloatingMimiService::class.java)
                context.stopService(intent)
            } catch (e: Throwable) {
                Log.e("FloatingMimiService", "Failed to stop FloatingMimiService safely", e)
            }
        }
    }

    class OrbView(context: Context) : View(context) {
        private var status: AssistantStatus = AssistantStatus.IDLE
        private var pulsePhase = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val handlerRef = Handler(Looper.getMainLooper())

        private val animRunnable = object : Runnable {
            override fun run() {
                pulsePhase += 0.1f
                invalidate()
                handlerRef.postDelayed(this, 30)
            }
        }

        init {
            handlerRef.post(animRunnable)
        }

        fun setStatus(newStatus: AssistantStatus) {
            status = newStatus
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val baseRadius = width * 0.35f
            val pulse = (sin(pulsePhase.toDouble()) * 0.08f).toFloat()
            val r = baseRadius * (1f + pulse)

            // Outer shadow / glow halo
            val glowColor = when (status) {
                AssistantStatus.LISTENING -> Color.parseColor("#4400E5FF")
                AssistantStatus.THINKING -> Color.parseColor("#55A855F7")
                AssistantStatus.SPEAKING -> Color.parseColor("#55F43F5E")
                else -> Color.parseColor("#3300E5FF")
            }
            shadowPaint.color = glowColor
            canvas.drawCircle(cx, cy, r * 1.35f, shadowPaint)

            // Orbit Ring
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = when (status) {
                AssistantStatus.LISTENING -> Color.parseColor("#00E5FF")
                AssistantStatus.THINKING -> Color.parseColor("#A855F7")
                AssistantStatus.SPEAKING -> Color.parseColor("#F43F5E")
                else -> Color.parseColor("#67F6FF")
            }
            canvas.drawCircle(cx, cy, r * 1.15f, paint)

            // Orbiting particle
            val angle = (pulsePhase * 2) % (2 * Math.PI)
            val px = (cx + (r * 1.15f) * cos(angle)).toFloat()
            val py = (cy + (r * 1.15f) * sin(angle)).toFloat()
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawCircle(px, py, 6f, paint)

            // Core Orb Gradient
            val coreColor1 = when (status) {
                AssistantStatus.LISTENING -> Color.parseColor("#00E5FF")
                AssistantStatus.THINKING -> Color.parseColor("#A855F7")
                AssistantStatus.SPEAKING -> Color.parseColor("#F43F5E")
                else -> Color.parseColor("#00E5FF")
            }
            val coreColor2 = when (status) {
                AssistantStatus.LISTENING -> Color.parseColor("#A855F7")
                AssistantStatus.THINKING -> Color.parseColor("#F43F5E")
                AssistantStatus.SPEAKING -> Color.parseColor("#00E5FF")
                else -> Color.parseColor("#0D1424")
            }

            paint.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(Color.WHITE, coreColor1, coreColor2),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            handlerRef.removeCallbacks(animRunnable)
        }
    }
}
