package com.example.sweepneko

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
import com.example.sweepneko.ui.theme.SweepNekoTheme
import kotlinx.coroutines.delay
import kotlin.math.sqrt

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val characterWidthDp = 120.dp
    val characterHeightDp = 120.dp
    val characterWidthPx = with(density) { characterWidthDp.toPx() }
    val characterHeightPx = with(density) { characterHeightDp.toPx() }
    
    val enemyWidthDp = 60.dp
    val enemyHeightDp = 60.dp
    val enemyWidthPx = with(density) { enemyWidthDp.toPx() }
    val enemyHeightPx = with(density) { enemyHeightDp.toPx() }
    
    val characterX = screenWidthPx / 2f
    val characterY = screenHeightPx - with(density) { 150.dp.toPx() }
    
    var spawnCount by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        var lastSpawnTime = System.currentTimeMillis()
        while (hp > 0) {
            val currentTime = System.currentTimeMillis()
            
            // Spawn enemy every 5 seconds
            if (currentTime - lastSpawnTime >= 5000) {
                // Spawn in spawn zone (above screen or top of screen)
                val spawnX = (Math.random() * (screenWidthPx - enemyWidthPx) + enemyWidthPx/2f).toFloat()
                val spawnY = -enemyHeightPx // Offscreen above
                spawnCount++
                enemies = enemies + Enemy(id = spawnCount, x = spawnX, y = spawnY, speed = 4f)
                lastSpawnTime = currentTime
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
                
                val charLeft = characterX - characterWidthPx / 2
                val charRight = characterX + characterWidthPx / 2
                val charTop = characterY - characterHeightPx / 2
                val charBottom = characterY + characterHeightPx / 2

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
            
            delay(16) // ~60 FPS
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        
        // Spawn Zone Text Area
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("spawn zone", fontSize = 48.sp, color = Color.Red.copy(alpha = 0.5f), fontWeight = FontWeight.Light)
        }

        // Enemies
        for (enemy in enemies) {
            Image(
                painter = painterResource(id = R.drawable.t_enemy),
                contentDescription = "Enemy",
                modifier = Modifier
                    .offset(
                        x = with(density) { enemy.x.toDp() } - (enemyWidthDp / 2),
                        y = with(density) { enemy.y.toDp() } - (enemyHeightDp / 2)
                    )
                    .size(width = enemyWidthDp, height = enemyHeightDp)
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
        ) {
             Image(
                 painter = painterResource(id = R.drawable.cat_character),
                 contentDescription = "Character",
                 modifier = Modifier
                     .size(width = characterWidthDp, height = characterHeightDp)
                     .border(2.dp, if (isImmune) Color.Red else Color.Transparent)
             )
        }
        
        // HP Bar
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
                Text(text = "HP", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
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
    }
}
