package com.example.menciliegio

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.launch

@Composable
fun ProductManagerScreen(viewModel: ProductViewModel, onBack: () -> Unit) {
    val prodotti by viewModel.allProducts.collectAsState(initial = emptyList())
    val categorie = listOf("Antipasti", "Primi", "Secondi", "Contorni", "Dolci")
    var tabSelezionata by remember { mutableIntStateOf(0) }

    val nome by viewModel.nomeInserito.collectAsState()
    val subSelezionata by viewModel.sottocategoriaSelezionata.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    val json = viewModel.esportaDatiInJson()
                    stream.write(json.toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                viewModel.importaDatiDaJson(reader.readText())
            }
        }
    }

    // Nota: SfondoNero deve essere definito nei tuoi colori, se dà errore usa Color.Black
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
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // CORRETTO: Usiamo 'viewModel' (non productViewModel) e passiamo 'context'
            Button(onClick = {
                viewModel.aggiungiProdotto(context)
            }) {
                Text("Aggiungi")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = subSelezionata == "Base", onClick = { viewModel.sottocategoriaSelezionata.value = "Base" }, colors = RadioButtonDefaults.colors(selectedColor = OroCiliegio))
            Text("Base", color = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = subSelezionata == "Extra", onClick = { viewModel.sottocategoriaSelezionata.value = "Extra" }, colors = RadioButtonDefaults.colors(selectedColor = OroCiliegio))
            Text("Extra", color = Color.White)
        }

        ScrollableTabRow(selectedTabIndex = tabSelezionata, containerColor = Color.Black, contentColor = OroCiliegio, edgePadding = 0.dp) {
            categorie.forEachIndexed { index, title ->
                Tab(selected = tabSelezionata == index, onClick = {
                    tabSelezionata = index
                    viewModel.categoriaSelezionata.value = title
                }, text = { Text(title) })
            }
        }

        Row(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Box(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).padding(4.dp)) {
                // Passiamo anche il context alla lista per l'eliminazione
                IngredientList("BASE", prodotti.filter { it.categoria == categorie[tabSelezionata] && it.sottocategoria == "Base" }, viewModel, context)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).padding(4.dp)) {
                // Passiamo anche il context alla lista per l'eliminazione
                IngredientList("EXTRA", prodotti.filter { it.categoria == categorie[tabSelezionata] && it.sottocategoria == "Extra" }, viewModel, context)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800080))) {
                Text("IMPORTA JSON")
            }
            Button(onClick = { exportLauncher.launch("backup_ciliegio.json") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF000080))) {
                Text("ESPORTA JSON")
            }
        }
    }
}

@Composable
fun IngredientList(title: String, list: List<Prodotto>, viewModel: ProductViewModel, context: Context) {
    Column {
        Text(text = title, color = OroCiliegio, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        HorizontalDivider(color = OroCiliegio, thickness = 1.dp)
        LazyColumn {
            items(list) { prodotto ->
                Text(
                    text = prodotto.nome,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            // CORRETTO: Passiamo sia il prodotto che il context
                            viewModel.eliminaProdotto(prodotto, context)
                        }
                )
            }
        }
    }
}