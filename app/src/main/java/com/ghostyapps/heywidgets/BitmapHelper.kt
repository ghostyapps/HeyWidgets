package com.ghostyapps.heywidgets

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat

object BitmapHelper {

    fun textToBitmap(
        context: Context,
        text: String,
        textSizeDp: Float,
        fontResId: Int,
        hexColor: String,
        withShadow: Boolean = false,
        alignment: Paint.Align = Paint.Align.LEFT
    ): Bitmap? {
        val scale = context.resources.displayMetrics.density
        val textSizePx = textSizeDp * scale

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor(hexColor)
        paint.textSize = textSizePx
        paint.textAlign = alignment

        if (withShadow) {
            paint.setShadowLayer(12f, 0f, 0f, Color.parseColor("#80000000"))
        }

        paint.typeface = try {
            ResourcesCompat.getFont(context, fontResId) ?: Typeface.DEFAULT_BOLD
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        // --- DÜZELTME BAŞLANGICI ---
        // Fontun tüm metriklerini (üst ve alt kuyruk payları dahil) alıyoruz
        val fontMetrics = paint.fontMetrics

        // Genişliği biraz daha rahat bırakıyoruz (aslında +4 veya +6 px daha güvenli)
        val width = (bounds.width() + 8).coerceAtLeast(1)

        // Yüksekliği bounds yerine fontun gerçek yüksekliğine (ascent + descent) göre belirliyoruz
        val height = (fontMetrics.bottom - fontMetrics.top).toInt().coerceAtLeast(1)

        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)

        val xPos = when (alignment) {
            Paint.Align.CENTER -> (canvas.width / 2).toFloat()
            Paint.Align.LEFT -> 4f // Kenardan 4px pay
            Paint.Align.RIGHT -> (canvas.width - 4).toFloat()
        }

        // Metni dikeyde ortalarken gerçek font metriklerini kullanıyoruz
        val yPos = (canvas.height / 2) - ((fontMetrics.descent + fontMetrics.ascent) / 2)
        // --- DÜZELTME BİTİŞİ ---

        canvas.drawText(text, xPos, yPos, paint)
        return image
    }
}