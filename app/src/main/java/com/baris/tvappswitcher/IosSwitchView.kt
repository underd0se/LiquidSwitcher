package com.baris.tvappswitcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Checkable

/**
 * An authentic iOS-style toggle switch designed for Android TV D-pad remote navigation.
 *
 * Features:
 * - Proportional pill geometry with smooth sliding thumb and drop shadow
 * - Authentic iOS palette (Active Green #34C759, Neutral Off #38394A, Thumb White #FFFFFF)
 * - TV Focus feedback: Cyan focus ring + subtle scale elevation
 * - Hardware key navigation (DPAD_CENTER, ENTER) and Checkable contract
 */
class IosSwitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Checkable {

    private val density = resources.displayMetrics.density

    private val trackRect = RectF()
    private val focusRingRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        setShadowLayer(2.5f * density, 0f, 1.5f * density, 0x4D000000)
    }
    private val focusRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = Color.parseColor("#00E5FF")
    }

    private val offTrackColor = Color.parseColor("#38394A")
    private val onTrackColor = Color.parseColor("#34C759")

    private var isCheckedState = false
    private var progress = 0f
    private var animator: ValueAnimator? = null

    private var onCheckedChangeListener: ((IosSwitchView, Boolean) -> Unit)? = null

    init {
        isFocusable = true
        isClickable = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .translationZ(8f * density)
                    .setDuration(140L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .translationZ(0f)
                    .setDuration(140L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            invalidate()
        }

        setOnClickListener {
            toggle()
        }
    }

    fun setOnCheckedChangeListener(listener: (IosSwitchView, Boolean) -> Unit) {
        onCheckedChangeListener = listener
    }

    override fun isChecked(): Boolean = isCheckedState

    override fun setChecked(checked: Boolean) {
        setChecked(checked, animate = isAttachedToWindow)
    }

    fun setChecked(checked: Boolean, animate: Boolean) {
        if (this.isCheckedState == checked) return
        this.isCheckedState = checked

        animator?.cancel()
        if (animate) {
            val target = if (checked) 1f else 0f
            animator = ValueAnimator.ofFloat(progress, target).apply {
                duration = 180L
                interpolator = DecelerateInterpolator()
                addUpdateListener { va ->
                    progress = va.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            progress = if (checked) 1f else 0f
            invalidate()
        }
        onCheckedChangeListener?.invoke(this, checked)
    }

    override fun toggle() {
        playSoundEffect(SoundEffectConstants.CLICK)
        setChecked(!isCheckedState, animate = true)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            toggle()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defW = (54f * density).toInt()
        val defH = (32f * density).toInt()
        val w = resolveSize(defW, widthMeasureSpec)
        val h = resolveSize(defH, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val padding = 2f * density
        trackRect.set(padding, padding, w - padding, h - padding)
        val trackRadius = trackRect.height() / 2f

        // Color interpolation
        val r = (Color.red(offTrackColor) + (Color.red(onTrackColor) - Color.red(offTrackColor)) * progress).toInt()
        val g = (Color.green(offTrackColor) + (Color.green(onTrackColor) - Color.green(offTrackColor)) * progress).toInt()
        val b = (Color.blue(offTrackColor) + (Color.blue(onTrackColor) - Color.blue(offTrackColor)) * progress).toInt()
        trackPaint.color = Color.rgb(r, g, b)

        canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)

        // Draw TV focus ring when focused
        if (isFocused) {
            val ringPad = 1f * density
            focusRingRect.set(
                trackRect.left - ringPad,
                trackRect.top - ringPad,
                trackRect.right + ringPad,
                trackRect.bottom + ringPad
            )
            canvas.drawRoundRect(focusRingRect, trackRadius + ringPad, trackRadius + ringPad, focusRingPaint)
        }

        // Draw smooth white thumb
        val thumbRadius = (trackRect.height() - 4f * density) / 2f
        val minThumbX = trackRect.left + 2f * density + thumbRadius
        val maxThumbX = trackRect.right - 2f * density - thumbRadius
        val thumbX = minThumbX + (maxThumbX - minThumbX) * progress
        val thumbY = trackRect.centerY()

        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint)
    }
}
