package com.wififtp.server.ui.screens

import android.os.Environment
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.wififtp.server.ui.components.EmptyState
import com.wififtp.server.ui.components.SectionLabel
import com.wififtp.server.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val sizeBytes: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified(),
)

private fun formatSize(bytes: Long): String = when {
    bytes > 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes > 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes > 1_024L         -> "%.0f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}

private fun fileIcon(name: String, isDir: Boolean): Pair<ImageVector, Color> {
    if (isDir) return Icons.Default.Folder to Warning
    return when (name.substringAfterLast('.').lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "heic" -> Icons.Default.Image to Color(0xFF818CF8)
        "mp4", "mov", "avi", "mkv", "3gp"           -> Icons.Default.VideoFile to Color(0xFFFB7185)
        "mp3", "m4a", "wav", "flac", "aac"          -> Icons.Default.AudioFile to Color(0xFFF472B6)
        "pdf"                                        -> Icons.Default.PictureAsPdf to Error
        "zip", "rar", "tar", "gz", "7z"             -> Icons.Default.FolderZip to TextMuted
        "doc", "docx"                                -> Icons.Default.Description to Primary
        "xls", "xlsx"                                -> Icons.Default.TableChart to Success
        "txt", "md", "log"                          -> Icons.Default.TextSnippet to TextDim
        "apk"                                        -> Icons.Default.Android to Success
        else                                         -> Icons.Default.InsertDriveFile to TextMuted
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen() {
    val rootDir = remember {
        Environment.getExternalStorageDirectory()
            ?: File(Environment.getExternalStorageState())
    }
    var currentDir by remember { mutableStateOf(rootDir) }
    var pathStack by remember { mutableStateOf(listOf(rootDir)) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("name") }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    val files by remember(currentDir, searchQuery, sortBy) {
        derivedStateOf {
            val rawList = currentDir.listFiles()?.map { FileItem(it) } ?: emptyList()
            val filtered = if (searchQuery.isBlank()) rawList
            else rawList.filter { it.name.contains(searchQuery, ignoreCase = true) }
            val sorted = when (sortBy) {
                "size" -> filtered.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.sizeBytes })
                "date" -> filtered.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.lastModified })
                else   -> filtered.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
            }
            sorted
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgDark)) {
        // Top bar
        Column(modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Files", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("name" to "Name", "size" to "Size", "date" to "Date").forEach { (key, label) ->
                        FilterChip(
                            selected = sortBy == key,
                            onClick = { sortBy = key },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.2f),
                                selectedLabelColor = Primary,
                                containerColor = Color.Transparent,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = sortBy == key,
                                selectedBorderColor = Primary.copy(alpha = 0.4f),
                                borderColor = BorderColor,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search files…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {{
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }} else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Primary,
                    focusedContainerColor = BgCard2,
                    unfocusedContainerColor = BgCard2,
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            // Breadcrumb
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pathStack.forEachIndexed { idx, dir ->
                    if (idx > 0) Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    val isLast = idx == pathStack.lastIndex
                    Text(
                        text = if (idx == 0) "Storage" else dir.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLast) TextPrimary else TextMuted,
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = if (!isLast) Modifier.clickable {
                            pathStack = pathStack.take(idx + 1)
                            currentDir = dir
                        } else Modifier,
                    )
                }
            }
        }

        // Back button
        if (pathStack.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        pathStack = pathStack.dropLast(1)
                        currentDir = pathStack.last()
                    }
                    .background(BgCard2)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Primary, modifier = Modifier.size(18.dp))
                Text("Back", style = MaterialTheme.typography.bodyMedium, color = Primary)
            }
            HorizontalDivider(color = BorderColor)
        }

        // File List
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.FolderOpen,
                    title = if (searchQuery.isNotEmpty()) "No results" else "Empty folder",
                    subtitle = if (searchQuery.isNotEmpty()) "No files match your search" else "This folder contains no files",
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(files, key = { it.file.absolutePath }) { item ->
                    val (icon, tint) = fileIcon(item.name, item.isDirectory)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (item.isDirectory) {
                                    currentDir = item.file
                                    pathStack = pathStack + item.file
                                    searchQuery = ""
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (item.isDirectory) "Folder · ${dateFormat.format(Date(item.lastModified))}"
                                else "${formatSize(item.sizeBytes)} · ${dateFormat.format(Date(item.lastModified))}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (item.isDirectory) {
                            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}
