package com.example.sweepneko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
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

import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
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
        SoundManager.init(this)
        setContent {
            SweepNekoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameMenuScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SoundManager.playMenuMusic()
    }
}

@Composable
fun GameMenuScreen(modifier: Modifier = Modifier) {
    val showExitDialog = remember { mutableStateOf(false) }
    val showSettingDialog = remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val prefs = remember { context.getSharedPreferences("SweepNekoPrefs", android.content.Context.MODE_PRIVATE) }
    var highScore by remember { mutableStateOf(prefs.getInt("high_score_wave", 1)) }
    var highCombo by remember { mutableStateOf(prefs.getInt("high_score_combo", 0)) }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val characterYTargetPx = remember(density, windowInfo.containerSize) { 
        val screenWidthPx = windowInfo.containerSize.width.toFloat()
        val screenHeightPx = windowInfo.containerSize.height.toFloat()
        val sizecharPx = min(screenWidthPx, screenHeightPx) * 0.8f
        screenHeightPx - sizecharPx * 0.4f
    }
    
    val animY = remember { Animatable(0f) }
    val animScale = remember { Animatable(1.4f) }
    val bounceScale = remember { Animatable(1f) }
    
    // UI Transitions
    val logoOffsetAnim = remember { Animatable(0f) }
    val uiAlphaAnim = remember { Animatable(0f) } // สำหรับ Best Wave และ Buttons (Popup effect)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isStarting = false
                // Update scores from prefs immediately on resume
                highScore = prefs.getInt("high_score_wave", 1)
                highCombo = prefs.getInt("high_score_combo", 0)
                
                scope.launch {
                    // Reset animations when returning to menu
                    launch { animY.snapTo(0f) }
                    launch { animScale.snapTo(1.4f) }
                    launch { logoOffsetAnim.snapTo(0f) }
                    launch { uiAlphaAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
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
                add(ImageDecoderDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    LaunchedEffect(isStarting) {
        if (isStarting) {
            // UI Disappear
            launch { uiAlphaAnim.animateTo(0f, tween(300)) }
            launch { logoOffsetAnim.animateTo(-400f, tween(500, easing = BackEaseIn)) }

            // 1. Shake animation
            repeat(4) {
                animY.animateTo(15f, tween(40))
                animY.animateTo(-15f, tween(40))
            }
            animY.animateTo(0f, tween(40))
            
            // 2. Move to bottom and scale to game size
            coroutineScope {
                launch {
                    val screenHeightPx = windowInfo.containerSize.height.toFloat()
                    val centerToBottomOffsetPx = (screenHeightPx / 2f) - characterYTargetPx
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
    
    var isRealCat by remember { mutableStateOf(prefs.getBoolean("is_real_cat", false)) }
    var catClickCount by remember { mutableStateOf(0) }

    val menuCharPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(if (isRealCat) R.drawable.realcat else R.drawable.cat_char)
            .size(coil.size.Size.ORIGINAL)
            .build(),
        imageLoader = imageLoader
    )

    val bombPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.bomb)
            .build(),
        imageLoader = imageLoader
    )
    
    var showBombEffect by remember { mutableStateOf(false) }

    // Logo Floating Animation
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

    if (showExitDialog.value) {
        AlertDialog(
            onDismissRequest = { showExitDialog.value = false },
            title = { Text(text = "Confirm Exit") },
            text = { Text(text = "Are you sure you want to exit the application?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog.value = false
                    activity?.finish()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSettingDialog.value) {
        AlertDialog(
            onDismissRequest = { showSettingDialog.value = false },
            containerColor = Color(0xFFEAB676),
            title = { Text(text = "Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Background Music", fontWeight = FontWeight.Medium, color = Color.Black)
                            TextButton(onClick = { }) {
                                Text("Preview", color = Color(0xFF887164))
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .offset(y = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                            )
                            Slider(
                                value = SoundManager.bgmVolumeSnapshot.value,
                                onValueChange = { SoundManager.setBGMVolume(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF887164),
                                    activeTrackColor = Color(0xFF887164),
                                    inactiveTrackColor = Color(0xFF887164).copy(alpha = 0.3f),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Sound Effects", fontWeight = FontWeight.Medium, color = Color.Black)
                            TextButton(onClick = { SoundManager.playSFX("slash") }) {
                                Text("Test SFX", color = Color(0xFF887164))
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .offset(y = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                            )
                            Slider(
                                value = SoundManager.sfxVolumeSnapshot.value,
                                onValueChange = { SoundManager.setSFXVolume(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF887164),
                                    activeTrackColor = Color(0xFF887164),
                                    inactiveTrackColor = Color(0xFF887164).copy(alpha = 0.3f),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Button(
                        onClick = {
                            prefs.edit()
                                .putInt("high_score_wave", 1)
                                .putInt("high_score_combo", 0)
                                .apply()
                            highScore = 1
                            highCombo = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Statistics", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF887164))
                ) {
                    Text("Close", color = Color.White)
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
                        translationY = translateY + logoOffsetAnim.value
                        rotationZ = rotation
                        alpha = uiAlphaAnim.value
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
                            .scale(animScale.value * bounceScale.value * if (isRealCat) 0.7f else 1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isStarting) {
                                    scope.launch {
                                        // Bounce animation
                                        bounceScale.animateTo(
                                            targetValue = 1.15f,
                                            animationSpec = tween(100, easing = FastOutSlowInEasing)
                                        )
                                        bounceScale.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }

                                    catClickCount++
                                    if (catClickCount >= 10) {
                                        SoundManager.playSFX("bomb")
                                        showBombEffect = true
                                        scope.launch {
                                            delay(1000)
                                            showBombEffect = false
                                            isRealCat = !isRealCat
                                            prefs.edit().putBoolean("is_real_cat", isRealCat).apply()
                                        }
                                        catClickCount = 0
                                    }
                                }
                            }
                    )
                    
                    if (showBombEffect) {
                        Image(
                            painter = bombPainter,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(1f)
                                .aspectRatio(1f)
                                .scale(animScale.value * bounceScale.value * if (isRealCat) 0.7f else 1f)
                        )
                    }
            }

            // Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .offset(y = (-40).dp)
                    .graphicsLayer {
                        alpha = uiAlphaAnim.value
                        scaleX = 0.8f + (uiAlphaAnim.value * 0.2f)
                        scaleY = 0.8f + (uiAlphaAnim.value * 0.2f)
                    }
            ) {
                if (!isStarting || uiAlphaAnim.value > 0.1f) {
                    MenuText(
                        text = "PLAY",
                        color = Color.Black,
                        backgroundColor = Color(0xFFEAB676),
                        delay = 0,
                        onClick = { isStarting = true }
                    )
                    MenuText(
                        text = "SETTING",
                        color = Color.Black,
                        backgroundColor = Color(0xFFEAB676),
                        delay = 200,
                        onClick = { showSettingDialog.value = true }
                    )
                    MenuText(
                        text = "EXIT",
                        color = Color.White,
                        backgroundColor = Color(0xFFE53935),
                        delay = 400,
                        onClick = { showExitDialog.value = true }
                    )
                } else {
                    Spacer(modifier = Modifier.height(260.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // High Score Display (Bottom Left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .graphicsLayer { 
                    alpha = uiAlphaAnim.value
                    translationX = -100f * (1f - uiAlphaAnim.value)
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "BEST WAVE: $highScore",
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.width(2.dp).height(16.dp).background(Color.White.copy(alpha = 0.3f)))
                Text(
                    text = "BEST HIT: $highCombo",
                    color = Color(0xFFE07A7A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MenuText(
    text: String,
    color: Color,
    delay: Int = 0,
    backgroundColor: Color = Color(0xFFEAB676),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offsetTransition by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                delayMillis = delay,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_offset"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = offsetTransition
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor.copy(alpha = 0.7f))
                .matchParentSize()
        )
        
        Box(
            modifier = Modifier
                .offset(y = if (isPressed) 2.dp else 0.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp, horizontal = 32.dp)
        ) {
            Text(
                text = text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

// Easing helpers
val BackEaseIn = Easing { fraction ->
    val s = 1.70158f
    fraction * fraction * ((s + 1) * fraction - s)
}
