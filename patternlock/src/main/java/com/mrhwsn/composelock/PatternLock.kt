package com.mrhwsn.composelock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

interface LockCallback {
    fun onStart(dot: Dot)
    fun onDotConnected(dot: Dot)
    fun onResult(result: List<Dot>)
}

@Composable
fun PatternLock(
    modifier: Modifier = Modifier,
    dimension: Int = 3,
    sensitivity: Float = 50f,
    dotsColor: Color = Color.Gray,
    dotsSize: Float = 20f,
    linesColor: Color = Color.Black,
    linesStroke: Float = 8f,
    animationDuration: Int = 200,
    animationDelay: Long = 100,
    callback: LockCallback
) {
    val scope = rememberCoroutineScope()
    val connectedDots = remember { mutableStateListOf<Dot>() }
    val currentDotState = remember { mutableStateOf<Dot?>(null) }
    val previewEnd = remember { mutableStateOf(Offset.Unspecified) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val dots = remember(dimension, dotsSize, canvasSize) {
        if (canvasSize == Size.Zero) emptyList() else buildList {
            val cellW = canvasSize.width / (dimension + 1)
            val cellH = canvasSize.height / (dimension + 1)
            for (c in 0 until dimension) {
                for (r in 0 until dimension) {
                    add(
                        Dot(
                            id = this.size + 1,
                            offset = Offset((c + 1) * cellW, (r + 1) * cellH),
                            size = Animatable(dotsSize)
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it.toSize() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var currentDot: Dot? = null
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.isEmpty()) continue
                        val change = event.changes.first()
                        val pos = change.position

                        if (change.pressed) {
                            val hitDot = dots.find { dot ->
                                (dot.offset - pos).getDistance() <= sensitivity
                            }

                            if (hitDot != null && !connectedDots.any { it.id == hitDot.id }) {
                                if (currentDot == null) {
                                    connectedDots.add(hitDot)
                                    callback.onStart(hitDot)
                                } else {
                                    val intermediates = findIntermediateDots(
                                        from = currentDot,
                                        to = hitDot,
                                        allDots = dots,
                                        selectedIds = connectedDots.map { it.id }.toSet()
                                    )
                                    intermediates.forEach { interDot ->
                                        connectedDots.add(interDot)
                                        callback.onDotConnected(interDot)
                                        animateDot(
                                            scope,
                                            interDot,
                                            dotsSize,
                                            animationDuration,
                                            animationDelay
                                        )
                                    }
                                    connectedDots.add(hitDot)
                                    callback.onDotConnected(hitDot)
                                }
                                animateDot(
                                    scope,
                                    hitDot,
                                    dotsSize,
                                    animationDuration,
                                    animationDelay
                                )
                                currentDot = hitDot
                            }
                            currentDotState.value = currentDot
                            previewEnd.value = pos
                        } else if (currentDot != null) {
                            callback.onResult(connectedDots.toList())
                            connectedDots.clear()
                            currentDotState.value = null
                            currentDot = null
                            previewEnd.value = Offset.Unspecified
                        }
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val connectedList = connectedDots.toList()

            // Draw established lines between consecutively selected dots
            for (i in 0 until connectedList.size - 1) {
                drawLine(
                    color = linesColor,
                    start = connectedList[i].offset,
                    end = connectedList[i + 1].offset,
                    strokeWidth = linesStroke,
                    cap = StrokeCap.Round
                )
            }

            // Draw active preview line
            val lastConnected = currentDotState.value
            if (lastConnected != null && previewEnd.value != Offset.Unspecified) {
                drawLine(
                    color = linesColor,
                    start = lastConnected.offset,
                    end = previewEnd.value,
                    strokeWidth = linesStroke,
                    cap = StrokeCap.Round
                )
            }

            // Draw all dots
            dots.forEach { dot ->
                drawCircle(
                    color = dotsColor,
                    radius = dot.size.value,
                    center = dot.offset
                )
            }
        }
    }
}

private fun animateDot(
    scope: CoroutineScope,
    dot: Dot,
    baseSize: Float,
    duration: Int,
    delayTime: Long
) {
    scope.launch {
        dot.size.animateTo(baseSize * 1.8f, tween(duration))
        delay(delayTime.milliseconds)
        dot.size.animateTo(baseSize, tween(duration))
    }
}

private fun findIntermediateDots(
    from: Dot,
    to: Dot,
    allDots: List<Dot>,
    selectedIds: Set<Int>
): List<Dot> {
    val intermediates = mutableListOf<Dot>()
    val dx = to.offset.x - from.offset.x
    val dy = to.offset.y - from.offset.y

    for (candidate in allDots) {
        if (candidate.id in selectedIds || candidate.id == from.id || candidate.id == to.id) continue

        // Cross product for collinearity check
        val cross =
            (candidate.offset.x - from.offset.x) * dy - dx * (candidate.offset.y - from.offset.y)
        if (abs(cross) > 1.0f) continue

        // Bounding box to ensure candidate is between from and to
        val inX =
            candidate.offset.x in min(from.offset.x, to.offset.x)..max(from.offset.x, to.offset.x)
        val inY =
            candidate.offset.y in min(from.offset.y, to.offset.y)..max(from.offset.y, to.offset.y)

        if (inX && inY) {
            intermediates.add(candidate)
        }
    }
    return intermediates.sortedBy { (it.offset - from.offset).getDistance() }
}

@Preview(showBackground = true)
@Composable
fun PatternLockPreview() {
    PatternLock(
        modifier = Modifier.size(300.dp),
        dimension = 3,
        sensitivity = 50f,
        dotsColor = Color.LightGray,
        dotsSize = 20f,
        linesColor = Color.Black,
        linesStroke = 8f,
        callback = object: LockCallback {
            override fun onStart(dot: Dot) {}
            override fun onDotConnected(dot: Dot) {}
            override fun onResult(result: List<Dot>) {}
        }
    )
}
