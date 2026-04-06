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

@Composable
fun HpStaminaBar(hp: Int, stamina: Float) {
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HP Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(30.dp)
                    .border(1.dp, Color.Black)
                    .background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (hp > 0) hp / 100f else 0f)
                        .fillMaxHeight()
                        .background(Color.Red.copy(alpha = 0.6f))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "HP", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // SP Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .border(1.dp, Color.Black)
                    .background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (stamina > 0f) stamina / 100f else 0f)
                        .fillMaxHeight()
                        .background(Color.Blue.copy(alpha = 0.6f))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "SP", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
        }
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
