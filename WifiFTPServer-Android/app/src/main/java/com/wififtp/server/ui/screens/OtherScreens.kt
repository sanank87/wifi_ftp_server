package com.wififtp.server.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.wififtp.server.data.*
import com.wififtp.server.ui.MainViewModel
import com.wififtp.server.ui.components.*
import com.wififtp.server.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════════
// CONNECTED DEVICES SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun DevicesScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val clients = state.serverState.connectedClients
    val transfers = state.transfers

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Devices", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${clients.size} client${if (clients.size != 1) "s" else ""} connected",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (!state.serverState.isRunning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.WifiOff,
                    title = "Server not running",
                    subtitle = "Start the FTP server from the Dashboard to allow connections",
                )
            }
        } else if (clients.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.DevicesOther,
                    title = "No devices connected",
                    subtitle = "Open FileZilla on your PC and connect using the URL shown on the Dashboard",
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(clients, key = { it.id }) { client ->
                    val activeTransfers = transfers.filter {
                        it.clientIp == client.ipAddress && it.status == TransferStatus.ACTIVE
                    }
                    DeviceCard(client = client, activeTransfers = activeTransfers)
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(client: ConnectedClient, activeTransfers: List<TransferLog>) {
    val hasTransfer = activeTransfers.isNotEmpty()
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    AppCard(glow = hasTransfer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (hasTransfer) Success.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(12.dp),
                    )
                    .border(
                        1.dp,
                        if (hasTransfer) Success.copy(alpha = 0.3f) else BorderColor,
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Computer, null, tint = if (hasTransfer) Success else TextMuted, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(client.ipAddress, style = MaterialTheme.typography.titleMedium)
                Text("Connected ${fmt.format(Date(client.connectedAt))}", style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(if (hasTransfer) Success else Primary, CircleShape),
            )
        }

        Spacer(Modifier.height(10.dp))
        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard2, RoundedCornerShape(8.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Upload, null, tint = Success, modifier = Modifier.size(12.dp))
                Text(formatBytes(client.bytesUploaded), style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Uploaded", style = MaterialTheme.typography.labelSmall)
            }
            Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderColor))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Download, null, tint = Primary, modifier = Modifier.size(12.dp))
                Text(formatBytes(client.bytesDownloaded), style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Downloaded", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Active transfers
        activeTransfers.forEach { transfer ->
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard2, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (transfer.type == TransferType.UPLOAD) Icons.Default.Upload else Icons.Default.Download,
                            null, tint = Primary, modifier = Modifier.size(12.dp),
                        )
                        Text(transfer.filename, style = MaterialTheme.typography.bodySmall, color = TextDim, maxLines = 1, modifier = Modifier.widthIn(max = 160.dp))
                    }
                    Text("${transfer.progress}%", style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                TransferProgressBar(transfer.progress)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ACTIVITY SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ActivityScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val allTransfers = state.transfers
    val activeTransfers = allTransfers.filter { it.status == TransferStatus.ACTIVE }
    val historyTransfers = allTransfers.filter { it.status != TransferStatus.ACTIVE }
    val fmt = remember { SimpleDateFormat("HH:mm · MMM dd", Locale.getDefault()) }

    val totalUploaded = allTransfers.filter { it.type == TransferType.UPLOAD && it.status == TransferStatus.DONE }.sumOf { it.sizeBytes }
    val totalDownloaded = allTransfers.filter { it.type == TransferType.DOWNLOAD && it.status == TransferStatus.DONE }.sumOf { it.sizeBytes }

    Column(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Activity", style = MaterialTheme.typography.headlineMedium)
                    Text("Transfer history & live progress", style = MaterialTheme.typography.bodySmall)
                }
                if (historyTransfers.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearTransferHistory() }) {
                        Text("Clear", color = Error, fontSize = 12.sp)
                    }
                }
            }
        }

        if (allTransfers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.SwapVert,
                    title = "No transfer history",
                    subtitle = "File transfers will appear here when clients connect and send or receive files",
                )
            }
            return
        }

        // Summary cards
        if (allTransfers.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(Icons.Default.Upload, "Uploaded", formatBytes(totalUploaded), Success, Modifier.weight(1f))
                StatCard(Icons.Default.Download, "Downloaded", formatBytes(totalDownloaded), Primary, Modifier.weight(1f))
                StatCard(Icons.Default.Bolt, "Active", activeTransfers.size.toString(), Warning, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            if (activeTransfers.isNotEmpty()) {
                item { SectionLabel("ACTIVE", modifier = Modifier.padding(bottom = 8.dp)) }
                items(activeTransfers, key = { it.id }) { log ->
                    TransferLogItem(log = log, fmt = fmt)
                    Spacer(Modifier.height(6.dp))
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            if (historyTransfers.isNotEmpty()) {
                item { SectionLabel("HISTORY", modifier = Modifier.padding(bottom = 8.dp)) }
                items(historyTransfers, key = { it.id }) { log ->
                    TransferLogItem(log = log, fmt = fmt)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun TransferLogItem(log: TransferLog, fmt: SimpleDateFormat) {
    val isActive = log.status == TransferStatus.ACTIVE
    val isUpload = log.type == TransferType.UPLOAD
    val iconColor = if (isUpload) Success else Primary

    AppCard(glow = isActive, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isUpload) Icons.Default.Upload else Icons.Default.Download,
                    null, tint = iconColor, modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(log.filename, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(fmt.format(Date(log.timestampMs)), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    "${formatBytes(log.sizeBytes)} · ${log.clientIp}" +
                        if (isActive && log.speedBytesPerSec > 0) " · ${formatBytes(log.speedBytesPerSec)}/s" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))
                when (log.status) {
                    TransferStatus.ACTIVE -> TransferProgressBar(log.progress)
                    TransferStatus.DONE   -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Success, modifier = Modifier.size(12.dp))
                        Text("Complete", style = MaterialTheme.typography.bodySmall, color = Success)
                    }
                    TransferStatus.ERROR  -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Cancel, null, tint = Error, modifier = Modifier.size(12.dp))
                        Text("Failed", style = MaterialTheme.typography.bodySmall, color = Error)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val config = state.config

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPortDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // Security section
        SectionLabel("Security")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SettingRow(
                icon = Icons.Default.Lock,
                iconTint = Primary,
                label = "Require Authentication",
                subtitle = "Username & password login",
                trailing = { Switch(checked = config.requireAuth, onCheckedChange = viewModel::updateRequireAuth, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)) },
            )
            HorizontalDivider(color = BorderColor)
            SettingRow(
                icon = Icons.Default.Person,
                iconTint = TextDim,
                label = "Username",
                subtitle = config.username,
                onClick = { showUsernameDialog = true },
                trailing = { Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            )
            HorizontalDivider(color = BorderColor)
            SettingRow(
                icon = Icons.Default.Key,
                iconTint = Warning,
                label = "Password",
                subtitle = "••••••••",
                onClick = { showPasswordDialog = true },
                trailing = { Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            )
            HorizontalDivider(color = BorderColor)
            SettingRow(
                icon = Icons.Default.PersonOff,
                iconTint = Warning,
                label = "Allow Anonymous Login",
                subtitle = "No password required",
                trailing = { Switch(checked = config.allowAnonymous, onCheckedChange = viewModel::updateAllowAnonymous, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Warning)) },
            )
        }

        // Server section
        SectionLabel("Server")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SettingRow(
                icon = Icons.Default.Router,
                iconTint = Primary,
                label = "FTP Port",
                subtitle = "Port ${config.port}",
                onClick = { showPortDialog = true },
                trailing = { Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            )
            HorizontalDivider(color = BorderColor)
            SettingRow(
                icon = Icons.Default.Wifi,
                iconTint = Success,
                label = "Auto-Start on WiFi",
                subtitle = "Launch server when WiFi connects",
                trailing = { Switch(checked = config.autoStartOnWifi, onCheckedChange = viewModel::updateAutoStart, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Success)) },
            )
            HorizontalDivider(color = BorderColor)
            SettingRow(
                icon = Icons.Default.Notifications,
                iconTint = Primary,
                label = "Background Service",
                subtitle = "Keep server running when app is closed",
                trailing = { Switch(checked = config.runInBackground, onCheckedChange = viewModel::updateRunBackground, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)) },
            )
        }

        // About section
        SectionLabel("About")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SettingRow(icon = Icons.Default.Info, iconTint = TextMuted, label = "Version", subtitle = "WiFi FTP Server 1.0.0")
            HorizontalDivider(color = BorderColor)
            SettingRow(icon = Icons.Default.Shield, iconTint = Success, label = "Protocol", subtitle = "FTP RFC-959 via Apache FTPServer 1.2.0")
            HorizontalDivider(color = BorderColor)
            SettingRow(icon = Icons.Default.Code, iconTint = TextMuted, label = "Framework", subtitle = "Kotlin + Jetpack Compose + Hilt")
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────
    if (showUsernameDialog) {
        var input by remember { mutableStateOf(config.username) }
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Change Username") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Username") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary),
                )
            },
            confirmButton = {
                TextButton(onClick = { if (input.isNotBlank()) { viewModel.updateUsername(input); showUsernameDialog = false } }) {
                    Text("Save", color = Primary)
                }
            },
            dismissButton = { TextButton(onClick = { showUsernameDialog = false }) { Text("Cancel") } },
            containerColor = BgCard,
        )
    }
    if (showPasswordDialog) {
        var input by remember { mutableStateOf("") }
        var visible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextMuted)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary),
                )
            },
            confirmButton = {
                TextButton(onClick = { if (input.length >= 4) { viewModel.updatePassword(input); showPasswordDialog = false } else viewModel.showSnackbar("Min 4 characters") }) {
                    Text("Save", color = Primary)
                }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") } },
            containerColor = BgCard,
        )
    }
    if (showPortDialog) {
        var input by remember { mutableStateOf(config.port.toString()) }
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = { Text("Change Port") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("Port (1025–65534)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePort(input.toIntOrNull() ?: config.port)
                    showPortDialog = false
                }) { Text("Save", color = Primary) }
            },
            dismissButton = { TextButton(onClick = { showPortDialog = false }) { Text("Cancel") } },
            containerColor = BgCard,
        )
    }
}

// ─── Shared helper ───────────────────────────────────────────────────────────
private fun formatBytes(bytes: Long): String = when {
    bytes > 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes > 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes > 1_024L         -> "%.0f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}
