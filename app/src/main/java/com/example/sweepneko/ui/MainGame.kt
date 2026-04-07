package com.example.sweepneko.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.systemGestureExclusion

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
    var id: Long,
    var x: Float,
    var y: Float,
    val speed: Float = 3f,
    var lastAttackTime: Long = 0L
)

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    var hp by remember { mutableIntStateOf(100) }
    var enemies by remember { mutableStateOf(listOf<Enemy>()) }
    var playerImmuneUntil by remember { mutableLongStateOf(0L) }
    
    var slashStart by remember { mutableStateOf<Offset?>(null) }
    var slashEnd by remember { mutableStateOf<Offset?>(null) }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    //sizechar
    val sizechar = 400.dp
    val characterWidthDp = sizechar
    val characterHeightDp = sizechar
    val characterWidthPx = with(density) { characterWidthDp.toPx() }
    val characterHeightPx = with(density) { characterHeightDp.toPx() }

    // Hitbox Size
    val hitboxchar = 100.dp
    val characterHitboxWidthPx = with(density) { hitboxchar.toPx() }
    val characterHitboxHeightPx = with(density) { hitboxchar.toPx() }
    
    val enemyWidthDp = 60.dp
    val enemyHeightDp = 60.dp
    val enemyWidthPx = with(density) { enemyWidthDp.toPx() }
    val enemyHeightPx = with(density) { enemyHeightDp.toPx() }
    
    val characterX = screenWidthPx / 2f
    val characterY = screenHeightPx - with(density) { 150.dp.toPx() }
    
    var spawnCount by remember { mutableLongStateOf(0L) }

    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastFrameTime = System.currentTimeMillis()
        var timeSinceLastSpawn = 0L

        while (hp > 0) {
            val currentTime = System.currentTimeMillis()
            val dt = currentTime - lastFrameTime
            lastFrameTime = currentTime

            if (!isPaused) {
                timeSinceLastSpawn += dt
                // Spawn enemy every 5 seconds
                if (timeSinceLastSpawn >= 5000) {
                    // Spawn in spawn zone (above screen or top of screen)
                    val spawnX = (Math.random() * (screenWidthPx - enemyWidthPx) + enemyWidthPx/2f).toFloat()
                    val spawnY = -enemyHeightPx // Offscreen above
                    spawnCount++
                    enemies = enemies + Enemy(id = spawnCount, x = spawnX, y = spawnY, speed = 4f)
                    timeSinceLastSpawn = 0L
                }
            
            // Move enemies & check collision
            var newHp = hp
            val newEnemies = enemies.map { enemy ->
                val dx = characterX - enemy.x
                val dy = characterY - enemy.y
                val dist = sqrt((dx*dx + dy*dy).toDouble()).toFloat()
                
                var nx = enemy.x
                var ny = enemy.y
                if (dist > 0) {
                    nx += (dx/dist) * enemy.speed
                    ny += (dy/dist) * enemy.speed
                }
                
                // Collision (AABB Bounding Box based on PNG dimensions)
                val enemyLeft = nx - enemyWidthPx / 2
                val enemyRight = nx + enemyWidthPx / 2
                val enemyTop = ny - enemyHeightPx / 2
                val enemyBottom = ny + enemyHeightPx / 2
                
                val charLeft = characterX - characterHitboxWidthPx / 2
                val charRight = characterX + characterHitboxWidthPx / 2
                val charTop = characterY - characterHitboxHeightPx / 2
                val charBottom = characterY + characterHitboxHeightPx / 2

                if (enemyRight > charLeft && enemyLeft < charRight &&
                    enemyBottom > charTop && enemyTop < charBottom) {
                    // Revert movement to stop at the collision boundary
                    nx = enemy.x
                    ny = enemy.y
                    
                    if (currentTime > playerImmuneUntil) {
                        newHp -= 25
                        playerImmuneUntil = currentTime + 3000L // 3 seconds immune/cooldown
                    }
                }
                
                enemy.copy(x = nx, y = ny)
            }
            
            // Resolve overlap between enemies (circle-based for smooth sliding)
            val minDist = enemyWidthPx
            for (i in newEnemies.indices) {
                for (j in i + 1 until newEnemies.size) {
                    val e1 = newEnemies[i]
                    val e2 = newEnemies[j]
                    val dx = e2.x - e1.x
                    val dy = e2.y - e1.y
                    val dist2 = dx*dx + dy*dy
                    if (dist2 < minDist * minDist && dist2 > 0) {
                        val distance = sqrt(dist2.toDouble()).toFloat()
                        val overlap = minDist - distance
                        val pushX = (dx/distance) * overlap * 0.5f
                        val pushY = (dy/distance) * overlap * 0.5f
                        e1.x -= pushX
                        e1.y -= pushY
                        e2.x += pushX
                        e2.y += pushY
                    }
                }
            }
            
                hp = newHp
                enemies = newEnemies
            }
            
            delay(16) // ~60 FPS
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
                            val l2 = distSq
                            val remainingEnemies = enemies.filterNot { enemy ->
                                var t = ((enemy.x - start.x) * dx + (enemy.y - start.y) * dy) / l2
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
            EnemyView(enemy, density, enemyWidthDp, enemyHeightDp) 
        }
        
        val isImmune = System.currentTimeMillis() < playerImmuneUntil
        PlayerCharacterView(isImmune, density, characterX, characterY, characterWidthDp, characterHeightDp, hitboxchar)
        
        GameHealthBar(hp)
        
        if (hp <= 0) {
            val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
            GameOverPanel(
                onRestart = { hp = 100; enemies = emptyList(); playerImmuneUntil = 0L },
                onMenu = { activity?.finish() }
            )
        }

        if (hp > 0 && !isPaused) {
            PauseButton(onPause = { isPaused = true })
        }

        if (isPaused && hp > 0) {
            val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
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
    density: androidx.compose.ui.unit.Density,
    widthDp: androidx.compose.ui.unit.Dp,
    heightDp: androidx.compose.ui.unit.Dp
) {
    Image(
        painter = painterResource(id = R.drawable.t_enemy),
        contentDescription = "Enemy",
        modifier = Modifier
            .offset(
                x = with(density) { enemy.x.toDp() } - (widthDp / 2),
                y = with(density) { enemy.y.toDp() } - (heightDp / 2)
            )
            .size(width = widthDp, height = heightDp)
    )
}

@Composable
fun PlayerCharacterView(
    isImmune: Boolean,
    density: androidx.compose.ui.unit.Density,
    x: Float, y: Float,
    widthDp: androidx.compose.ui.unit.Dp, heightDp: androidx.compose.ui.unit.Dp,
    hitboxDp: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .offset(
                x = with(density) { x.toDp() } - (widthDp / 2),
                y = with(density) { y.toDp() } - (heightDp / 2)
            )
            .size(width = widthDp, height = heightDp),
        contentAlignment = Alignment.Center
    ) {
         Image(
             painter = painterResource(id = R.drawable.cat_character),
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
fun GameHealthBar(hp: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
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
