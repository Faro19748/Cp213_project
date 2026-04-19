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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sweepneko.ui.theme.SweepNekoTheme

import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

class MenuGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immersive Fullscreen Mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

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
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

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
            .background(Color(0xFFFFEDCE))
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
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(R.drawable.cat_char)
                        .size(coil.size.Size.ORIGINAL)
                        .build(),
                    imageLoader = imageLoader
                ),
                contentDescription = "Character",
                modifier = Modifier
                    .fillMaxSize(1f) // เต็มพื้นที่อ้างอิง Box
                    .aspectRatio(1f) // คุมสัดส่วนภาพไม่ให้บิดเบี้ยว
                    .scale(1.4f) // สเกลขยายขนาดเกิน Box โดยไม่เบียดองค์ประกอบอื่น
            )
        }

        // Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.offset(y = (-40).dp)
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
