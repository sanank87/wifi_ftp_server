package com.wififtp.server.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.wififtp.server.data.*
import com.wififtp.server.ui.MainViewModel
import com.wififtp.server.ui.components.*
import com.wififtp.server.ui.theme.*
import com.wififtp.server.util.NetworkUtils
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val serverState = state.serverState
    val isRunning = serverState.isRunning
    val isStarting = serverState.isStarting

    // Pulse animation for the button ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    // Button press scale
    var buttonPressed by remember { mutableStateOf(false) }
    val btnScale by animateFloatAsState(
        targetValue = if (buttonPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale",
        finishedListener = { buttonPressed = false },
    )

    var showQr by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("FTP Server", style = MaterialTheme.typography.headlineMedium)
                Text("Wireless File Transfer", style = MaterialTheme.typography.bodySmall)
            }
            StatusBadge(isRunning)
        }

        // Big Toggle Button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Animated pulse rings
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .border(2.dp, Success.copy(alpha = pulseAlpha), CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .border(1.5.dp, Success.copy(alpha = pulseAlpha * 0.5f), CircleShape),
                    )
                }
                // Main button
                Button(
                    onClick = {
                        buttonPressed = true
                        if (isRunning) viewModel.stopServer() else viewModel.startServer()
                    },
                    modifier = Modifier
                        .size(120.dp)
                        .scale(btnScale),
                    shape = CircleShape,
                    enabled = !isStarting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Success else Color(0xFF2D3748),
                        disabledContainerColor = Color(0xFF2D3748),
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 12.dp,
                        pressedElevation = 4.dp,
                    ),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (isStarting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                            )
                        } else {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(38.dp),
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = when {
                                isStarting -> "STARTING"
                                isRunning  -> "STOP"
                                else       -> "START"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
        }

        // Connection Info Card
        AppCard(glow = isRunning, modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Connection Info")
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoBox(
                    label = "IP ADDRESS",
                    value = if (isRunning) serverState.ipAddress else "—",
                    modifier = Modifier.weight(1f),
                )
                InfoBox(
                    label = "PORT",
                    value = if (isRunning) serverState.port.toString() else "—",
                    valueColor = if (isRunning) Primary else TextMuted,
                    modifier = Modifier.weight(1f),
                )
            }
            if (isRunning) {
                Spacer(Modifier.height(14.dp))
                CopyRow(label = "FTP URL", value = viewModel.ftpUrl)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showQr = !showQr },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                    ) {
                        Icon(Icons.Default.QrCode, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (showQr) "Hide QR" else "QR Code", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(viewModel.ftpUrl))
                            viewModel.showSnackbar("URL copied!")
                        },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy URL", fontSize = 12.sp)
                    }
                }
            }
        }

        // QR Code
        if (isRunning && showQr && state.qrBitmap != null) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Scan to Connect")
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Image(
                            bitmap = state.qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(160.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Open in FileZilla, WinSCP, or any FTP client",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }

        // Live Stats Row
        if (isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    icon = Icons.Default.Devices,
                    label = "Connected",
                    value = serverState.connectedClients.size.toString(),
                    tint = Warning,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Default.SwapVert,
                    label = "Transfers",
                    value = state.transfers.count { it.status == TransferStatus.ACTIVE }.toString(),
                    tint = Primary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Default.Wifi,
                    label = "Wifi",
                    value = "ON",
                    tint = Success,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // How-to tip (shown when stopped)
        if (!isRunning) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("How to connect from PC", style = MaterialTheme.typography.titleMedium)
                        listOf(
                            "1. Tap START to launch the FTP server",
                            "2. Open FileZilla or WinSCP on your PC",
                            "3. Host: IP shown above · Port: 2121",
                            "4. Username: admin · Password: admin123",
                            "5. Click Connect and browse files",
                        ).forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = TextDim)
                        }
                    }
                }
            }
        }
    }
}
