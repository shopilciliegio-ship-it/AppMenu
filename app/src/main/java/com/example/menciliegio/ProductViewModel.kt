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
    fun rinominaProdotto(prodotto: Prodotto, nuovoNome: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val aggiornato = prodotto.copy(nome = nuovoNome.trim())
            dao.insertProdotto(aggiornato)
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

                    // ✅ Carica i prodotti già presenti nel database
                    val esistenti = dao.getAllProdottiStatic()
                    val nomiEsistenti = esistenti.map {
                        "${it.nome.trim().lowercase()}|${it.categoria}|${it.sottocategoria}"
                    }.toSet()

                    val nuovi = mutableListOf<Prodotto>()
                    for (i in 0 until arrayP.length()) {
                        val o = arrayP.getJSONObject(i)
                        val nome = o.getString("nome").trim()
                        val categoria = o.getString("categoria")
                        val sottocategoria = o.getString("sottocategoria")
                        val chiave = "${nome.lowercase()}|${categoria}|${sottocategoria}"

                        // ✅ Aggiunge solo se NON esiste già
                        if (chiave !in nomiEsistenti) {
                            nuovi.add(Prodotto(nome = nome, categoria = categoria, sottocategoria = sottocategoria))
                        }
                    }

                    if (nuovi.isNotEmpty()) {
                        dao.insertProdotti(nuovi)
                        Log.d("Import", "Importati ${nuovi.size} nuovi prodotti, skippati ${arrayP.length() - nuovi.size} duplicati")
                    } else {
                        Log.d("Import", "Nessun nuovo prodotto da importare, tutti già presenti")
                    }
                }

                if (root.has("correzioni")) {
                    val arrayC = root.getJSONArray("correzioni")

                    // ✅ Carica le correzioni già presenti nel database
                    val correzioniEsistenti = dao.getAllCorrezioniStatic()
                    val originaliEsistenti = correzioniEsistenti.map {
                        it.originale.trim().lowercase()
                    }.toSet()

                    val nuove = mutableListOf<Correzione>()
                    for (i in 0 until arrayC.length()) {
                        val o = arrayC.getJSONObject(i)
                        val originale = o.getString("originale").trim()
                        val corretto = o.getString("corretto").trim()

                        // ✅ Aggiunge solo se NON esiste già
                        if (originale.lowercase() !in originaliEsistenti) {
                            nuove.add(Correzione(originale = originale, corretto = corretto))
                        }
                    }

                    if (nuove.isNotEmpty()) {
                        dao.insertCorrezioni(nuove)
                        Log.d("Import", "Importate ${nuove.size} nuove correzioni, skippate ${arrayC.length() - nuove.size} duplicate")
                    } else {
                        Log.d("Import", "Nessuna nuova correzione da importare, tutte già presenti")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
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