package com.baris.tvappswitcher

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * A custom FrameLayout that renders a fluid translucent dock tray.
 *
 * Implements a multi-layer optical rendering pipeline:
 *  1. Multi-pass ambient contact & drop shadows (floating elevation)
 *  2. Translucent obsidian/slate gradient glass base (transparency configurable)
 *  3. Specular curved gloss sheen across top of the tray
 *  4. Subtle bottom shelf reflection catch
 *  5. Barely visible, hair-thin specular rim highlight (single hairline stroke)
 *  6. Razor-sharp top edge micro-highlight line
 *  7. Safe vertical child clipping (allows upward magnification while containing horizontal scrolling)
 */
class LiquidDockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var cornerRadius = 32f * density

    private val dockRect = RectF()
    private val shadowOuterRect = RectF()
    private val shadowMidRect = RectF()
    private val shadowNearRect = RectF()
    private val sheenRect = RectF()
    private val bottomCatchRect = RectF()
    private val rimRect = RectF()
    private val childRect = RectF()

    private val clipPath = Path()
    private val childClipPath = Path()

    // Paints
    private val shadowOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val shadowMidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val shadowNearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bottomCatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Barely visible, hair-thin rim stroke (hairline ~0.5dp / 1px)
    private val outerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = maxOf(1f, 0.45f * density)
    }

    private val topMicroHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = maxOf(1f, 0.6f * density)
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sheenPaint.blendMode = BlendMode.SCREEN
            bottomCatchPaint.blendMode = BlendMode.SCREEN
            outerRimPaint.blendMode = BlendMode.OVERLAY
            topMicroHighlightPaint.blendMode = BlendMode.SCREEN
        } else {
            val screenXfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            sheenPaint.xfermode = screenXfermode
            bottomCatchPaint.xfermode = screenXfermode
            topMicroHighlightPaint.xfermode = screenXfermode
        }
    }

    fun refreshCustomization() {
        if (width > 0 && height > 0) {
            updateGeometryAndShaders(width, height)
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        updateGeometryAndShaders(w, h)
    }

    private fun updateGeometryAndShaders(w: Int, h: Int) {
        val fw = w.toFloat()
        val fh = h.toFloat()

        val scaleFactor = ConfigManager.getDockScalePercent(context) / 100f
        val transPercent = ConfigManager.getDockTransparencyPercent(context)
        val opacity = (1f - (transPercent / 100f)).coerceIn(0.06f, 1.0f)

        cornerRadius = 32f * density * scaleFactor

        dockRect.set(0f, 0f, fw, fh)

        // Floating ambient drop shadows (scaled by opacity)
        val shadowAlphaOuter = (0x1C * opacity).toInt().coerceIn(0, 255)
        val shadowAlphaMid = (0x35 * opacity).toInt().coerceIn(0, 255)
        val shadowAlphaNear = (0x50 * opacity).toInt().coerceIn(0, 255)

        shadowOuterPaint.color = Color.argb(shadowAlphaOuter, 0, 0, 0)
        shadowMidPaint.color = Color.argb(shadowAlphaMid, 0, 0, 0)
        shadowNearPaint.color = Color.argb(shadowAlphaNear, 0, 0, 0)

        shadowOuterRect.set(-4f * density, 4f * density, fw + 4f * density, fh + 10f * density)
        shadowMidRect.set(-2f * density, 2.5f * density, fw + 2f * density, fh + 6f * density)
        shadowNearRect.set(0f, 1.5f * density, fw, fh + 3f * density)

        // 1. Translucent obsidian glass base gradient (scaled by user transparency)
        val topAlpha = (0x80 * opacity).toInt().coerceIn(0x0a, 255)
        val midAlpha = (0x95 * opacity).toInt().coerceIn(0x10, 255)
        val botAlpha = (0xAA * opacity).toInt().coerceIn(0x18, 255)

        bodyPaint.shader = LinearGradient(
            0f, 0f, 0f, fh,
            intArrayOf(
                Color.argb(topAlpha, 0x1E, 0x22, 0x33), // Top: Translucent twilight indigo
                Color.argb(midAlpha, 0x14, 0x17, 0x24), // Mid: Deep charcoal
                Color.argb(botAlpha, 0x0A, 0x0C, 0x16)  // Bottom: Dark obsidian
            ),
            floatArrayOf(0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )

        // 2. Specular top gloss sheen (curved glass surface reflection)
        val sheenHeight = fh * 0.45f
        sheenRect.set(0f, 0f, fw, sheenHeight)
        val sheenTopAlpha = (0x48 * (0.4f + 0.6f * opacity)).toInt().coerceIn(0, 255)
        val sheenMidAlpha = (0x16 * (0.4f + 0.6f * opacity)).toInt().coerceIn(0, 255)
        sheenPaint.shader = LinearGradient(
            0f, 0f, 0f, sheenHeight,
            intArrayOf(
                Color.argb(sheenTopAlpha, 0xFF, 0xFF, 0xFF),
                Color.argb(sheenMidAlpha, 0xFF, 0xFF, 0xFF),
                Color.argb(0x00, 0xFF, 0xFF, 0xFF)
            ),
            floatArrayOf(0f, 0.40f, 1.0f),
            Shader.TileMode.CLAMP
        )

        // 3. Bottom shelf reflection catch
        val bottomCatchHeight = 12f * density * scaleFactor
        bottomCatchRect.set(0f, fh - bottomCatchHeight, fw, fh)
        bottomCatchPaint.shader = LinearGradient(
            0f, fh - bottomCatchHeight, 0f, fh,
            intArrayOf(
                Color.argb(0x00, 0xFF, 0xFF, 0xFF),
                Color.argb((0x1C * opacity).toInt().coerceIn(0, 255), 0xFF, 0xFF, 0xFF)
            ),
            null,
            Shader.TileMode.CLAMP
        )

        // 4. Barely visible, hair-thin specular rim highlight (single hairline stroke)
        outerRimPaint.strokeWidth = maxOf(1f, 0.45f * density)
        val halfRim = outerRimPaint.strokeWidth / 2f
        rimRect.set(halfRim, halfRim, fw - halfRim, fh - halfRim)
        outerRimPaint.shader = LinearGradient(
            0f, 0f, fw, fh,
            intArrayOf(
                Color.argb(0x65, 0xFF, 0xFF, 0xFF), // Gentle specular corner glint
                Color.argb(0x22, 0xFF, 0xFF, 0xFF), // Hairline top edge
                Color.argb(0x0E, 0xFF, 0xFF, 0xFF), // Barely visible ambient side
                Color.argb(0x18, 0xFF, 0xFF, 0xFF), // Subtle bottom curve
                Color.argb(0x30, 0xFF, 0xFF, 0xFF)  // Soft bottom corner catch
            ),
            floatArrayOf(0f, 0.25f, 0.50f, 0.75f, 1.0f),
            Shader.TileMode.CLAMP
        )

        // 5. Razor-sharp top edge micro-highlight line (hairline glint)
        topMicroHighlightPaint.strokeWidth = maxOf(1f, 0.6f * density)
        val hlStart = cornerRadius * 0.7f
        val hlEnd = fw - hlStart
        topMicroHighlightPaint.shader = LinearGradient(
            hlStart, 0f, hlEnd, 0f,
            intArrayOf(
                Color.argb(0x00, 0xFF, 0xFF, 0xFF),
                Color.argb(0x60, 0xFF, 0xFF, 0xFF),
                Color.argb(0x00, 0xFF, 0xFF, 0xFF)
            ),
            floatArrayOf(0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )

        clipPath.reset()
        clipPath.addRoundRect(dockRect, cornerRadius, cornerRadius, Path.Direction.CW)

        // Child clip path: extends 90dp upward so magnified/lifted icons never clip at top,
        // but preserves bottom and side curvature so scrolling icons never bleed past left/right ends.
        val topExtension = 90f * density * scaleFactor
        childRect.set(0f, -topExtension, fw, fh)
        val radii = floatArrayOf(
            cornerRadius, cornerRadius, // top-left
            cornerRadius, cornerRadius, // top-right
            cornerRadius, cornerRadius, // bottom-right
            cornerRadius, cornerRadius  // bottom-left
        )
        childClipPath.reset()
        childClipPath.addRoundRect(childRect, radii, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        val fw = width.toFloat()
        val fh = height.toFloat()
        if (fw <= 0f || fh <= 0f) return

        // 1. Draw multi-pass ambient drop shadows
        canvas.drawRoundRect(shadowOuterRect, cornerRadius + 4f * density, cornerRadius + 4f * density, shadowOuterPaint)
        canvas.drawRoundRect(shadowMidRect, cornerRadius + 2f * density, cornerRadius + 2f * density, shadowMidPaint)
        canvas.drawRoundRect(shadowNearRect, cornerRadius, cornerRadius, shadowNearPaint)

        // 2. Draw translucent glass body
        canvas.drawRoundRect(dockRect, cornerRadius, cornerRadius, bodyPaint)

        // 3. Draw specular top sheen and bottom shelf reflection inside the rounded glass bounds
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawRect(sheenRect, sheenPaint)
        canvas.drawRect(bottomCatchRect, bottomCatchPaint)
        canvas.restore()

        // 4. Draw hair-thin, barely visible specular rim highlight (behind children)
        val halfRim = outerRimPaint.strokeWidth / 2f
        val rimRadius = (cornerRadius - halfRim).coerceAtLeast(0f)
        canvas.drawRoundRect(rimRect, rimRadius, rimRadius, outerRimPaint)

        // 5. Draw hair-thin overhead micro-highlight glint line at the very top edge (behind children)
        val hlStart = cornerRadius * 0.7f
        val hlEnd = fw - hlStart
        canvas.drawLine(
            hlStart,
            halfRim,
            hlEnd,
            halfRim,
            topMicroHighlightPaint
        )

        super.onDraw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val fw = width.toFloat()
        val fh = height.toFloat()
        if (fw <= 0f || fh <= 0f) {
            super.dispatchDraw(canvas)
            return
        }

        // Clip children safely within horizontal bounds while allowing upward magnification
        canvas.save()
        canvas.clipPath(childClipPath)
        super.dispatchDraw(canvas)
        canvas.restore()
    }
}
