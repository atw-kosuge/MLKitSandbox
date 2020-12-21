/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mozukudetector.view.layer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.mozukudetector.view.GraphicOverlayView
import com.example.mozukudetector.view.GraphicOverlayView.Graphic

class InfoTextLayer(
    private val overlay: GraphicOverlayView,
    private val latency: Double,
    private val framesPerSecond: Int?
) : Graphic(overlay) {

    private val textPaint: Paint

    init {
        textPaint = Paint()
        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE
        postInvalidate()
    }

    @Synchronized
    override fun draw(canvas: Canvas) {
        val x = TEXT_SIZE * 0.5f
        val y = TEXT_SIZE * 1.5f
        canvas.drawText(
            "InputImage size: " + overlay.imageWidth + "x" + overlay.imageHeight,
            x,
            y,
            textPaint
        )

        // Draw FPS (if valid) and inference latency
        if (framesPerSecond != null) {
            canvas.drawText(
                "FPS: $framesPerSecond, latency: $latency ms", x, y + TEXT_SIZE, textPaint
            )
        } else {
            canvas.drawText("Latency: $latency ms", x, y + TEXT_SIZE, textPaint)
        }
    }

    companion object {
        private const val TEXT_COLOR = Color.WHITE
        private const val TEXT_SIZE = 60.0f
    }
}