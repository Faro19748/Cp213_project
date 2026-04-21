package com.example.sweepneko

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*
import androidx.compose.animation.Animatable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.example.sweepneko.ui.theme.SweepNekoTheme
import kotlin.math.min

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
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    GameMenuScreen()
                }
            }
        }
    }
}

@Composable
fun GameMenuScreen(modifier: Modifier = Modifier) {
    var showExitDialog by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val characterYTargetPx = remember(density, configuration) { 
        with(density) {
            val screenHeightPx = configuration.screenHeightDp.dp.toPx()
            val sizechar = min(configuration.screenWidthDp, configuration.screenHeightDp).dp * 0.8f
            screenHeightPx - (sizechar * 0.4f).toPx()
        }
    }
    
    val animY = remember { Animatable(0f) }
    val animScale = remember { Animatable(1.4f) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isStarting = false
                scope.launch {
                    animY.snapTo(0f)
                    animScale.snapTo(1.4f)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    LaunchedEffect(isStarting) {
        if (isStarting) {
            // 1. Shake animation
            repeat(4) {
                animY.animateTo(15f, tween(40))
                animY.animateTo(-15f, tween(40))
            }
            animY.animateTo(0f, tween(40))
            
            // 2. Move to bottom and scale to game size
            coroutineScope {
                launch {
                    // Calculate the relative offset needed to reach the target Y position in the GameScreen
                    // In MenuGame, the character is inside a Box with weight(1f) and center alignment.
                    // To match the GameScreen's position (bottom area), we move it to characterYTargetPx.
                    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
                    val centerToBottomOffsetPx = (screenHeightPx / 2f) - (characterYTargetPx)
                    animY.animateTo(-centerToBottomOffsetPx / density.density + 80f, tween(500, easing = FastOutLinearInEasing))
                }
                launch {
                    animScale.animateTo(0.8f, tween(500))
                }
            }
            
            delay(100)
            activity?.startActivity(android.content.Intent(activity, MainGame::class.java))
            @Suppress("DEPRECATION")
            activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    val bgPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.bg)
            .build(), 
        imageLoader = imageLoader
    )

    val logoPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.logo)
            .build(), 
        imageLoader = imageLoader
    )
    
    val spinPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.spin)
            .build(), 
        imageLoader = imageLoader
    )
    
    val menuCharPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.cat_char)
            .size(coil.size.Size.ORIGINAL)
            .build(),
        imageLoader = imageLoader
    )

    // Logo Animation
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val translateY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

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

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = bgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // 1. Logo (Top Center)
            Image(
                painter = logoPainter,
                contentDescription = "Logo",
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(1f)
                    .graphicsLayer {
                        translationY = translateY
                        rotationZ = rotation
                    },
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )

            // 2. Character Image (Center)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .offset(y = animY.value.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = if (isStarting) spinPainter else menuCharPainter,
                    contentDescription = "Character",
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .aspectRatio(1f)
                        .scale(animScale.value)
                )
            }

            // Buttons
            if (!isStarting) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.offset(y = (-40).dp)
                ) {
                    MenuText(
                        text = "PLAY",
                        color = Color.Black,
                        onClick = { isStarting = true }
                    )
                    MenuText(
                        text = "SETTING",
                        color = Color.Black,
                        onClick = { /* TODO: Setting Action */ }
                    )
                    MenuText(
                        text = "EXIT",
                        color = Color.Red,
                        onClick = { showExitDialog = true }
                    )
                }
            } else {
                // Spacer to maintain layout while buttons are hidden
                Spacer(modifier = Modifier.height(260.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MenuText(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Text(
        text = text,
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        color = color,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Remove default ripple for cleaner look
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 16.dp)
    )
}
