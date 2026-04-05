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
import android.util.Log

@Composable
fun ProductManagerScreen(
    viewModel: ProductViewModel,
    sharePointService: SharePointService?,
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

    // ✅ Stato per il popup modifica/elimina
    var prodottoSelezionato by remember { mutableStateOf<Prodotto?>(null) }
    var nuovoNome by remember { mutableStateOf("") }

    // ✅ Popup modifica/elimina ingrediente
    if (prodottoSelezionato != null) {
        AlertDialog(
            onDismissRequest = {
                prodottoSelezionato = null
                nuovoNome = ""
            },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    "Ingrediente: ${prodottoSelezionato!!.nome}",
                    color = OroCiliegio,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Vuoi rinominare o eliminare questo ingrediente?",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nuovoNome,
                        onValueChange = { nuovoNome = it },
                        label = { Text("Nuovo nome", color = OroCiliegio) },
                        placeholder = { Text(prodottoSelezionato!!.nome, color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OroCiliegio,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                // ✅ Bottone RINOMINA
                Button(
                    onClick = {
                        if (nuovoNome.isNotEmpty()) {
                            viewModel.rinominaProdotto(prodottoSelezionato!!, nuovoNome, context)
                        }
                        prodottoSelezionato = null
                        nuovoNome = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OroCiliegio),
                    enabled = nuovoNome.isNotEmpty()
                ) {
                    Text("Rinomina", color = Color.Black)
                }
            },
            dismissButton = {
                Row {
                    // ✅ Bottone ELIMINA
                    Button(
                        onClick = {
                            viewModel.eliminaProdotto(prodottoSelezionato!!, context)
                            prodottoSelezionato = null
                            nuovoNome = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Elimina", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // ✅ Bottone ANNULLA
                    OutlinedButton(
                        onClick = {
                            prodottoSelezionato = null
                            nuovoNome = ""
                        }
                    ) {
                        Text("Annulla", color = Color.Gray)
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Button(
            onClick = {
                // ✅ Salva su OneDrive prima di tornare alla home
                if (sharePointService != null) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val json = viewModel.esportaDatiInJson()
                            sharePointService.uploadJsonToOneDrive(json, "backup_ciliegio.json")
                        } catch (e: Exception) {
                            Log.e("OneDrive", "Errore salvataggio: ${e.message}")
                        }
                        launch(Dispatchers.Main) { onBack() }
                    }
                } else {
                    onBack()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
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
                    title = "BASE",
                    list = prodotti.filter {
                        it.categoria == categorie[tabSelezionata] && it.sottocategoria == "Base"
                    },
                    onItemClick = { prodotto ->
                        prodottoSelezionato = prodotto
                        nuovoNome = ""
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).padding(4.dp)) {
                IngredientList(
                    title = "EXTRA",
                    list = prodotti.filter {
                        it.categoria == categorie[tabSelezionata] && it.sottocategoria == "Extra"
                    },
                    onItemClick = { prodotto ->
                        prodottoSelezionato = prodotto
                        nuovoNome = ""
                    }
                )
            }
        }

        if (uploadStatus.isNotEmpty()) {
            Text(uploadStatus, color = Color.Green, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }

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

// ✅ IngredientList aggiornata: ora passa il prodotto intero al click
@Composable
fun IngredientList(
    title: String,
    list: List<Prodotto>,
    onItemClick: (Prodotto) -> Unit
) {
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
                        .clickable { onItemClick(prodotto) }
                )
            }
        }
    }
}