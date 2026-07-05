package com.detoxapp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Minimal dependency-free bar chart: draws one bar per value in [values],
 * scaled to the tallest bar. Used to show "blocks per day, last 7 days".
 */
class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Int> = emptyList()
    private var labels: List<String> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4C6EF5")
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222222")
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    fun setData(values: List<Int>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val maxVal = (values.maxOrNull() ?: 0).coerceAtLeast(1)
        val chartBottom = height - 60f
        val chartTop = 30f
        val chartHeight = chartBottom - chartTop
        val slotWidth = width.toFloat() / values.size
        val barWidth = slotWidth * 0.5f

        values.forEachIndexed { index, value ->
            val centerX = slotWidth * index + slotWidth / 2f
            val barHeight = (value.toFloat() / maxVal) * chartHeight
            val top = chartBottom - barHeight
            canvas.drawRoundRect(
                centerX - barWidth / 2f, top,
                centerX + barWidth / 2f, chartBottom,
                8f, 8f, barPaint
            )
            if (value > 0) {
                canvas.drawText(value.toString(), centerX, top - 10f, valuePaint)
            }
            if (index < labels.size) {
                canvas.drawText(labels[index], centerX, height.toFloat() - 10f, labelPaint)
            }
        }
    }
}
