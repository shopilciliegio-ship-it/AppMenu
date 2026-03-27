package com.example.menciliegio

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray

@Composable
fun MenuBuilderScreen(
    data: String,
    servizio: String,
    nomeMenu: String,
    productViewModel: ProductViewModel,
    builderViewModel: MenuBuilderViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categorie = listOf("Antipasti", "Primi", "Secondi", "Contorni", "Dolci")
    var tabIndice by remember { mutableIntStateOf(0) }
    val catCorrente = categorie[tabIndice]

    // Stati per dialoghi
    var mostraDialogoAggiunta by remember { mutableStateOf(false) }
    var sottoCategoriaTarget by remember { mutableStateOf("Base") }
    var nuovoIngredienteNome by remember { mutableStateOf("") }
    var piattoDaModificare by remember { mutableStateOf<Pair<String, PiattoMenu>?>(null) }
    var nuovoNomeTesto by remember { mutableStateOf("") }

    val tuttiIProdotti by productViewModel.allProducts.collectAsState(initial = emptyList())
    val baseList = tuttiIProdotti.filter { it.categoria == catCorrente && it.sottocategoria == "Base" }
    val extraList = tuttiIProdotti.filter { it.categoria == catCorrente && it.sottocategoria == "Extra" }

    // --- LOGICA SALVATAGGIO (Invariata) ---
    val launcherJsonEN = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val piattiMappa = builderViewModel.menuFinale.mapValues { entry -> entry.value.map { it.nomeVisualizzato } }
                    val piattiEN = MenuGenerator.translateMenuWithRules(piattiMappa, productViewModel.dao)
                    val root = JSONObject().apply {
                        put("date", data)
                        put("service", "Lunch")
                        put("price", builderViewModel.prezzoPersona.value)
                        put("items", JSONObject().apply {
                            piattiEN.forEach { (cat, lista) -> put(cat, JSONArray().apply { lista.forEach { put(it) } }) }
                        })
                    }
                    context.contentResolver.openOutputStream(it)?.use { stream -> stream.write(root.toString(2).toByteArray()) }
                    Toast.makeText(context, "Bozza Inglese JSON salvata!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { Toast.makeText(context, "Errore JSON: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    val launcherIT = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        uri?.let {
            scope.launch {
                val piattiMappa = builderViewModel.menuFinale.mapValues { entry -> entry.value.map { it.nomeVisualizzato } }
                val bitmapIT = MenuGenerator.generateMenuJpg(context, piattiMappa, "$servizio $data", builderViewModel.prezzoPersona.value, false)
                context.contentResolver.openOutputStream(it)?.use { stream -> bitmapIT.compress(Bitmap.CompressFormat.JPEG, 95, stream) }
                builderViewModel.salvaMenuNelDB(data, servizio, nomeMenu)
                Toast.makeText(context, "Italiano salvato! Ora salva il JSON EN", Toast.LENGTH_LONG).show()
                launcherJsonEN.launch("bozza_menu_${data}_EN.json")
            }
        }
    }

    // --- UI DIALOGS (Invariate) ---
    if (mostraDialogoAggiunta) {
        AlertDialog(
            onDismissRequest = { mostraDialogoAggiunta = false },
            title = { Text("Aggiungi a $catCorrente") },
            text = { OutlinedTextField(value = nuovoIngredienteNome, onValueChange = { nuovoIngredienteNome = it }, label = { Text("Nome") }) },
            confirmButton = { Button(onClick = { productViewModel.aggiungiIngredienteVeloce(nuovoIngredienteNome, catCorrente, sottoCategoriaTarget); nuovoIngredienteNome = ""; mostraDialogoAggiunta = false }) { Text("Salva") } },
            dismissButton = { TextButton(onClick = { mostraDialogoAggiunta = false }) { Text("Annulla") } }
        )
    }

    if (piattoDaModificare != null) {
        AlertDialog(
            onDismissRequest = { piattoDaModificare = null },
            title = { Text("Modifica/Elimina Piatto") },
            text = { OutlinedTextField(value = nuovoNomeTesto, onValueChange = { nuovoNomeTesto = it }, label = { Text("Nome visualizzato") }) },
            confirmButton = { Button(onClick = { if (nuovoNomeTesto.isNotEmpty()) builderViewModel.rinominaESalvaRegola(piattoDaModificare!!.first, piattoDaModificare!!.second, nuovoNomeTesto); piattoDaModificare = null }) { Text("Salva") } },
            dismissButton = { Button(onClick = { builderViewModel.rimuoviPiatto(piattoDaModificare!!.first, piattoDaModificare!!.second); piattoDaModificare = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Elimina") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Header
        Column(modifier = Modifier.fillMaxWidth().background(Color.DarkGray).padding(8.dp)) {
            Text("$servizio - $data", color = OroCiliegio, fontSize = 12.sp)
            Text(nomeMenu.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // --- TABS OTTIMIZZATE ---
        ScrollableTabRow(
            selectedTabIndex = tabIndice,
            containerColor = Color(0xFF1A1A1A),
            contentColor = OroCiliegio,
            edgePadding = 0.dp,
            divider = {},
            indicator = {} // Rimuoviamo la linea sotto perché coloriamo tutto il box
        ) {
            categorie.forEachIndexed { index, title ->
                val selezionata = tabIndice == index
                Tab(
                    selected = selezionata,
                    onClick = { tabIndice = index },
                    modifier = Modifier
                        .height(45.dp)
                        .background(if (selezionata) OroCiliegio else Color.Transparent),
                    text = {
                        Text(
                            text = title,
                            color = if (selezionata) Color.Black else Color.White,
                            fontWeight = if (selezionata) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Selezione Ingredienti
        Row(modifier = Modifier.weight(0.8f).padding(8.dp)) {
            Column(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).padding(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("BASE", color = OroCiliegio, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { sottoCategoriaTarget = "Base"; mostraDialogoAggiunta = true }) { Icon(Icons.Default.Add, null, tint = OroCiliegio, modifier = Modifier.size(20.dp)) }
                }
                LazyColumn { items(baseList) { prod -> IngredientRow(prod.nome, builderViewModel.selectedBase) } }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).padding(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("EXTRA", color = OroCiliegio, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { sottoCategoriaTarget = "Extra"; mostraDialogoAggiunta = true }) { Icon(Icons.Default.Add, null, tint = OroCiliegio, modifier = Modifier.size(20.dp)) }
                }
                LazyColumn { items(extraList) { prod -> IngredientRow(prod.nome, builderViewModel.selectedExtra) } }
            }
        }

        // --- FOOTER CON ANTEPRIMA A 2 COLONNE ---
        Column(modifier = Modifier.fillMaxWidth().background(Color.DarkGray).padding(horizontal = 12.dp, vertical = 8.dp)) {

            // Riga Prezzo e Bottone Genera
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Prezzo €", color = OroCiliegio, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                // --- CAMPO PREZZO RIMPICCIOLITO ---
                TextField(
                    value = builderViewModel.prezzoPersona.value,
                    onValueChange = { builderViewModel.prezzoPersona.value = it },
                    modifier = Modifier.width(70.dp).height(48.dp),
                    textStyle = TextStyle(fontSize = 14.sp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.LightGray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { builderViewModel.generaPiatto(catCorrente) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("AGGIUNGI ${catCorrente.uppercase()}", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("📜 ANTEPRIMA MENÙ (Clicca per modificare)", color = OroCiliegio, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            // --- ANTEPRIMA A DUE COLONNE ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(180.dp).fillMaxWidth(), // Aumentato spazio anteprima
                contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categorie.forEach { cat ->
                    val piatti = builderViewModel.menuFinale[cat]
                    if (!piatti.isNullOrEmpty()) {
                        // Header categoria nella griglia (occupa 2 colonne)
                        item(span = { GridItemSpan(2) }) {
                            Text(cat.uppercase(), color = OroCiliegio, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        // Lista piatti (2 per riga)
                        items(piatti) { piatto ->
                            Surface(
                                color = Color(0xFF2C2C2C),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.clickable {
                                    piattoDaModificare = cat to piatto
                                    nuovoNomeTesto = piatto.nomeVisualizzato
                                }
                            ) {
                                Text(
                                    text = piatto.nomeVisualizzato,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottoni finali
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), modifier = Modifier.height(40.dp)) { Text("ESCI") }
                Button(
                    onClick = { launcherIT.launch("menu_${data}_IT.jpg") },
                    colors = ButtonDefaults.buttonColors(containerColor = OroCiliegio),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("PRODUCI JPG", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun IngredientRow(nome: String, selectedList: MutableList<String>) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(32.dp)) {
        Checkbox(
            checked = selectedList.contains(nome),
            onCheckedChange = { if (it) selectedList.add(nome) else selectedList.remove(nome) },
            colors = CheckboxDefaults.colors(checkedColor = OroCiliegio)
        )
        Text(nome, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}