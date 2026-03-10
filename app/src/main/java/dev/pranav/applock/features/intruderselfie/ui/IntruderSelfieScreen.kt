package dev.pranav.applock.features.intruderselfie.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderSelfieScreen(navController: NavController) {
    val context = LocalContext.current
    var selfies by remember { mutableStateOf(listOf<File>()) }
    var selfieToDelete by remember { mutableStateOf<File?>(null) }

    fun loadSelfies() {
        val selfieDir = File(context.filesDir, "intruder_selfies")
        if (selfieDir.exists()) {
            selfies = selfieDir.listFiles()?.filter { it.extension == "jpg" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        loadSelfies()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intruder Selfies") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (selfies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No intruder selfies captured yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(selfies) { selfie ->
                    SelfieCard(
                        file = selfie,
                        onDelete = { selfieToDelete = selfie }
                    )
                }
            }
        }
    }

    if (selfieToDelete != null) {
        AlertDialog(
            onDismissRequest = { selfieToDelete = null },
            title = { Text("Delete Selfie") },
            text = { Text("Are you sure you want to delete this intruder selfie?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selfieToDelete?.delete()
                        selfieToDelete = null
                        loadSelfies()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selfieToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SelfieCard(file: File, onDelete: () -> Unit) {
    val bitmap = remember(file) {
        BitmapFactory.decodeFile(file.absolutePath)
    }

    val timestamp = remember(file) {
        val date = Date(file.lastModified())
        val sdf = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
        sdf.format(date)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Intruder Selfie",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Intruder Detected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
