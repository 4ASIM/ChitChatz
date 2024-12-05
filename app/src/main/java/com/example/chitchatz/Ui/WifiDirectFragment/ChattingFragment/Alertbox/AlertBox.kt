package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.Alertbox

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View

class AlertBox(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val rect: RectF = RectF()
    private val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
    private val drawPaint: Paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawPath(shapePath(), drawPaint)
    }

    private fun dp2px(dp: Int): Int {
        return (dp * displayMetrics.density + 0.5f).toInt()
    }

    private fun shapePath(): Path {
        return Path().apply {
            fillType = Path.FillType.WINDING
            addRoundRect(rect, 16f, 16f, Path.Direction.CW)
            addCircle(width / 2f, 0f, dp2px(50).toFloat(), Path.Direction.CCW)
        }
    }
}