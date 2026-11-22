package com.dayflow.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen() {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Quiet oversized corner shapes give the screen depth without competing
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = colors.primary.copy(alpha = 0.045f),
                radius = size.width * 0.62f,
                center = Offset(size.width * 1.08f, size.height * 0.08f)
            )
            drawCircle(
                color = colors.primary.copy(alpha = 0.035f),
                radius = size.width * 0.42f,
                center = Offset(-size.width * 0.18f, size.height * 0.88f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "D",
                        color = colors.onPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(11.dp))
                Text(
                    "DAYFLOW",
                    color = colors.onBackground,
                    fontSize = 17.sp,
                    letterSpacing = 2.6.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "YOUR DAY, IN FLOW",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Box(
                    modifier = Modifier.size(238.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outer = size.minDimension * 0.47f
                        drawCircle(
                            color = colors.primary.copy(alpha = 0.07f),
                            radius = outer,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = colors.primary.copy(alpha = 0.13f),
                            radius = outer * 0.82f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawArc(
                            color = colors.primary,
                            startAngle = -90f,
                            sweepAngle = 252f,
                            useCenter = false,
                            topLeft = Offset(center.x - outer * 0.91f, center.y - outer * 0.91f),
                            size = androidx.compose.ui.geometry.Size(outer * 1.82f, outer * 1.82f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawCircle(
                            color = colors.primary,
                            radius = 4.dp.toPx(),
                            center = Offset(center.x + outer * 0.91f, center.y)
                        )
                        drawCircle(
                            color = colors.primary.copy(alpha = 0.45f),
                            radius = 2.5.dp.toPx(),
                            center = Offset(center.x - outer * 0.82f, center.y - outer * 0.35f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .background(colors.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "24",
                                color = colors.primary,
                                fontSize = 58.sp,
                                lineHeight = 62.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-3).sp
                            )
                            Text(
                                "HOURS",
                                color = colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(30.dp))
                Text(
                    "Make room for\nwhat matters.",
                    color = colors.onBackground,
                    fontSize = 30.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.7).sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Plan your day. Find your rhythm.",
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(3.dp)
                        .background(colors.primary.copy(alpha = 0.16f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.58f)
                            .fillMaxHeight()
                            .background(colors.primary, CircleShape)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "GETTING THINGS READY",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
