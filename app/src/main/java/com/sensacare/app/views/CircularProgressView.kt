package com.sensacare.app.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.sensacare.app.R

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var progress = 0f
    private var maxProgress = 100f
    private var strokeWidth = 20f
    private var progressColor = ContextCompat.getColor(context, R.color.incrediness_orange)
    private var backgroundColor = ContextCompat.getColor(context, R.color.divider)
    private var textColor = ContextCompat.getColor(context, R.color.text_primary)
    private var title = ""
    private var subtitle = ""
    
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    
    init {
        setupPaints()
    }
    
    private fun setupPaints() {
        // Progress paint
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.ROUND
        
        // Background paint
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = strokeWidth
        backgroundPaint.strokeCap = Paint.Cap.ROUND
        backgroundPaint.color = backgroundColor
        
        // Text paint
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = textColor
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = (minOf(w, h) / 2f) - strokeWidth
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        
        // Draw progress arc
        paint.color = progressColor
        val sweepAngle = (progress / maxProgress) * 360f
        val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawArc(rect, -90f, sweepAngle, false, paint)
        
        // Draw text
        drawText(canvas)
    }
    
    private fun drawText(canvas: Canvas) {
        val textY = centerY + (textPaint.textSize / 3)
        
        // Draw title
        if (title.isNotEmpty()) {
            textPaint.textSize = 16f * resources.displayMetrics.density
            canvas.drawText(title, centerX, centerY - 20f, textPaint)
        }
        
        // Draw percentage
        textPaint.textSize = 32f * resources.displayMetrics.density
        textPaint.isFakeBoldText = true
        val percentage = ((progress / maxProgress) * 100).toInt()
        canvas.drawText("$percentage%", centerX, textY, textPaint)
        
        // Draw subtitle
        if (subtitle.isNotEmpty()) {
            textPaint.textSize = 14f * resources.displayMetrics.density
            textPaint.isFakeBoldText = false
            canvas.drawText(subtitle, centerX, textY + 30f, textPaint)
        }
    }
    
    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, maxProgress)
        invalidate()
    }
    
    fun setMaxProgress(maxProgress: Float) {
        this.maxProgress = maxProgress
        invalidate()
    }
    
    fun setProgressColor(color: Int) {
        this.progressColor = color
        invalidate()
    }
    
    fun setTitle(title: String) {
        this.title = title
        invalidate()
    }
    
    fun setSubtitle(subtitle: String) {
        this.subtitle = subtitle
        invalidate()
    }
    
    fun setStrokeWidth(width: Float) {
        this.strokeWidth = width
        paint.strokeWidth = width
        backgroundPaint.strokeWidth = width
        invalidate()
    }
} 