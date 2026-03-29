package com.example.menciliegio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, productViewModel: ProductViewModel) { // AGGIUNTO productViewModel qui
    val context = LocalContext.current

    // --- SINCRONIZZAZIONE AUTOMATICA ALL'AVVIO ---
    LaunchedEffect(Unit) {
        val folderUri = CloudStorageHelper.getFolderUri(context)
        if (folderUri != null) {
            val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, folderUri)
            val masterFile = root?.findFile("database_prodotti_master.json")

            if (masterFile != null) {
                val jsonContent = CloudStorageHelper.readFileContent(context, masterFile.uri)
                if (jsonContent != null) {
                    // Questa funzione ora verrà riconosciuta perché abbiamo passato il ViewModel
                    productViewModel.importaDatiDaJson(jsonContent)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- LOGO ---
        Image(
            painter = painterResource(id = R.drawable.ciliegio_trasparente),
            contentDescription = "Logo Al Ciliegio",
            modifier = Modifier
                .size(180.dp)
                .padding(bottom = 16.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Menù Agriturismo",
            color = OroCiliegio,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(40.dp))

        MenuButton("Gestione Prodotti", Color(0xFF008000)) { onNavigate("prodotti") }
        MenuButton("Compila Menù", OroCiliegio, Color.Black) { onNavigate("compila_header") }
        MenuButton("Archivio Menù", Color(0xFF0000FF)) { onNavigate("archivio") }

        Spacer(modifier = Modifier.height(20.dp))

        // --- TASTO ESCI FUNZIONANTE ---
        MenuButton("Esci", Color(0xFFFF0000)) {
            (context as? android.app.Activity)?.finish()
        }
    }
}

@Composable
fun MenuButton(text: String, color: Color, textColor: Color = Color.White, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}