package com.example.sweepneko

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite

@Composable
fun HpStaminaBar(
    hp: Int,
    stamina: Float,
    ultimateGauge: Float,
    comboCount: Int,
    isNextSlashRed: Boolean,
    wave: Int = 1,
    enemiesKilled: Int = 0,
    targetKills: Int = 15,
    isInfiniteSP: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // HP Bar Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                CustomBar(
                    progress = hp / 100f,
                    barColor = Color(0xFFE53935),
                    bgColor = Color(0xFF424242),
                    borderColor = Color(0xFFEEEEEE),
                    isHexagon = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(start = 28.dp)
                )
                
                // Heart Icon background for slight shadow effect
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "HP Shadow",
                    tint = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(48.dp)
                        .offset(x = (-2).dp, y = 2.dp)
                )
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "HP",
                    tint = Color(0xFFFF4842),
                    modifier = Modifier
                        .size(48.dp)
                        .offset(x = (-4).dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // SP Bar Row (Stamina) + Wave Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(0.6f)
            ) {
                CustomBar(
                    progress = stamina / 100f,
                    barColor = Color(0xFFFFCA28), // Electric Yellow
                    bgColor = Color(0xFF424242),
                    borderColor = Color(0xFFEEEEEE),
                    isHexagon = true,
                    isRainbow = isInfiniteSP,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(start = 28.dp)
                )
                
                LightningIcon(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = (-6).dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Wave Info next to SP bar
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(0.4f)
            ) {
                Text(
                    text = "Wave $wave",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF887164)
                )
                Text(
                    text = "Kills: $enemiesKilled / $targetKills",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF887164).copy(alpha = 0.8f)
                )
            }
        }
        
        if (comboCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            val comboColor = if (isNextSlashRed) Color(0xFFE07A7A) else Color(0xFFFF9800)
            
            // Animation for bouncing effect when combo increases
            val scale = remember { Animatable(1f) }
            LaunchedEffect(comboCount) {
                scale.animateTo(
                    targetValue = 1.4f,
                    animationSpec = tween(durationMillis = 50, easing = FastOutSlowInEasing)
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            Text(
                text = "Hit: $comboCount",
                fontSize = 28.sp, 
                fontWeight = FontWeight.Black, 
                color = comboColor,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )
        }
    }
}

@Composable
fun LightningIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Shadow for depth
        val shadowPath = Path().apply {
            moveTo(w * 0.65f + 3f, 2f)
            lineTo(w * 0.25f + 3f, h * 0.55f + 2f)
            lineTo(w * 0.5f + 3f, h * 0.55f + 2f)
            lineTo(w * 0.3f + 3f, h + 2f)
            lineTo(w * 0.85f + 3f, h * 0.4f + 2f)
            lineTo(w * 0.55f + 3f, h * 0.4f + 2f)
            close()
        }
        drawPath(shadowPath, color = Color.Black.copy(alpha = 0.3f))
        
        // Base Bolt (Vibrant Yellow)
        val boltPath = Path().apply {
            moveTo(w * 0.65f, 0f)
            lineTo(w * 0.25f, h * 0.55f)
            lineTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.3f, h)
            lineTo(w * 0.85f, h * 0.4f)
            lineTo(w * 0.55f, h * 0.4f)
            close()
        }
        drawPath(boltPath, color = Color(0xFFFFEB3B)) // Bright Yellow
        
        // Inner Shader for 3D effect
        val innerPath = Path().apply {
            moveTo(w * 0.65f, h * 0.05f)
            lineTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.4f, h * 0.8f)
            lineTo(w * 0.75f, h * 0.45f)
            lineTo(w * 0.55f, h * 0.45f)
            close()
        }
        drawPath(innerPath, color = Color(0xFFFFF59D)) // Light Yellow
        
        // Border outline
        drawPath(
            boltPath, 
            color = Color(0xFFF57F17), // Orange/Gold border
            style = Stroke(width = 3f, join = androidx.compose.ui.graphics.StrokeJoin.Miter)
        )
    }
}

@Composable
fun CustomBar(
    progress: Float,
    barColor: Color,
    bgColor: Color,
    borderColor: Color,
    isHexagon: Boolean,
    isRainbow: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowOffset"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = if (isHexagon) h / 2f else h / 1.5f
        
        val bgPath = Path().apply {
            if (isHexagon) {
                moveTo(s, 0f)
                lineTo(w - s, 0f)
                lineTo(w, h / 2f)
                lineTo(w - s, h)
                lineTo(s, h)
                lineTo(0f, h / 2f)
                close()
            } else {
                moveTo(0f, 0f)
                lineTo(w - s, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
        }
        
        drawPath(path = bgPath, color = bgColor)
        
        val minWidth = if (isHexagon) s * 2 else s
        val safeProgress = progress.coerceIn(0f, 1f)
        val currentW = minWidth + (w - minWidth) * safeProgress
        
        if (safeProgress > 0f) {
            val fgPath = Path().apply {
                if (isHexagon) {
                    moveTo(s, 0f)
                    lineTo(currentW - s, 0f)
                    lineTo(currentW, h / 2f)
                    lineTo(currentW - s, h)
                    lineTo(s, h)
                    lineTo(0f, h / 2f)
                    close()
                } else {
                    moveTo(0f, 0f)
                    lineTo(currentW - s, 0f)
                    lineTo(currentW, h)
                    lineTo(0f, h)
                    close()
                }
            }
            if (isRainbow) {
                val colors = listOf(
                    Color.Red, Color.Magenta, Color.Blue, Color.Cyan, 
                    Color.Green, Color.Yellow, Color.Red
                )
                
                drawPath(
                    path = fgPath,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = colors,
                        start = Offset(offset, 0f),
                        end = Offset(offset + 500f, 0f),
                        tileMode = androidx.compose.ui.graphics.TileMode.Repeated
                    )
                )
            } else {
                drawPath(path = fgPath, color = barColor)
            }
        }
        
        drawPath(
            path = bgPath,
            color = borderColor,
            style = Stroke(width = 6f, join = androidx.compose.ui.graphics.StrokeJoin.Bevel)
        )
    }
}

@Composable
fun GameOverMenu(onRestart: () -> Unit, onMenu: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "GAME OVER", color = Color.Red, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier.width(200.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB676))
            ) {
                Text("Restart", fontSize = 20.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onMenu,
                modifier = Modifier.width(200.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB676))
            ) {
                Text("Menu", fontSize = 20.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun PauseMenu(onResume: () -> Unit, onMenu: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "PAUSED", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onResume,
                modifier = Modifier.width(200.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB676))
            ) {
                Text("Resume", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onMenu,
                modifier = Modifier.width(200.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB676))
            ) {
                Text("Menu", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun InventoryRow(
    inventory: List<PowerUpType>,
    onUse: (PowerUpType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        inventory.distinct().forEach { type ->
            val count = inventory.count { it == type }
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    onClick = { onUse(type) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(48.dp),
                    shadowElevation = 4.dp
                ) {
                    val resId = when(type) {
                        PowerUpType.CAT_CAN -> R.drawable.catcan
                        PowerUpType.CAT_BAR -> R.drawable.carbar
                        PowerUpType.TIME_STOP -> R.drawable.time
                    }
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = resId),
                        contentDescription = type.name,
                        modifier = Modifier.padding(6.dp).fillMaxSize()
                    )
                }
                if (count > 1) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.Red,
                        modifier = Modifier.size(20.dp).offset(x = 6.dp, y = (-6).dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = count.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
