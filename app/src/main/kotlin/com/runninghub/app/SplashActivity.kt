package com.runninghub.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.runninghub.app.data.repository.DiscoveryRepository
import com.runninghub.app.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var discoveryRepository: DiscoveryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // P2 Decision: Preload UserInfo and Discovery Data concurrently 
        // to warm up singletons before MainActivity is created.
        lifecycleScope.launch {
            async { userRepository.refreshUserData() }
            async { discoveryRepository.refreshBanners() }
            async { discoveryRepository.refreshCategories() }
        }

        enableEdgeToEdge()

        setContent {
            SplashScreen {
                // Animation finished, navigate to main activity
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}

@Composable
fun SplashScreen(onAnimationEnd: () -> Unit) {
    // Colors
    val bgDark = Color(0xFF020617) // slate-950
    val emerald400 = Color(0xFF34D399)
    val emerald500 = Color(0xFF10B981)
    val blue500 = Color(0xFF3B82F6)
    val blue600 = Color(0xFF2563EB)
    
    // Setup Navigation Trigger
    LaunchedEffect(Unit) {
        delay(2800) // Total animation duration before navigating
        onAnimationEnd()
    }

    // --- Animations ---
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    
    // Core Glow Pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    // Vertical Streams (Data logic)
    val stream1Y by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stream1"
    )
    val stream2Y by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stream2"
    )
    val stream3Y by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stream3"
    )

    // Entry Animations
    val enterTransition = updateTransition(targetState = true, label = "enter")
    
    // "Running" text animation
    val runningX by enterTransition.animateFloat(
        transitionSpec = { tween(1200, easing = FastOutSlowInEasing) },
        label = "runningX"
    ) { state -> if (state) 0f else -150f }
    val runningY by enterTransition.animateFloat(
        transitionSpec = { tween(1200, easing = FastOutSlowInEasing) },
        label = "runningY"
    ) { state -> if (state) 0f else -150f }
    val runningScale by enterTransition.animateFloat(
        transitionSpec = { tween(1200, easing = FastOutSlowInEasing) },
        label = "runningScale"
    ) { state -> if (state) 1f else 1.5f }
    val runningAlpha by enterTransition.animateFloat(
        transitionSpec = { tween(800, easing = LinearEasing) },
        label = "runningAlpha"
    ) { state -> if (state) 1f else 0f }

    // "Hub" pop animation
    val hubScale by enterTransition.animateFloat(
        transitionSpec = { 
            keyframes {
                durationMillis = 1400
                0f at 0
                0f at 300 // delay
                1.3f at 700 with FastOutSlowInEasing
                0.9f at 1000 with FastOutLinearInEasing
                1f at 1400 with FastOutSlowInEasing
            }
        },
        label = "hubScale"
    ) { state -> if (state) 1f else 0f }
    val hubAlpha by enterTransition.animateFloat(
        transitionSpec = { 
            keyframes {
                durationMillis = 1400
                0f at 0
                0f at 300
                1f at 700
                1f at 1400
            }
        },
        label = "hubAlpha"
    ) { state -> if (state) 1f else 0f }

    // Loading Bar & Text
    val loadingY by enterTransition.animateFloat(
        transitionSpec = { 
            keyframes {
                durationMillis = 2000
                50f at 0
                50f at 1000 // delayed start
                0f at 2000 with EaseOut
            }
        },
        label = "loadingY"
    ) { state -> if (state) 0f else 50f }
    val loadingAlpha by enterTransition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 2000
                0f at 0
                0f at 1000
                1f at 2000
            }
        },
        label = "loadingAlpha"
    ) { state -> if (state) 1f else 0f }
    
    val loadingProgress by enterTransition.animateFloat(
        transitionSpec = { tween(2500, easing = EaseOutExpo) },
        label = "loadingProgress"
    ) { state -> if (state) 1f else 0f }

    val systemTextAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sysTextAlpha"
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .clip(RoundedCornerShape(0.dp))
    ) {
        // Vertical Streams
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f)) {
            val width = size.width
            val height = size.height
            
            // Stream 1
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, emerald500, Color.Transparent),
                    startY = stream1Y * height - height/2,
                    endY = stream1Y * height + height/2
                ),
                start = Offset(width * 0.2f, 0f),
                end = Offset(width * 0.2f, height),
                strokeWidth = 3f
            )
            // Stream 2 (Fast)
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, blue500, Color.Transparent),
                    startY = stream2Y * height - height/2,
                    endY = stream2Y * height + height/2
                ),
                start = Offset(width * 0.5f, 0f),
                end = Offset(width * 0.5f, height),
                strokeWidth = 6f
            )
            // Stream 3 (Slow)
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, emerald400, Color.Transparent),
                    startY = stream3Y * height - height/2,
                    endY = stream3Y * height + height/2
                ),
                start = Offset(width * 0.8f, 0f),
                end = Offset(width * 0.8f, height),
                strokeWidth = 3f
            )
        }

        // Core Glow (Canvas radial gradient to avoid clipping artifacts)
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(400.dp) // larger bounds to contain the glow
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                    alpha = glowAlpha
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        blue600.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                ),
                radius = size.width / 2f
            )
        }

        // Center Content (Logo Layout)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Running"
            Text(
                text = "Running",
                color = emerald400,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-2).sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = emerald400.copy(alpha = 0.6f),
                        blurRadius = 40f
                    )
                ),
                modifier = Modifier
                    .graphicsLayer {
                        translationX = runningX
                        translationY = runningY
                        scaleX = runningScale
                        scaleY = runningScale
                        alpha = runningAlpha
                    }
                    .offset(x = (-16).dp, y = 10.dp) // Offset to overlap and stagger
            )

            // "Hub"
            Box(
                modifier = Modifier
                    .offset(x = 32.dp, y = (-8).dp)
                    .graphicsLayer {
                        scaleX = hubScale
                        scaleY = hubScale
                        alpha = hubAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                // Pulse Ring Behind Hub
                val hubPingScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "hubPingScale"
                )
                val hubPingAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "hubPingAlpha"
                )
                
                Box(modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = hubPingScale
                        scaleY = hubPingScale
                        alpha = hubPingAlpha
                    }
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
                )

                Text(
                    text = "Hub",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .shadow(
                            elevation = 20.dp,
                            spotColor = blue600,
                            ambientColor = blue600,
                            shape = RoundedCornerShape(16.dp),
                            clip = false
                        )
                        .background(blue600, RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Loading Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .graphicsLayer {
                    translationY = loadingY
                    alpha = loadingAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SYSTEM STARTING",
                color = Color(0xFF94A3B8), // slate-400
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp, // 0.2em approx
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .alpha(systemTextAlpha)
            )
            
            // Progress Bar Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(percent = 50)) // slate-800
                    .clip(RoundedCornerShape(percent = 50))
            ) {
                // Progress Bar Fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = loadingProgress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(emerald400, blue500)
                            )
                        )
                )
            }
        }
    }
}
