package com.example.sweepneko

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

import androidx.compose.animation.core.*
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

@Composable
fun GameScreen(modifier: Modifier = Modifier, viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val activity = LocalActivity.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeOffset"
    )

    val shakeOffsetX: Float
    val shakeOffsetY: Float
    
    val elapsed = System.currentTimeMillis() - state.shakeTriggerTime
    if (elapsed < 500) {
        val decay = 1f - (elapsed / 500f)
        val currentIntensity = state.shakeIntensity * decay
        shakeOffsetX = (shakeAnim - 0.5f) * 2f * currentIntensity
        shakeOffsetY = (shakeAnim - 0.5f) * 2f * currentIntensity
    } else {
        shakeOffsetX = 0f
        shakeOffsetY = 0f
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("SweepNekoPrefs", android.content.Context.MODE_PRIVATE) }
    val isRealCat = remember { prefs.getBoolean("is_real_cat", false) }
    
    val screenWidthPx = remember(configuration, density) { with(density) { configuration.screenWidthDp.dp.toPx() } }
    val screenHeightPx = remember(configuration, density) { with(density) { configuration.screenHeightDp.dp.toPx() } }

    val sizechar = remember(configuration) { min(configuration.screenWidthDp, configuration.screenHeightDp).dp * 0.8f }

    val hitboxchar = remember(sizechar) { sizechar * 0.375f }
    val characterHitboxWidthPx = remember(hitboxchar, density) { with(density) { hitboxchar.toPx() } }
    val characterHitboxHeightPx = remember(hitboxchar, density) { with(density) { hitboxchar.toPx() } }
    
    val characterSizePx = remember(sizechar, density) { with(density) { sizechar.toPx() } }
    
    val characterX = remember(screenWidthPx) { screenWidthPx / 2f }
    val characterY = remember(screenHeightPx, sizechar, density) { screenHeightPx - with(density) { (sizechar * 0.4f).toPx() } }

    val shooterStopDistDraw = remember(density) { 450f * density.density }
    val shooterStopDistDrawSq = remember(shooterStopDistDraw) { shooterStopDistDraw * shooterStopDistDraw }

    LaunchedEffect(screenWidthPx, screenHeightPx, density.density) {
        viewModel.screenWidthPx = screenWidthPx
        viewModel.screenHeightPx = screenHeightPx
        viewModel.pixelDensity = density.density
        viewModel.characterX = characterX
        viewModel.characterY = characterY
        viewModel.characterHitboxWidthPx = characterHitboxWidthPx
        viewModel.characterHitboxHeightPx = characterHitboxHeightPx
        viewModel.startGameLoop()
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

    // Pre-load painters with specific sizes to reduce memory/CPU usage
    val bulletPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.bullet)
            .size(100) // Limit decode size
            .build(), 
        imageLoader = imageLoader
    )
    val fastEnemyPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.sm_enemy)
            .size(180)
            .build(), 
        imageLoader = imageLoader
    )
    val bigEnemyPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.b_enemy)
            .size(450)
            .build(), 
        imageLoader = imageLoader
    )
    val shootEnemyMovePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.s_enemy)
            .size(300)
            .build(), 
        imageLoader = imageLoader
    )
    val shootEnemyIdlePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.ss_enemy)
            .size(300)
            .build(), 
        imageLoader = imageLoader
    )
    val normalEnemyPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.n_enemy)
            .size(240)
            .build(), 
        imageLoader = imageLoader
    )
    val bgPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.bg)
            .build(), 
        imageLoader = imageLoader
    )
    val charPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(if (isRealCat) R.drawable.realcat else R.drawable.b_cat)
            .size(if (isRealCat) 200 else 600)
            .build(), 
        imageLoader = imageLoader
    )
    val bossPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.boss)
            .size(800)
            .build(), 
        imageLoader = imageLoader
    )
    
    val catCanPainter = painterResource(id = R.drawable.catcan)
    val catBarPainter = painterResource(id = R.drawable.carbar)
    val timePainter = painterResource(id = R.drawable.time)

    val deadFastPainter = painterResource(id = R.drawable.d_sm_enemy)
    val deadBigPainter = painterResource(id = R.drawable.d_b_enemy)
    val deadShootPainter = painterResource(id = R.drawable.d_s_enemy)
    val deadNormalPainter = painterResource(id = R.drawable.d_n_enemy)
    val deadBossPainter = painterResource(id = R.drawable.d_boss)
    
    val bombPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.bomb)
            .build(), 
        imageLoader = imageLoader
    )

    var showBomb by remember { mutableStateOf(false) }

    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver) {
            showBomb = true
            delay(1000)
            showBomb = false
            SoundManager.playGameOverMusic()
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .systemGestureExclusion()
        .graphicsLayer {
            translationX = shakeOffsetX
            translationY = shakeOffsetY
        }
    ) {
        Image(
            painter = bgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.isPaused) {
                if (state.isPaused) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset -> viewModel.onSlashStart(offset) },
                    onDrag = { change, dragAmount ->
                        if (dragAmount.getDistanceSquared() > 10f) {
                            viewModel.onSlashDrag(change.position)
                        }
                    },
                    onDragEnd = { viewModel.onSlashEnd() },
                    onDragCancel = { viewModel.onSlashCancel() }
                )
            }
        ) {
            // Projectiles
            state.projectiles.forEach { p ->
                key(p.id) {
                    Image(
                        painter = bulletPainter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = p.widthDp.dp, height = p.heightDp.dp)
                            .graphicsLayer {
                                translationX = p.x - (p.widthDp * density.density) / 2
                                translationY = p.y - (p.heightDp * density.density) / 2
                            }
                    )
                }
            }
            
            // PowerUps
            state.powerUps.forEach { pu ->
                key(pu.id) {
                    val painter = when(pu.type) {
                        PowerUpType.CAT_CAN -> catCanPainter
                        PowerUpType.CAT_BAR -> catBarPainter
                        PowerUpType.TIME_STOP -> timePainter
                    }
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = pu.widthDp.dp, height = pu.heightDp.dp)
                            .graphicsLayer {
                                translationX = pu.x - (pu.widthDp * density.density) / 2
                                translationY = pu.y - (pu.heightDp * density.density) / 2
                            }
                    )
                }
            }

            // Enemies
            state.enemies.forEach { enemy ->
                key(enemy.id) {
                    val dx = characterX - enemy.x
                    val dy = characterY - enemy.y
                    val distSq = dx*dx + dy*dy
                    val isInsideScreen = enemy.x >= enemy.widthPx / 2f && 
                                         enemy.x <= screenWidthPx - enemy.widthPx / 2f && 
                                         enemy.y >= enemy.heightPx / 2f && 
                                         enemy.y <= screenHeightPx - enemy.heightPx / 2f
                    val isMoving = !(enemy.type == EnemyType.SHOOTING && distSq <= shooterStopDistDrawSq && isInsideScreen)
                                         
                    val localPainter = when (enemy.type) {
                        EnemyType.FAST -> fastEnemyPainter
                        EnemyType.BIG -> bigEnemyPainter
                        EnemyType.SHOOTING -> if (isMoving) shootEnemyMovePainter else shootEnemyIdlePainter
                        EnemyType.BOSS -> bossPainter
                        else -> normalEnemyPainter
                    }
                    
                    val isHitEffectActive = (enemy.type == EnemyType.BOSS) && 
                                            (System.currentTimeMillis() - enemy.lastHitTime < 200)

                    Box(
                        modifier = Modifier
                            .size(width = enemy.widthDp.dp, height = enemy.heightDp.dp)
                            .graphicsLayer {
                                translationX = enemy.x - enemy.widthPx / 2
                                translationY = enemy.y - enemy.heightPx / 2
                                scaleX = if (enemy.isFlipped) -1f else 1f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = localPainter,
                            contentDescription = null,
                            colorFilter = if (isHitEffectActive) androidx.compose.ui.graphics.ColorFilter.tint(Color.Red.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            // Fading (Dead) Enemies
            val currentTimeRender = System.currentTimeMillis()
            state.fadingEnemies.forEach { fading ->
                key("dead_${fading.enemy.id}") {
                    val alpha = max(0f, 1f - (currentTimeRender - fading.deathTime) / 1000f)
                    val painter = when (fading.enemy.type) {
                        EnemyType.FAST -> deadFastPainter
                        EnemyType.BIG -> deadBigPainter
                        EnemyType.SHOOTING -> deadShootPainter
                        EnemyType.BOSS -> deadBossPainter
                        else -> deadNormalPainter
                    }
                    Image(
                        painter = painter,
                        contentDescription = null,
                        alpha = alpha,
                        modifier = Modifier
                            .size(width = fading.enemy.widthDp.dp, height = fading.enemy.heightDp.dp)
                            .graphicsLayer {
                                translationX = fading.enemy.x - fading.enemy.widthPx / 2
                                translationY = fading.enemy.y - fading.enemy.heightPx / 2
                                scaleX = if (fading.enemy.isFlipped) -1f else 1f
                            }
                    )
                }
            }
            
            // Character
            val isImmune = System.currentTimeMillis() < state.playerImmuneUntil
            Box(
                modifier = Modifier
                    .size(sizechar)
                    .graphicsLayer {
                        translationX = characterX - characterSizePx / 2
                        translationY = characterY - characterSizePx / 2
                    },
                contentAlignment = Alignment.Center
            ) {
                 if (showBomb) {
                    Image(painter = bombPainter, contentDescription = "Bomb", modifier = Modifier.fillMaxSize())
                 } else if (!state.isGameOver) {
                    Image(painter = charPainter, contentDescription = null, modifier = Modifier.fillMaxSize())
                 }
            }
            
            if (state.slashStart != null && state.slashEnd != null || state.fadingSlashes.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val drawSlash = { start: Offset, end: Offset, alphaOuter: Float, alphaInner: Float, isRed: Boolean, isGold: Boolean ->
                        val dx = end.x - start.x
                        val dy = end.y - start.y
                        val dist = sqrt(dx*dx + dy*dy)
                        if (dist > 0) {
                            val nx = -dy / dist
                            val ny = dx / dist
                            val midX = (start.x + end.x) / 2f
                            val midY = (start.y + end.y) / 2f
                            
                            val multiplier = if (isGold) 2.2f else if (isRed) 1.8f else 1f
                            val outerDist = min(150f, dist * 0.12f * multiplier)
                            val innerDist = min(25f, dist * 0.02f * multiplier)
                            val coreDist = min(80f, dist * 0.06f * multiplier)
                            val strokeWidth = min(35f, max(5f, dist * 0.02f * multiplier))

                            val cpOuterX = midX + nx * outerDist
                            val cpOuterY = midY + ny * outerDist
                            val cpInnerX = midX + nx * innerDist
                            val cpInnerY = midY + ny * innerDist
                            
                            val pathOuter = Path().apply {
                                moveTo(start.x, start.y)
                                quadraticTo(cpOuterX, cpOuterY, end.x, end.y)
                                quadraticTo(cpInnerX, cpInnerY, start.x, start.y)
                                close()
                            }
                            val coreCpX = midX + nx * coreDist
                            val coreCpY = midY + ny * coreDist
                            val pathCore = Path().apply {
                                moveTo(start.x, start.y)
                                quadraticTo(coreCpX, coreCpY, end.x, end.y)
                            }

                            val outerColor = if (isGold) Color(0xFFFFD700) else if (isRed) Color(0xFFFF1111) else Color(0xFF7AAEE0)
                            val alphaMult = if (isGold) 1.5f else if (isRed) 1.2f else 1f
                            
                            drawPath(
                                path = pathOuter,
                                color = outerColor.copy(alpha = min(1f, alphaOuter * alphaMult)),
                            )
                            drawPath(
                                path = pathCore,
                                color = Color.White.copy(alpha = alphaInner),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            
                            if (isRed || isGold) {
                                val innerGlowPath = Path().apply {
                                    moveTo(start.x, start.y)
                                    quadraticTo(coreCpX, coreCpY, end.x, end.y)
                                }
                                val glowColor = if (isGold) Color.White else Color(0xFFFFD700)
                                drawPath(
                                    path = innerGlowPath,
                                    color = glowColor.copy(alpha = alphaInner * 0.8f),
                                    style = Stroke(width = strokeWidth * 0.4f, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    if (state.slashStart != null && state.slashEnd != null) {
                        if (state.isUltimateActive) {
                            val dx = state.slashEnd!!.x - state.slashStart!!.x
                            val dy = state.slashEnd!!.y - state.slashStart!!.y
                            val angle = 0.6f
                            val cosA = kotlin.math.cos(angle.toDouble()).toFloat()
                            val sinA = kotlin.math.sin(angle.toDouble()).toFloat()
                            val p1x = state.slashStart!!.x + (dx * cosA - dy * sinA)
                            val p1y = state.slashStart!!.y + (dx * sinA + dy * cosA)
                            val p2x = state.slashStart!!.x + (dx * cosA + dy * sinA)
                            val p2y = state.slashStart!!.y + (-dx * sinA + dy * cosA)
                            
                            drawSlash(state.slashStart!!, state.slashEnd!!, 0.9f, 1f, false, true)
                            drawSlash(state.slashStart!!, Offset(p1x, p1y), 0.9f, 1f, false, true)
                            drawSlash(state.slashStart!!, Offset(p2x, p2y), 0.9f, 1f, false, true)
                        } else {
                            drawSlash(state.slashStart!!, state.slashEnd!!, 0.9f, 1f, state.isNextSlashRed, false)
                        }
                    }
                    
                    val currentTime = System.currentTimeMillis()
                    state.fadingSlashes.forEach { slash ->
                        val elapsed = currentTime - slash.startTime
                        val progress = elapsed / 400f
                        if (progress in 0f..1f) {
                            val alpha = max(0f, 1f - progress)
                            drawSlash(slash.start, slash.end, alpha * 0.9f, alpha, slash.isRed, slash.isGold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp)
            ) {
                HpStaminaBar(
                    hp = state.hp, 
                    stamina = state.stamina, 
                    ultimateGauge = state.ultimateGauge, 
                    comboCount = state.comboCount, 
                    isNextSlashRed = state.isNextSlashRed,
                    wave = state.wave,
                    enemiesKilled = state.enemiesKilledInWave,
                    targetKills = state.targetKillsForWave,
                    isInfiniteSP = System.currentTimeMillis() < state.infiniteStaminaUntil
                )
            }
            
            if (state.isGameOver) {
                GameOverMenu(
                    onRestart = { viewModel.restartGame() },
                    onMenu = { activity?.finish() }
                )
            }

            if (!state.isGameOver && !state.isPaused) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Wave Info will be drawn inside HpStaminaBar now or nearby

                    Box(
                        modifier = Modifier.align(Alignment.BottomStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            InventoryRow(
                                inventory = state.inventory,
                                onUse = { viewModel.usePowerUp(it) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { state.ultimateGauge / 100f },
                                    modifier = Modifier.size(64.dp),
                                    color = Color(0xFFFFD700),
                                    strokeWidth = 4.dp,
                                    trackColor = Color.Gray.copy(alpha = 0.5f)
                                )
                                FloatingActionButton(
                                    onClick = { viewModel.activateUltimate() }, 
                                    containerColor = if (state.ultimateGauge >= 100f) Color(0xFFFFD700).copy(alpha = 0.9f) else Color.DarkGray.copy(alpha = 0.7f),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Text(
                                        "ULT", 
                                        fontSize = 16.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (state.ultimateGauge >= 100f) Color.Black else Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                    
                    FloatingActionButton(
                        onClick = { viewModel.pauseGame() }, 
                        containerColor = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text("II", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            if (state.isPaused && !state.isGameOver) {
                PauseMenu(
                    onResume = { viewModel.resumeGame() }, 
                    onMenu = { 
                        viewModel.saveHighScore(state.wave, state.maxComboInRun)
                        activity?.finish() 
                    }
                )
            }
        }
    }
}
