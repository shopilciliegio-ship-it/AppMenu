package com.example.menciliegio

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProductManagerScreen(
    viewModel: ProductViewModel,
    sharePointService: SharePointService?,   // ← AGGIUNTO (nullable: se non loggato)
    onBack: () -> Unit
) {
    val prodotti by viewModel.allProducts.collectAsState(initial = emptyList())
    val categorie = listOf("Antipasti", "Primi", "Secondi", "Contorni", "Dolci")
    var tabSelezionata by remember { mutableIntStateOf(0) }
    val nome by viewModel.nomeInserito.collectAsState()
    val subSelezionata by viewModel.sottocategoriaSelezionata.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploadStatus by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
            Text("← HOME")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("GESTIONE PRODOTTI", color = OroCiliegio, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            OutlinedTextField(
                value = nome,
                onValueChange = { viewModel.nomeInserito.value = it },
                label = { Text("Nuovo Ingrediente", color = OroCiliegio) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.aggiungiProdotto(context) }) {
                Text("Aggiungi")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = subSelezionata == "Base",
                onClick = { viewModel.sottocategoriaSelezionata.value = "Base" },
                colors = RadioButtonDefaults.colors(selectedColor = OroCiliegio)
            )
            Text("Base", color = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = subSelezionata == "Extra",
                onClick = { viewModel.sottocategoriaSelezionata.value = "Extra" },
                colors = RadioButtonDefaults.colors(selectedColor = OroCiliegio)
            )
            Text("Extra", color = Color.White)
        }

        ScrollableTabRow(
            selectedTabIndex = tabSelezionata,
            containerColor = Color.Black,
            contentColor = OroCiliegio,
            edgePadding = 0.dp
        ) {
            categorie.forEachIndexed { index, title ->
                Tab(
                    selected = tabSelezionata == index,
                    onClick = {
                        tabSelezionata = index
                        viewModel.categoriaSelezionata.value = title
                    },
                    text = { Text(title) }
                )
            }
        }

        Row(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Box(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).padding(4.dp)) {
                IngredientList(
                    "BASE",
                    prodotti.filter { it.categoria == categorie[tabSelezionata] && it.sottocategoria == "Base" },
                    viewModel,
                    context
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).padding(4.dp)) {
                IngredientList(
                    "EXTRA",
                    prodotti.filter { it.categoria == categorie[tabSelezionata] && it.sottocategoria == "Extra" },
                    viewModel,
                    context
                )
            }
        }

        // Messaggio di stato upload
        if (uploadStatus.isNotEmpty()) {
            Text(uploadStatus, color = Color.Green, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ✅ Solo il bottone SALVA SU ONEDRIVE (rimosso IMPORTA JSON)
        Button(
            onClick = {
                if (sharePointService == null) {
                    uploadStatus = "⚠️ Effettua prima il Login SharePoint!"
                } else {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val json = viewModel.esportaDatiInJson()
                            sharePointService.uploadJsonToOneDrive(json, "backup_ciliegio.json")
                            launch(Dispatchers.Main) {
                                uploadStatus = "✅ Backup salvato su OneDrive!"
                            }
                        } catch (e: Exception) {
                            launch(Dispatchers.Main) {
                                uploadStatus = "❌ Errore: ${e.message}"
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF000080))
        ) {
            Text("💾 SALVA SU ONEDRIVE")
        }
    }
}

@Composable
fun IngredientList(title: String, list: List<Prodotto>, viewModel: ProductViewModel, context: Context) {
    Column {
        Text(
            text = title,
            color = OroCiliegio,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        HorizontalDivider(color = OroCiliegio, thickness = 1.dp)
        LazyColumn {
            items(list) { prodotto ->
                Text(
                    text = prodotto.nome,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { viewModel.eliminaProdotto(prodotto, context) }
                )
            }
        }
    }
}