package com.example.menciliegio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onFileSelected: (String, String, String, String, String) -> Unit, // ritorna jsonContent, data, servizio, nome, lingua
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val files = remember { CloudStorageHelper.listJsonFiles(context) }
    var selectedFile by remember { mutableStateOf<DocumentFile?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archivio Menù", color = OroCiliegio) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = OroCiliegio)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nessun file trovato", color = Color.Gray)
            }
        }

        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(files) { file ->
                ListItem(
                    modifier = Modifier.clickable {
                        selectedFile = file
                        showDialog = true
                    },
                    headlineContent = { Text(file.name ?: "Senza nome", color = Color.White) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }

        if (showDialog && selectedFile != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Carica Menù") },
                text = { Text("Quale lingua vuoi caricare per la modifica?") },
                confirmButton = {
                    Button(onClick = {
                        val content = CloudStorageHelper.readFileContent(context, selectedFile!!.uri)
                        if (content != null) onFileSelected(content, "", "", "", "IT")
                        showDialog = false
                    }) { Text("ITALIANO") }
                },
                dismissButton = {
                    Button(onClick = {
                        val content = CloudStorageHelper.readFileContent(context, selectedFile!!.uri)
                        if (content != null) onFileSelected(content, "", "", "", "EN")
                        showDialog = false
                    }) { Text("INGLESE") }
                }
            )
        }
    }
}