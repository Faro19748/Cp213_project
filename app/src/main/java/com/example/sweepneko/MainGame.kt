package com.example.sweepneko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

enum class EnemyType { NORMAL, BIG, FAST }

data class Enemy(
    var id: Long,
    var x: Float,
    var y: Float,
    val type: EnemyType = EnemyType.NORMAL,
    val speed: Float = 3f,
    var hp: Int = 1,
    var widthPx: Float = 0f,
    var heightPx: Float = 0f,
    var widthDp: Float = 60f,
    var heightDp: Float = 60f,
    var lastHitTime: Long = 0L,
    var lastAttackTime: Long = 0L
)

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    var hp by remember { mutableIntStateOf(100) }
    var stamina by remember { mutableFloatStateOf(100f) }
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

    // Hitbox Size
    val hitboxchar = 100.dp
    val characterHitboxWidthPx = with(density) { hitboxchar.toPx() }
    val characterHitboxHeightPx = with(density) { hitboxchar.toPx() }
    
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
                // Stamina recovery: 10 per 1 second -> 10 per 1000ms
                if (stamina < 100f && slashStart == null) {
                    stamina += (dt * 10f) / 1000f
                    if (stamina > 100f) stamina = 100f
                }

                timeSinceLastSpawn += dt
                // Spawn enemy every 5 seconds
                if (timeSinceLastSpawn >= 2000) {
                    val randType = Math.random()
                    val type: EnemyType
                    val ehp: Int
                    val speed: Float
                    val wDp: Float
                    val hDp: Float
                    if (randType < 0.6) {
                        type = EnemyType.NORMAL
                        ehp = 1
                        speed = 4f
                        wDp = 60f
                        hDp = 60f
                    } else if (randType < 0.8) {
                        type = EnemyType.FAST
                        ehp = 1
                        speed = 8f
                        wDp = 40f
                        hDp = 40f
                    } else {
                        type = EnemyType.BIG
                        ehp = 3
                        speed = 1.5f
                        wDp = 120f
                        hDp = 120f
                    }
                    val eWidthPx = wDp * density.density
                    val eHeightPx = hDp * density.density

                    // Randomly choose spawn side: 0 = Top, 1 = Left, 2 = Right
                    val side = (0..2).random()
                    val spawnX: Float
                    val spawnY: Float
                    when (side) {
                        0 -> { // Top
                            spawnX = (Math.random() * (screenWidthPx - eWidthPx) + eWidthPx / 2f).toFloat()
                            spawnY = -eHeightPx
                        }
                        1 -> { // Left
                            spawnX = -eWidthPx
                            spawnY = (Math.random() * (screenHeightPx / 2f)).toFloat()
                        }
                        else -> { // Right
                            spawnX = screenWidthPx + eWidthPx
                            spawnY = (Math.random() * (screenHeightPx / 2f)).toFloat()
                        }
                    }
                    spawnCount++
                    enemies = enemies + Enemy(
                        id = spawnCount, x = spawnX, y = spawnY, 
                        type = type, speed = speed, hp = ehp, 
                        widthPx = eWidthPx, heightPx = eHeightPx, 
                        widthDp = wDp, heightDp = hDp
                    )
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
                val enemyLeft = nx - enemy.widthPx / 2
                val enemyRight = nx + enemy.widthPx / 2
                val enemyTop = ny - enemy.heightPx / 2
                val enemyBottom = ny + enemy.heightPx / 2
                
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
            for (i in newEnemies.indices) {
                for (j in i + 1 until newEnemies.size) {
                    val e1 = newEnemies[i]
                    val e2 = newEnemies[j]
                    val minDist = (e1.widthPx + e2.widthPx) / 2f
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
                    if (stamina >= 5f) { // Require some minimum stamina to start slashing
                        slashStart = offset
                        slashEnd = offset
                    } else {
                        slashStart = null
                        slashEnd = null
                    }
                },
                onDrag = { change, dragAmount ->
                    if (slashStart != null && slashEnd != null) {
                        val distance = sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                        val staminaCost = distance * 0.02f // Tune this factor for how fast it drains
                        if (stamina >= staminaCost) {
                            stamina -= staminaCost
                            slashEnd = change.position
                        } else {
                            stamina = 0f
                        }
                    }
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
                            val currentTime = System.currentTimeMillis()
                            val remainingEnemies = enemies.mapNotNull { enemy ->
                                var t = ((enemy.x - start.x) * dx + (enemy.y - start.y) * dy) / l2
                                t = max(0f, min(1f, t))
                                val projX = start.x + t * dx
                                val projY = start.y + t * dy
                                val distToEnemySq = (enemy.x - projX) * (enemy.x - projX) + (enemy.y - projY) * (enemy.y - projY)
                                
                                val hitRadius = enemy.widthPx / 2f
                                if (distToEnemySq <= hitRadius * hitRadius) {
                                    if (currentTime - enemy.lastHitTime > 300) {
                                        val newHp = enemy.hp - 1
                                        if (newHp > 0) {
                                            enemy.copy(hp = newHp, lastHitTime = currentTime)
                                        } else null
                                    } else enemy
                                } else enemy
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
        
        // Enemies
        for (enemy in enemies) {
            val painterId = when (enemy.type) {
                EnemyType.FAST -> R.drawable.t_fastenemy
                EnemyType.BIG -> R.drawable.t_bigenemy
                else -> R.drawable.t_enemy
            }
            Image(
                painter = painterResource(id = painterId),
                contentDescription = "Enemy",
                modifier = Modifier
                    .offset(
                        x = with(density) { enemy.x.toDp() } - (enemy.widthDp.dp / 2),
                        y = with(density) { enemy.y.toDp() } - (enemy.heightDp.dp / 2)
                    )
                    .size(width = enemy.widthDp.dp, height = enemy.heightDp.dp)
            )
        }
        
        // Character
        val isImmune = System.currentTimeMillis() < playerImmuneUntil
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { characterX.toDp() } - (characterWidthDp / 2),
                    y = with(density) { characterY.toDp() } - (characterHeightDp / 2)
                )
                .size(width = characterWidthDp, height = characterHeightDp),
            contentAlignment = Alignment.Center
        ) {
             Image(
                 painter = painterResource(id = R.drawable.cat_character),
                 contentDescription = "Character",
                 modifier = Modifier.fillMaxSize()
             )
             
             // Hitbox border
             Box(
                 modifier = Modifier
                     .size(hitboxchar)
                     .border(2.dp, if (isImmune) Color.Red else Color.Transparent)
             )
        }
        
        // Draw Slash Line (on top of enemies and character)
        if (slashStart != null && slashEnd != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Red.copy(alpha = 0.8f),
                    start = slashStart!!,
                    end = slashEnd!!,
                    strokeWidth = 15f,
                    cap = StrokeCap.Round
                )
            }
        }

        // HP and Stamina Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .fillMaxWidth(),
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
                // Fixed width box so HP and SP bars align nicely
                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    Text(text = "HP", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    Text(text = "SP", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
                }
            }
        }
        
        if (hp <= 0) {
            val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "GAME OVER", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            hp = 100
                            stamina = 100f
                            enemies = emptyList()
                            playerImmuneUntil = 0L
                        },
                        modifier = Modifier.width(200.dp).height(50.dp)
                    ) {
                        Text("Restart", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            activity?.finish()
                        },
                        modifier = Modifier.width(200.dp).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Menu", fontSize = 20.sp)
                    }
                }
            }
        }

        // Pause Button
        if (hp > 0 && !isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = { isPaused = true },
                    containerColor = Color.White
                ) {
                    Text("II", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        // Pause Menu
        if (isPaused && hp > 0) {
            val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
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
                        onClick = { isPaused = false },
                        modifier = Modifier.width(200.dp).height(50.dp)
                    ) {
                        Text("Resume", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { activity?.finish() },
                        modifier = Modifier.width(200.dp).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Menu", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}
