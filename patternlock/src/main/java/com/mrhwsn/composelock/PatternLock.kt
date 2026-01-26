package com.mrhwsn.composelock

import android.util.Range
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface LockCallback {
    fun onStart(dot: Dot)
    fun onDotConnected(dot: Dot)
    fun onResult(result: List<Dot>)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PatternLock(
    modifier: Modifier,
    dimension: Int,
    sensitivity: Float,
    dotsColor: Color,
    dotsSize: Float,
    linesColor: Color,
    linesStroke: Float,
    animationDuration: Int = 200,
    animationDelay: Long = 100,
    callback: LockCallback
) {
    val scope = rememberCoroutineScope()
    val dotsList = remember {
        mutableListOf<Dot>()
    }
    val previewLine = remember {
        mutableStateOf<Line?>(null)
    }
    val connectedLines = remember {
        mutableStateListOf<Line>()
    }
    val connectedDots = remember {
        mutableStateListOf<Dot>()
    }

    Canvas(
        modifier.pointerInteropFilter {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    for (dots in dotsList) {
                        if (
                            it.x in Range(
                                dots.offset.x - sensitivity,
                                dots.offset.x + sensitivity
                            ) &&
                            it.y in Range(dots.offset.y - sensitivity, dots.offset.y + sensitivity)
                        ) {
                            connectedDots.add(dots)
                            callback.onStart(dots)
                            scope.launch {
                                dots.size.animateTo(
                                    (dotsSize * 1.8).toFloat(),
                                    tween(animationDuration)
                                )
                                delay(animationDelay)
                                dots.size.animateTo(dotsSize, tween(animationDuration))
                            }
                            previewLine.value = Line(start = dots.offset, end = dots.offset)
                        }
                    }
                }


                MotionEvent.ACTION_MOVE -> {
                    if (previewLine.value != null) {
                        previewLine.value = previewLine.value!!.copy(end = Offset(it.x, it.y))
                    }
                    for (dots in dotsList) {
                        if (!connectedDots.contains(dots)) {
                            if (
                                it.x in Range(
                                    dots.offset.x - sensitivity,
                                    dots.offset.x + sensitivity
                                ) &&
                                it.y in Range(
                                    dots.offset.y - sensitivity,
                                    dots.offset.y + sensitivity
                                )
                            ) {
                                if (previewLine.value != null)
                                    connectedLines.add(
                                        Line(
                                            start = previewLine.value!!.start,
                                            end = dots.offset
                                        )
                                    )
                                connectedDots.add(dots)
                                callback.onDotConnected(dots)
                                scope.launch {
                                    dots.size.animateTo(
                                        (dotsSize * 1.8).toFloat(),
                                    )
                                    delay(animationDelay)
                                    dots.size.animateTo(dotsSize, tween(animationDuration))
                                }
                                if (previewLine.value != null)
                                    previewLine.value =
                                        previewLine.value!!.copy(start = dots.offset)
                            }
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    previewLine.value = null
                    callback.onResult(connectedDots.toList())
                    connectedLines.clear()
                    connectedDots.clear()
                }
            }
            true
        }) {
        val realDimension = dimension + 1
        val spaceBetweenWidthDots = size.width / realDimension
        val spaceBetweenHeightDots = size.height / realDimension
        val dotsOnWidth = arrayOfNulls<Int>(realDimension * realDimension)
        val dotsOnHeight = arrayOfNulls<Int>(realDimension * realDimension)
        dotsOnWidth.forEachIndexed { widthIndex, _ ->
            val readWidthIndex = widthIndex + 1
            dotsOnHeight.forEachIndexed { heightIndex, _ ->
                val readHeightIndex = heightIndex + 1
                if (readWidthIndex < realDimension && readHeightIndex < realDimension) {
                    if (dotsList.count() < dimension * dimension) {
                        val dotOffset = Offset(
                            (spaceBetweenWidthDots * readWidthIndex),
                            (spaceBetweenHeightDots * readHeightIndex)
                        )
                        dotsList.add(
                            Dot(
                                dotsList.size + 1,
                                dotOffset,
                                Animatable(dotsSize)
                            )
                        )
                    }
                }
            }
        }
        if (previewLine.value != null) {
            drawLine(
                color = linesColor,
                start = previewLine.value!!.start,
                end = previewLine.value!!.end,
                strokeWidth = linesStroke,
                cap = StrokeCap.Round
            )
        }
        for (dots in dotsList) {
            drawCircle(
                color = dotsColor,
                radius = dots.size.value,
                center = dots.offset
            )
        }
        for (line in connectedLines) {
            drawLine(
                color = linesColor,
                start = line.start,
                end = line.end,
                strokeWidth = linesStroke,
                cap = StrokeCap.Round
            )
        }

    }
}

@Preview
@Composable
fun PatternLockPreview() {
    PatternLock(
        Modifier,
        4,
        100f,
        Color.Black,
        20f,
        Color.Black,
        30f,
        200,
        100,
        object : LockCallback {
            override fun onStart(dot: Dot) {}
            override fun onDotConnected(dot: Dot) {}
            override fun onResult(result: List<Dot>) {}
        }
    )
}
