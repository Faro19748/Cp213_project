package com.example.sweepneko.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.sweepneko.R
import com.example.sweepneko.ui.theme.SweepNekoTheme
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

class MainGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immersive Fullscreen Mode (Hide system navigation gestures)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        
        enableEdgeToEdge()
        setContent {
            SweepNekoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

data class Enemy(
    val id: Long,
    val x: Float,
    val y: Float,
    val speed: Float = 3f,
    val lastAttackTime: Long = 0L
)

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    var hp by remember { mutableIntStateOf(100) }
    var sp by remember { mutableIntStateOf(100) }
    var enemies by remember { mutableStateOf(listOf<Enemy>()) }
    var playerImmuneUntil by remember { mutableLongStateOf(0L) }
    
    var slashStart by remember { mutableStateOf<Offset?>(null) }
    var slashEnd by remember { mutableStateOf<Offset?>(null) }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Optimize: Cache density-dependent pixel values to avoid re-calculating on every frame
    val screenWidthPx = remember(configuration.screenWidthDp, density) { with(density) { configuration.screenWidthDp.dp.toPx() } }
    val screenHeightPx = remember(configuration.screenHeightDp, density) { with(density) { configuration.screenHeightDp.dp.toPx() } }

    val characterWidthDp = 400.dp
    val characterHeightDp = 400.dp

    val hitboxchar = 100.dp
    val characterHitboxWidthPx = remember(density) { with(density) { hitboxchar.toPx() } }
    val characterHitboxHeightPx = remember(density) { with(density) { hitboxchar.toPx() } }
    
    val enemyWidthDp = 60.dp
    val enemyHeightDp = 60.dp
    val enemyWidthPx = remember(density) { with(density) { enemyWidthDp.toPx() } }
    val enemyHeightPx = remember(density) { with(density) { enemyHeightDp.toPx() } }
    
    val characterX = screenWidthPx / 2f
    val characterY = remember(screenHeightPx, density) { screenHeightPx - with(density) { 150.dp.toPx() } }
    
    var spawnCount by remember { mutableLongStateOf(0L) }

    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastFrameTime = System.currentTimeMillis()
        var timeSinceLastSpawn = 0L

        while (hp > 0) {
            withFrameMillis { currentTime ->
                val dt = currentTime - lastFrameTime
                lastFrameTime = currentTime

                if (!isPaused) {
                    timeSinceLastSpawn += dt
                    // Spawn 4 enemies every 5 seconds
                    if (timeSinceLastSpawn >= 5000) {
                        val newSpawnedEnemies = mutableListOf<Enemy>()
                        repeat(4) {
                            val spawnX = (Math.random() * (screenWidthPx - enemyWidthPx) + enemyWidthPx / 2f).toFloat()
                            val spawnY = -with(density) { (100 + Math.random() * 200).dp.toPx() }
                            spawnCount++
                            newSpawnedEnemies.add(Enemy(id = spawnCount, x = spawnX, y = spawnY, speed = 4f))
                        }
                        enemies = enemies + newSpawnedEnemies
                        timeSinceLastSpawn = 0L
                    }

                    // Move enemies & check collision
                    var newHp = hp
                    val currentImmuneUntil = playerImmuneUntil
                    val movedEnemies = enemies.map { enemy ->
                        val dx = characterX - enemy.x
                        val dy = characterY - enemy.y
                        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                        var nx = enemy.x
                        var ny = enemy.y
                        if (dist > 0) {
                            nx += (dx / dist) * enemy.speed
                            ny += (dy / dist) * enemy.speed
                        }

                        // Collision
                        val enemyLeft = nx - enemyWidthPx / 2
                        val enemyRight = nx + enemyWidthPx / 2
                        val enemyTop = ny - enemyHeightPx / 2
                        val enemyBottom = ny + enemyHeightPx / 2

                        val charLeft = characterX - characterHitboxWidthPx / 2
                        val charRight = characterX + characterHitboxWidthPx / 2
                        val charTop = characterY - characterHitboxHeightPx / 2
                        val charBottom = characterY + characterHitboxHeightPx / 2

                        if (enemyRight > charLeft && enemyLeft < charRight &&
                            enemyBottom > charTop && enemyTop < charBottom
                        ) {
                            nx = enemy.x
                            ny = enemy.y

                            if (currentTime > currentImmuneUntil) {
                                newHp -= 25
                                playerImmuneUntil = currentTime + 3000L
                            }
                        }
                        enemy.copy(x = nx, y = ny)
                    }

                    // Resolve overlap (Optimized: only if more than 1 enemy)
                    val finalEnemies = if (movedEnemies.size > 1) {
                        val result = movedEnemies.toMutableList()
                        for (i in result.indices) {
                            for (j in i + 1 until result.size) {
                                val e1 = result[i]
                                val e2 = result[j]
                                val dx = e2.x - e1.x
                                val dy = e2.y - e1.y
                                val dist2 = dx * dx + dy * dy
                                if (dist2 < enemyWidthPx * enemyWidthPx && dist2 > 0) {
                                    val distance = sqrt(dist2.toDouble()).toFloat()
                                    val overlap = enemyWidthPx - distance
                                    val pushX = (dx / distance) * overlap * 0.5f
                                    val pushY = (dy / distance) * overlap * 0.5f
                                    result[i] = e1.copy(x = e1.x - pushX, y = e1.y - pushY)
                                    result[j] = e2.copy(x = e2.x + pushX, y = e2.y + pushY)
                                }
                            }
                        }
                        result
                    } else {
                        movedEnemies
                    }

                    hp = newHp
                    enemies = finalEnemies
                }
            }
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .systemGestureExclusion()
        .background(Color(0xFFEEEEEE))
        .pointerInput(isPaused) {
            if (isPaused) return@pointerInput
            detectDragGestures(
                onDragStart = { offset ->
                    slashStart = offset
                    slashEnd = offset
                },
                onDrag = { change, _ ->
                    slashEnd = change.position
                },
                onDragEnd = {
                    val start = slashStart
                    val end = slashEnd
                    if (start != null && end != null) {
                        val dx = end.x - start.x
                        val dy = end.y - start.y
                        val distSq = dx*dx + dy*dy
                        // Check if it's a drag line (distance > 50) to prevent single tap
                        if (distSq > 2500f) {
                            val remainingEnemies = enemies.filterNot { enemy ->
                                var t = ((enemy.x - start.x) * dx + (enemy.y - start.y) * dy) / distSq
                                t = max(0f, min(1f, t))
                                val projX = start.x + t * dx
                                val projY = start.y + t * dy
                                val distToEnemySq = (enemy.x - projX) * (enemy.x - projX) + (enemy.y - projY) * (enemy.y - projY)
                                
                                val hitRadius = enemyWidthPx / 2f
                                distToEnemySq <= hitRadius * hitRadius
                            }
                            enemies = remainingEnemies
                        }
                    }
                    slashStart = null
                    slashEnd = null
                },
                onDragCancel = {
                    slashStart = null
                    slashEnd = null
                }
            )
        }
    ) {
        // Sub-components
        SlashLineEffect(slashStart, slashEnd)
        SpawnZoneText()
        
        enemies.forEach { enemy ->
            key(enemy.id) {
                EnemyView(enemy, enemyWidthDp, enemyHeightDp)
            }
        }

        val isImmune = System.currentTimeMillis() < playerImmuneUntil
        PlayerCharacterView(isImmune, characterX, characterY, characterWidthDp, characterHeightDp, hitboxchar)
        
        GameHealthBar(hp, sp)
        
        if (hp <= 0) {
            val activity = androidx.activity.compose.LocalActivity.current
            GameOverPanel(
                onRestart = { hp = 100; sp = 100; enemies = emptyList(); playerImmuneUntil = 0L },
                onMenu = { activity?.finish() }
            )
        }

        if (hp > 0 && !isPaused) {
            PauseButton(onPause = { isPaused = true })
        }

        if (isPaused && hp > 0) {
            val activity = androidx.activity.compose.LocalActivity.current
            PauseMenuPanel(
                onResume = { isPaused = false },
                onMenu = { activity?.finish() }
            )
        }
    }
}

// --- Separated Components ---

@Composable
fun SlashLineEffect(start: Offset?, end: Offset?) {
    if (start != null && end != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.Cyan.copy(alpha = 0.8f),
                start = start,
                end = end,
                strokeWidth = 15f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SpawnZoneText() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Text("spawn zone", fontSize = 48.sp, color = Color.Red.copy(alpha = 0.5f), fontWeight = FontWeight.Light)
    }
}

@Composable
fun EnemyView(
    enemy: Enemy,
    widthDp: androidx.compose.ui.unit.Dp,
    heightDp: androidx.compose.ui.unit.Dp
) {
    Image(
        painter = painterResource(id = R.drawable.n_enemy),
        contentDescription = "Enemy",
        modifier = Modifier
            .size(width = widthDp, height = heightDp)
            .graphicsLayer {
                translationX = enemy.x - size.width / 2
                translationY = enemy.y - size.height / 2
            }
    )
}

@Composable
fun PlayerCharacterView(
    isImmune: Boolean,
    x: Float, y: Float,
    widthDp: androidx.compose.ui.unit.Dp, heightDp: androidx.compose.ui.unit.Dp,
    hitboxDp: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(width = widthDp, height = heightDp)
            .graphicsLayer {
                translationX = x - size.width / 2
                translationY = y - size.height / 2
            },
        contentAlignment = Alignment.Center
    ) {
         Image(
             painter = painterResource(id = R.drawable.cat_char),
             contentDescription = "Character",
             modifier = Modifier.fillMaxSize()
         )
         Box(
             modifier = Modifier
                 .size(hitboxDp)
                 .border(2.dp, if (isImmune) Color.Red else Color.Transparent)
         )
    }
}

@Composable
fun GameHealthBar(hp: Int, sp: Int) {
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
        
        // SP Bar Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                CustomBar(
                    progress = sp / 100f,
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
fun GameOverPanel(onRestart: () -> Unit, onMenu: () -> Unit) {
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
fun PauseButton(onPause: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(onClick = onPause, containerColor = Color.White) {
            Text("II", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun PauseMenuPanel(onResume: () -> Unit, onMenu: () -> Unit) {
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
