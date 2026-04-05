package com.example.menciliegio

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.text.BasicTextField

@Composable
fun MenuBuilderScreen(
    data: String,
    servizio: String,
    nomeMenu: String,
    productViewModel: ProductViewModel,
    builderViewModel: MenuBuilderViewModel,
    sharePointService: SharePointService?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categorie = listOf("Antipasti", "Primi", "Secondi", "Contorni", "Dolci")
    var tabIndice by remember { mutableIntStateOf(0) }
    val catCorrente = categorie[tabIndice]

    // --- STATI PER PREVIEW ---
    var showPreview by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }

    // Stati per dialoghi
    var mostraDialogoAggiunta by remember { mutableStateOf(false) }
    var sottoCategoriaTarget by remember { mutableStateOf("Base") }
    var nuovoIngredienteNome by remember { mutableStateOf("") }
    var piattoDaModificare by remember { mutableStateOf<Pair<String, PiattoMenu>?>(null) }
    var nuovoNomeTesto by remember { mutableStateOf("") }

    val tuttiIProdotti by productViewModel.allProducts.collectAsState(initial = emptyList())
    val baseList = tuttiIProdotti.filter { it.categoria == catCorrente && it.sottocategoria == "Base" }
    val extraList = tuttiIProdotti.filter { it.categoria == catCorrente && it.sottocategoria == "Extra" }
    var prezzoLocale by remember { mutableStateOf(builderViewModel.prezzoPersona.value) }

    // Funzione interna per gestire il salvataggio automatico
    fun eseguiSalvataggioCloud() {
        scope.launch {
            // 1. Calcolo del nome base (Prefisso-Giorno-Servizio-Data)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ITALIAN)
            val dateObj = sdf.parse(data) ?: java.util.Date()
            val giornoSett = java.text.SimpleDateFormat("EEEE", java.util.Locale.ITALIAN).format(dateObj).lowercase()
            val prefisso = when (giornoSett) {
                "lunedì" -> if (servizio.lowercase().contains("pranzo")) "02" else "03"
                "martedì" -> if (servizio.lowercase().contains("pranzo")) "04" else "05"
                "mercoledì" -> if (servizio.lowercase().contains("pranzo")) "06" else "07"
                "giovedì" -> if (servizio.lowercase().contains("pranzo")) "08" else "09"
                "venerdì" -> if (servizio.lowercase().contains("pranzo")) "10" else "11"
                "sabato" -> if (servizio.lowercase().contains("pranzo")) "12" else "13"
                "domenica" -> if (servizio.lowercase().contains("pranzo")) "14" else "15"
                else -> "00"
            }

            val nomeExtraSuf = if (nomeMenu.isEmpty() || nomeMenu.equals("Standard", ignoreCase = true)) "" else "-$nomeMenu"

            // --- GESTIONE LINGUA NEL NOME ---
            val suffixLingua = if (builderViewModel.linguaCorrente.value == "EN") "-EN" else ""
            val nomeFileBase = "$prefisso-$giornoSett-$servizio-$data$nomeExtraSuf$suffixLingua"

            // 2. Preparazione dati (Entrambi sempre presenti nel JSON)
            val piattiAttuali = builderViewModel.menuFinale.mapValues { entry -> entry.value.map { it.nomeVisualizzato } }

            // Se stiamo modificando un EN, i piattiAttuali sono già in inglese.
            // Per il JSON unico però ci servono entrambi.
            // (Nota: se hai caricato un EN, i piattiIT nel JSON saranno sovrascritti con quelli EN
            // a meno di non avere una logica di traduzione inversa, ma seguiamo la tua richiesta)

            // Il menù viene sempre compilato in IT. La traduzione avviene solo al salvataggio:
            val piattiIT = builderViewModel.menuFinale.mapValues { it.value.map { p -> p.nomeVisualizzato } }
            val piattiEN = MenuGenerator.translateMenuWithRules(piattiIT, productViewModel.dao)
            // Poi salva entrambi nel JSON

            // 3. Generazione JPG (nella lingua corretta)
            val bitmapDaSalvare = MenuGenerator.generateMenuJpg(
                context,
                piattiAttuali, // Usa i piatti che vedi a schermo
                "$servizio $data",
                builderViewModel.prezzoPersona.value,
                false
            )
            val streamJpg = ByteArrayOutputStream()
            bitmapDaSalvare.compress(Bitmap.CompressFormat.JPEG, 95, streamJpg)

            val jpgSalvato = CloudStorageHelper.writeFileToCloud(context, "$nomeFileBase.jpg", "image/jpeg", streamJpg.toByteArray())

            // 4. Generazione JSON (Usiamo lo stesso nome base del JPG)
            val rootJson = JSONObject().apply {
                put("date", data)
                put("service", servizio)
                put("extra_name", nomeMenu)
                put("price", builderViewModel.prezzoPersona.value)
                put("menu_it", JSONObject().apply { piattiIT.forEach { (cat, lista) -> put(cat, JSONArray().apply { lista.forEach { put(it) } }) } })
                put("menu_en", JSONObject().apply { piattiEN.forEach { (cat, lista) -> put(cat, JSONArray().apply { lista.forEach { put(it) } }) } })
            }

            val jsonSalvato = CloudStorageHelper.writeFileToCloud(context, "$nomeFileBase.json", "application/json", rootJson.toString(2).toByteArray())

            if (jpgSalvato && jsonSalvato) {
                Toast.makeText(context, "File sovrascritto: $nomeFileBase", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- UI PRINCIPALE ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Header
            Column(modifier = Modifier.fillMaxWidth().background(Color.DarkGray).padding(8.dp)) {
                Text("$servizio - $data", color = OroCiliegio, fontSize = 12.sp)
                Text(nomeMenu.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = tabIndice,
                containerColor = Color(0xFF1A1A1A),
                contentColor = OroCiliegio,
                edgePadding = 0.dp,
                divider = {},
                indicator = {}
            ) {
                categorie.forEachIndexed { index, title ->
                    val selezionata = tabIndice == index
                    Tab(
                        selected = selezionata,
                        onClick = { tabIndice = index },
                        modifier = Modifier.height(45.dp).background(if (selezionata) OroCiliegio else Color.Transparent),
                        text = { Text(text = title, color = if (selezionata) Color.Black else Color.White, fontWeight = if (selezionata) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
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

            // Footer
            Column(modifier = Modifier.fillMaxWidth().background(Color.DarkGray).padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Riga Prezzo e Aggiungi
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Prezzo €", color = OroCiliegio, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = prezzoLocale,
                        onValueChange = {
                            prezzoLocale = it
                            builderViewModel.prezzoPersona.value = it  // aggiorna anche il ViewModel
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier
                            .width(70.dp)
                            .height(48.dp)
                            .background(Color.White)
                            .padding(horizontal = 8.dp, vertical = 14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { builderViewModel.generaPiatto(catCorrente) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000)),
                        shape = RoundedCornerShape(4.dp)
                    ) { Text("AGGIUNGI ${catCorrente.uppercase()}", fontSize = 12.sp) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Anteprima Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(180.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categorie.forEach { cat ->
                        val piatti = builderViewModel.menuFinale[cat]
                        if (!piatti.isNullOrEmpty()) {
                            item(span = { GridItemSpan(2) }) { Text(cat.uppercase(), color = OroCiliegio, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            items(piatti) { piatto ->
                                Surface(
                                    color = Color(0xFF2C2C2C),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.clickable { piattoDaModificare = cat to piatto; nuovoNomeTesto = piatto.nomeVisualizzato }
                                ) { Text(piatto.nomeVisualizzato, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }

                // --- BOTTONI FINALI (3 BOTTONI) ---
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            // ✅ Salva JSON ingredienti su OneDrive prima di uscire
                            if (sharePointService != null) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val json = productViewModel.esportaDatiInJson()
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("ESCI", fontSize = 11.sp)
                    }

                    // BOTTONE PREVIEW CON CARICAMENTO E THREAD SEPARATO
                    Button(
                        enabled = !isGeneratingPreview,
                        onClick = {
                            isGeneratingPreview = true
                            scope.launch(Dispatchers.Default) {
                                try {
                                    val piattiIT = builderViewModel.menuFinale.mapValues { entry -> entry.value.map { it.nomeVisualizzato } }
                                    // Passiamo TRUE per avere lo sfondo bianco e caricamento leggero
                                    val bitmap = MenuGenerator.generateMenuJpg(context, piattiIT, "$servizio $data", builderViewModel.prezzoPersona.value, true)

                                    withContext(Dispatchers.Main) {
                                        previewBitmap = bitmap
                                        showPreview = true
                                        isGeneratingPreview = false
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isGeneratingPreview = false
                                        Toast.makeText(context, "Errore Anteprima", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        if (isGeneratingPreview) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("PREVIEW", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { eseguiSalvataggioCloud() },
                        colors = ButtonDefaults.buttonColors(containerColor = OroCiliegio),
                        modifier = Modifier.weight(1.2f).height(40.dp)
                    ) {
                        Text("SALVA CLOUD", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // --- OVERLAY PREVIEW ZOOMABILE ---
        if (showPreview && previewBitmap != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).zIndex(10f)
            ) {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale *= zoomChange
                    offset += offsetChange
                }

                androidx.compose.foundation.Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = maxOf(1f, scale),
                            scaleY = maxOf(1f, scale),
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(state = transformState)
                )

                // Bottone Chiudi con Pulizia Memoria
                IconButton(
                    onClick = {
                        showPreview = false
                        previewBitmap?.recycle() // Libera la RAM della bitmap
                        previewBitmap = null
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                }

                Text(
                    "Pizzica per zoomare • Trascina per spostare",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                )
            }
        }
    }

    // --- DIALOGS (Invariati) ---
    if (mostraDialogoAggiunta) {
        AlertDialog(
            onDismissRequest = { mostraDialogoAggiunta = false },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    "Aggiungi a $catCorrente",
                    color = OroCiliegio,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = nuovoIngredienteNome,
                    onValueChange = { nuovoIngredienteNome = it },
                    label = { Text("Nome", color = OroCiliegio) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OroCiliegio,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        productViewModel.aggiungiIngredienteVeloce(
                            nuovoIngredienteNome,
                            catCorrente,
                            sottoCategoriaTarget,
                            context
                        )
                        nuovoIngredienteNome = ""
                        mostraDialogoAggiunta = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OroCiliegio)
                ) {
                    Text("Aggiungi", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostraDialogoAggiunta = false }) {
                    Text("Annulla", color = Color.Gray)
                }
            }
        )
    }

    if (piattoDaModificare != null) {
        AlertDialog(
            onDismissRequest = { piattoDaModificare = null },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    "Modifica/Elimina Piatto",
                    color = OroCiliegio,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Piatto: ${piattoDaModificare!!.second.nomeVisualizzato}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nuovoNomeTesto,
                        onValueChange = { nuovoNomeTesto = it },
                        label = { Text("Nome visualizzato", color = OroCiliegio) },
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
                Button(
                    onClick = {
                        if (nuovoNomeTesto.isNotEmpty()) {
                            builderViewModel.rinominaESalvaRegola(
                                piattoDaModificare!!.first,
                                piattoDaModificare!!.second,
                                nuovoNomeTesto,
                                context,
                                productViewModel
                            )
                        }
                        piattoDaModificare = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OroCiliegio),
                    enabled = nuovoNomeTesto.isNotEmpty()
                ) {
                    Text("Salva", color = Color.Black)
                }
            },
            dismissButton = {
                Row {
                    Button(
                        onClick = {
                            builderViewModel.rimuoviPiatto(
                                piattoDaModificare!!.first,
                                piattoDaModificare!!.second
                            )
                            piattoDaModificare = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Elimina", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { piattoDaModificare = null }
                    ) {
                        Text("Annulla", color = Color.Gray)
                    }
                }
            }
        )
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