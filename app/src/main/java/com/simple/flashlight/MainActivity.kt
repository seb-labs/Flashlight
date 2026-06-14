package com.simple.flashlight

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlashlightScreen(activity = this)
        }
    }
}

@Composable
fun FlashlightScreen(
    activity: MainActivity,
    vm: FlashlightViewModel = viewModel()
) {
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.refreshFlashHardware()
        }
    }

    LaunchedEffect(Unit) {
        vm.initScreenLight(activity)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            vm.refreshFlashHardware()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isOn by vm.isFlashlightOn.collectAsState()
    val hasFlash by vm.hasFlashHardware.collectAsState()
    val isSos by vm.isSosActive.collectAsState()
    val timerRunning by vm.isTimerRunning.collectAsState()
    val timerRemaining by vm.timerRemaining.collectAsState()
    val screenLightOn by vm.screenLightOn.collectAsState()

    var batteryLevel by remember { mutableIntStateOf(vm.getBatteryLevel()) }
    var isCharging by remember { mutableStateOf(vm.isCharging()) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                if (level >= 0 && scale > 0) {
                    batteryLevel = (level * 100 / scale.toFloat()).toInt()
                }
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    val anyActive = isOn || isSos || screenLightOn

    val buttonColor by animateColorAsState(
        targetValue = when {
            isSos -> Color(0xFFFF1744)
            screenLightOn -> Color(0xFFFFEA00)
            isOn -> Color(0xFF76FF03)
            else -> Color(0xFF424242)
        },
        animationSpec = tween(300),
        label = "btn_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    var showTimerDialog by remember { mutableStateOf(false) }
    if (showTimerDialog) {
        TimerDialog(
            isRunning = timerRunning,
            remainingText = formatClock(timerRemaining),
            onDismiss = { showTimerDialog = false },
            onSelect = { minutes ->
                showTimerDialog = false
                vm.startTimer(minutes)
            },
            onCancel = {
                vm.cancelTimer()
                showTimerDialog = false
            }
        )
    }

    val errorMsg by vm.errorMessage.collectAsState()
    errorMsg?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            vm.clearError()
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = when {
                            screenLightOn -> listOf(
                                Color(0xFFFFFDEB),
                                Color(0xFFFFF6B8),
                                Color(0xFFFFFDEB)
                            )
                            else -> listOf(
                                Color(0xFF0A0D12),
                                Color(0xFF101827),
                                Color(0xFF0A0D12)
                            )
                        }
                    )
                )
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (screenLightOn) Color.White.copy(alpha = 0.88f) else Color.Transparent
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Simple Flashlight",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hasFlash) "LED-Taschenlampe · schnell & direkt" else "Bildschirmlicht · LED nicht verfügbar",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161C28))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                isSos -> "SOS aktiv"
                                screenLightOn -> "Bildschirmlicht an"
                                isOn -> "LED an"
                                timerRunning -> "Timer läuft"
                                else -> "Bereit"
                            },
                            color = if (anyActive) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { vm.toggle() },
                            modifier = Modifier
                                .size(190.dp)
                                .scale(if (anyActive) pulseScale else 1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = if (anyActive) 18.dp else 4.dp,
                                pressedElevation = 2.dp
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (anyActive) Icons.Filled.FlashOn else Icons.Outlined.FlashOn,
                                contentDescription = if (anyActive) "Ausschalten" else "Einschalten",
                                modifier = Modifier.size(76.dp),
                                tint = if (anyActive) Color.Black else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = when {
                                isSos -> "SOS blinkt gerade"
                                screenLightOn -> "Bildschirm als Lichtquelle"
                                isOn -> "LED-Taschenlampe aktiv"
                                timerRunning -> "Auto-Off in ${formatClock(timerRemaining)}"
                                else -> "Tippe auf den Kreis zum Einschalten"
                            },
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SmallActionButton(
                            icon = Icons.Filled.Sos,
                            label = "SOS",
                            isActive = isSos,
                            activeColor = Color(0xFFFF1744),
                            onClick = { vm.toggleSos() }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        SmallActionButton(
                            icon = if (timerRunning) Icons.Filled.TimerOff else Icons.Filled.Timer,
                            label = if (timerRunning) formatClock(timerRemaining) else "Timer",
                            isActive = timerRunning,
                            activeColor = Color(0xFF448AFF),
                            onClick = { showTimerDialog = true }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        SmallActionButton(
                            icon = if (screenLightOn) Icons.Filled.LightMode else Icons.Outlined.LightMode,
                            label = "Display",
                            isActive = screenLightOn,
                            activeColor = Color(0xFFFFEA00),
                            onClick = { vm.toggleScreenLightOnly() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161C28))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text(
                            text = "Akku",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BatteryIndicator(level = batteryLevel, isCharging = isCharging)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SmallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.2f) else Color(0xFF1E1E1E)
    val contentColor = if (isActive) activeColor else Color.White.copy(alpha = 0.7f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = bgColor,
                contentColor = contentColor
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BatteryIndicator(level: Int, isCharging: Boolean) {
    val batteryColor = when {
        level <= 15 -> Color(0xFFFF1744)
        level <= 30 -> Color(0xFFFF9100)
        else -> Color(0xFF76FF03)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Icon(
            imageVector = when {
                isCharging -> Icons.Filled.BatteryChargingFull
                level >= 90 -> Icons.Filled.BatteryFull
                level >= 50 -> Icons.Filled.Battery5Bar
                level >= 30 -> Icons.Filled.Battery3Bar
                else -> Icons.Filled.Battery1Bar
            },
            contentDescription = "Akku",
            tint = batteryColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (level >= 0) "$level%" else "--%",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        if (isCharging) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "⚡", fontSize = 12.sp)
        }
    }
}

@Composable
fun TimerDialog(
    isRunning: Boolean,
    remainingText: String,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                text = if (isRunning) "Timer läuft" else "Auto-Off Timer",
                color = Color.White
            )
        },
        text = {
            if (isRunning) {
                Text(
                    text = "Noch $remainingText. Das Licht wird automatisch ausgeschaltet.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text = "Wann soll das Licht automatisch ausgeschaltet werden?",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            if (isRunning) {
                TextButton(onClick = onCancel) {
                    Text("Timer abbrechen", color = Color(0xFFFF1744))
                }
            } else {
                Column {
                    TextButton(onClick = { onSelect(1) }) {
                        Text("1 Minute", color = Color(0xFF448AFF))
                    }
                    TextButton(onClick = { onSelect(5) }) {
                        Text("5 Minuten", color = Color(0xFF448AFF))
                    }
                    TextButton(onClick = { onSelect(10) }) {
                        Text("10 Minuten", color = Color(0xFF448AFF))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

private fun formatClock(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
