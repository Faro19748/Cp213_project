package com.example.sweepneko

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sweepneko.ui.theme.SweepNekoTheme

class MenuGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SweepNekoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameMenuScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GameMenuScreen(modifier: Modifier = Modifier) {
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = (LocalContext.current as? Activity)

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = "Confirm Exit") },
            text = { Text(text = "Are you sure you want to exit the application?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    activity?.finish()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // 1. Game Name (Top Center)
        Text(
            text = "SweepNeko",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )

        // 2. Character Image (Center)
        // ใส่ Box ครอบไว้แล้วกำหนด weight(1f) เพื่อให้ Box นี้เป็นตัว "กันชน" ไม่ให้ปุ่มเลื่อน
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat_character),
                contentDescription = "Character",
                modifier = Modifier.size(500.dp)
            )
        }

        // Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 3. Play Button
            Button(
                onClick = { 
                    activity?.startActivity(android.content.Intent(activity, MainGame::class.java))
                },
                modifier = Modifier
                    .width(240.dp)
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Play", fontSize = 24.sp)
            }

            // 4. Setting Button
            Button(
                onClick = { /* TODO: Setting Action */ },
                modifier = Modifier
                    .width(240.dp)
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Setting", fontSize = 24.sp)
            }
            
            // 5. Exit Button with confirmation
            Button(
                onClick = { showExitDialog = true },
                modifier = Modifier
                    .width(240.dp)
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(text = "Exit", fontSize = 24.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
