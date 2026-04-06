package com.example.sweepneko

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
    val activity = LocalContext.current as? Activity
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val sizechar = 400.dp
    val characterWidthDp = sizechar
    val characterHeightDp = sizechar

    val hitboxchar = 200.dp
    val characterHitboxWidthPx = with(density) { hitboxchar.toPx() }
    val characterHitboxHeightPx = with(density) { hitboxchar.toPx() }
    
    val characterX = screenWidthPx / 2f
    val characterY = screenHeightPx - with(density) { 150.dp.toPx() }
    
    var spawnCount by remember { mutableLongStateOf(0L) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(isPaused) {
        if (isPaused) return@LaunchedEffect
        var lastFrameTime = System.currentTimeMillis()
        var timeSinceLastSpawn = 0L

        while (hp > 0) {
            val currentTime = System.currentTimeMillis()
            val dt = currentTime - lastFrameTime
            lastFrameTime = currentTime

            if (!isPaused) {
                if (stamina < 100f && slashStart == null) {
                    stamina += (dt * 10f) / 1000f
                    if (stamina > 100f) stamina = 100f
                }

                timeSinceLastSpawn += dt
                if (timeSinceLastSpawn >= 2000) {
                    spawnCount++
                    enemies = enemies + Enemy.createRandomSpawn(
                        id = spawnCount, 
                        screenWidthPx = screenWidthPx, 
                        screenHeightPx = screenHeightPx, 
                        pixelDensity = density.density
                    )
                    timeSinceLastSpawn = 0L
                }
            
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
                    
                    val enemyLeft = nx - enemy.widthPx / 2; val enemyRight = nx + enemy.widthPx / 2
                    val enemyTop = ny - enemy.heightPx / 2; val enemyBottom = ny + enemy.heightPx / 2
                    
                    val charLeft = characterX - characterHitboxWidthPx / 2; val charRight = characterX + characterHitboxWidthPx / 2
                    val charTop = characterY - characterHitboxHeightPx / 2; val charBottom = characterY + characterHitboxHeightPx / 2

                    if (enemyRight > charLeft && enemyLeft < charRight &&
                        enemyBottom > charTop && enemyTop < charBottom) {
                        nx = enemy.x; ny = enemy.y
                        if (currentTime > playerImmuneUntil) {
                            newHp -= 25
                            playerImmuneUntil = currentTime + 3000L
                        }
                    }
                    enemy.copy(x = nx, y = ny)
                }
                
                for (i in newEnemies.indices) {
                    for (j in i + 1 until newEnemies.size) {
                        val e1 = newEnemies[i]; val e2 = newEnemies[j]
                        val minDist = (e1.widthPx + e2.widthPx) / 2f
                        val dx = e2.x - e1.x; val dy = e2.y - e1.y
                        val dist2 = dx*dx + dy*dy
                        if (dist2 < minDist * minDist && dist2 > 0) {
                            val distance = sqrt(dist2.toDouble()).toFloat()
                            val overlap = minDist - distance
                            val pushX = (dx/distance) * overlap * 0.5f; val pushY = (dy/distance) * overlap * 0.5f
                            e1.x -= pushX; e1.y -= pushY; e2.x += pushX; e2.y += pushY
                        }
                    }
                }
                hp = newHp
                enemies = newEnemies
            }
            delay(16)
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
                    if (stamina >= 5f) { slashStart = offset; slashEnd = offset }
                },
                onDrag = { change, dragAmount ->
                    if (slashStart != null && slashEnd != null) {
                        val distance = sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                        val staminaCost = distance * 0.02f
                        if (stamina >= staminaCost) {
                            stamina -= staminaCost
                            slashEnd = change.position
                        } else { stamina = 0f }
                    }
                },
                onDragEnd = {
                    val start = slashStart; val end = slashEnd
                    if (start != null && end != null) {
                        val dx = end.x - start.x; val dy = end.y - start.y
                        val distSq = dx*dx + dy*dy
                        if (distSq > 2500f) {
                            val l2 = distSq
                            val currentTime = System.currentTimeMillis()
                            enemies = enemies.mapNotNull { enemy ->
                                var t = ((enemy.x - start.x) * dx + (enemy.y - start.y) * dy) / l2
                                t = max(0f, min(1f, t))
                                val projX = start.x + t * dx; val projY = start.y + t * dy
                                val distToEnemySq = (enemy.x - projX) * (enemy.x - projX) + (enemy.y - projY) * (enemy.y - projY)
                                
                                val hitRadius = enemy.widthPx / 2f
                                if (distToEnemySq <= hitRadius * hitRadius) {
                                    if (currentTime - enemy.lastHitTime > 300) {
                                        val newEnemyHp = enemy.hp - 1
                                        if (newEnemyHp > 0) enemy.copy(hp = newEnemyHp, lastHitTime = currentTime) else null
                                    } else enemy
                                } else enemy
                            }
                        }
                    }
                    slashStart = null; slashEnd = null
                },
                onDragCancel = { slashStart = null; slashEnd = null }
            )
        }
    ) {
        // Enemies
        enemies.forEach { enemy ->
            val painterId = when (enemy.type) {
                EnemyType.FAST -> R.drawable.t_fastenemy
                EnemyType.BIG -> R.drawable.t_bigenemy
                else -> R.drawable.t_enemy
            }
            Image(
                painter = painterResource(id = painterId),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = with(density) { enemy.x.toDp() } - (enemy.widthDp.dp / 2),
                            y = with(density) { enemy.y.toDp() } - (enemy.heightDp.dp / 2))
                    .size(width = enemy.widthDp.dp, height = enemy.heightDp.dp)
            )
        }
        
        // Character
        val isImmune = System.currentTimeMillis() < playerImmuneUntil
        Box(
            modifier = Modifier
                .offset(x = with(density) { characterX.toDp() } - (characterWidthDp / 2),
                        y = with(density) { characterY.toDp() } - (characterHeightDp / 2))
                .size(width = characterWidthDp, height = characterHeightDp),
            contentAlignment = Alignment.Center
        ) {
             Image(painter = painterResource(id = R.drawable.cat_character), contentDescription = null, modifier = Modifier.fillMaxSize())
             Box(modifier = Modifier.size(hitboxchar).border(2.dp, if (isImmune) Color.Red else Color.Transparent))
        }
        
        if (slashStart != null && slashEnd != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color = Color.Red.copy(alpha = 0.8f), start = slashStart!!, end = slashEnd!!, strokeWidth = 15f, cap = StrokeCap.Round)
            }
        }

        // ใช้ Component จากไฟล์ GameComponents.kt
        HpStaminaBar(hp = hp, stamina = stamina)
        
        if (hp <= 0) {
            GameOverMenu(
                onRestart = { hp = 100; stamina = 100f; enemies = emptyList(); playerImmuneUntil = 0L },
                onMenu = { activity?.finish() }
            )
        }

        if (hp > 0 && !isPaused) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(onClick = { isPaused = true }, containerColor = Color.White) {
                    Text("II", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        if (isPaused && hp > 0) {
            PauseMenu(onResume = { isPaused = false }, onMenu = { activity?.finish() })
        }
    }
}
