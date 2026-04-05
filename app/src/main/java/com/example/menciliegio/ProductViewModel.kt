package com.example.menciliegio

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ProductViewModel(val dao: MenuDao) : ViewModel() {

    val allProducts = dao.getAllProdotti()
    var nomeInserito = MutableStateFlow("")
    var categoriaSelezionata = MutableStateFlow("Antipasti")
    var sottocategoriaSelezionata = MutableStateFlow("Base")

    // --- FUNZIONE DI SINCRONIZZAZIONE ---
    fun syncDatabaseToCloud(context: Context) {
        // Sincronizzazione ora gestita tramite SharePointService al login
    }

    // 1. Questa serve per la schermata "Gestione Prodotti"
    fun aggiungiProdotto(context: Context) {
        val testo = nomeInserito.value.trim()
        if (testo.isNotEmpty()) {
            val nomeFormattato = testo.lowercase().replaceFirstChar { it.uppercase() }
            viewModelScope.launch(Dispatchers.IO) {
                dao.insertProdotto(Prodotto(nome = nomeFormattato, categoria = categoriaSelezionata.value, sottocategoria = sottocategoriaSelezionata.value))
                nomeInserito.value = ""
                syncDatabaseToCloud(context) // Sincronizza Cloud
            }
        }
    }

    // 2. Questa serve per il tastino "+" nel "Compila Menù" (L'errore è qui!)
    fun aggiungiIngredienteVeloce(nome: String, categoria: String, sottocategoria: String, context: Context) {
        if (nome.isBlank()) return
        val nomeFormattato = nome.trim().lowercase().replaceFirstChar { it.uppercase() }
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertProdotto(Prodotto(nome = nomeFormattato, categoria = categoria, sottocategoria = sottocategoria))
            syncDatabaseToCloud(context) // Sincronizza Cloud
        }
    }

    fun eliminaProdotto(prodotto: Prodotto, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteProdotto(prodotto)
            syncDatabaseToCloud(context) // Sincronizza
        }
    }

    suspend fun esportaDatiInJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val prodottiArray = JSONArray()
        dao.getAllProdottiStatic().forEach { p ->
            prodottiArray.put(JSONObject().apply {
                put("nome", p.nome)
                put("categoria", p.categoria)
                put("sottocategoria", p.sottocategoria)
            })
        }
        val correzioniArray = JSONArray()
        dao.getAllCorrezioniStatic().forEach { c ->
            correzioniArray.put(JSONObject().apply {
                put("originale", c.originale)
                put("corretto", c.corretto)
            })
        }
        root.put("prodotti", prodottiArray)
        root.put("correzioni", correzioniArray)
        return@withContext root.toString(2)
    }

    fun importaDatiDaJson(jsonString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                if (root.has("prodotti")) {
                    val arrayP = root.getJSONArray("prodotti")
                    val listaP = mutableListOf<Prodotto>()
                    for (i in 0 until arrayP.length()) {
                        val o = arrayP.getJSONObject(i)
                        listaP.add(Prodotto(nome = o.getString("nome"), categoria = o.getString("categoria"), sottocategoria = o.getString("sottocategoria")))
                    }
                    dao.insertProdotti(listaP)
                }
                if (root.has("correzioni")) {
                    val arrayC = root.getJSONArray("correzioni")
                    val listaC = mutableListOf<Correzione>()
                    for (i in 0 until arrayC.length()) {
                        val o = arrayC.getJSONObject(i)
                        listaC.add(Correzione(originale = o.getString("originale"), corretto = o.getString("corretto")))
                    }
                    dao.insertCorrezioni(listaC)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    // Dentro ProductViewModel.kt
    fun salvaSuSharePoint(service: SharePointService, jsonDati: String, immagineDati: ByteArray?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nomeFileJson = "menu_${System.currentTimeMillis()}.json"
                service.uploadJsonToOneDrive(jsonDati, nomeFileJson)

                immagineDati?.let {
                    val nomeFileJpg = "foto_${System.currentTimeMillis()}.jpg"
                    service.uploadImageToOneDrive(it, nomeFileJpg)
                }
            } catch (e: Exception) {
                Log.e("SharePoint", "Errore durante l'upload", e)
            }
        }
    }
}