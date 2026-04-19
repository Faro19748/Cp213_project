package com.example.sweepneko

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun HpStaminaBar(hp: Int, stamina: Float, ultimateGauge: Float, comboCount: Int, isNextSlashRed: Boolean) {
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // SP Bar Row (Stamina)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                CustomBar(
                    progress = stamina / 100f,
                    barColor = Color(0xFFFFCA28), // Electric Yellow
                    bgColor = Color(0xFF424242),
                    borderColor = Color(0xFFEEEEEE),
                    isHexagon = true,
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
        }
        
        if (comboCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            val comboColor = if (isNextSlashRed) Color(0xFFE07A7A) else Color(0xFFFF9800)
            Text(
                text = "Combo: $comboCount", 
                fontSize = 28.sp, 
                fontWeight = FontWeight.Black, 
                color = comboColor,
                modifier = Modifier.padding(start = 16.dp)
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
    modifier: Modifier = Modifier
) {
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
            drawPath(path = fgPath, color = barColor)
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
            Text(text = "GAME OVER", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRestart, modifier = Modifier.width(200.dp).height(50.dp)) {
                Text("Restart", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onMenu,
                modifier = Modifier.width(200.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Menu", fontSize = 20.sp)
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
            Button(onClick = onResume, modifier = Modifier.width(200.dp).height(50.dp)) {
                Text("Resume", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onMenu,
                modifier = Modifier.width(200.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Menu", fontSize = 20.sp)
            }
        }
    }
}
