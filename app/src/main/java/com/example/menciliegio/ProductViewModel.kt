package com.example.menciliegio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ProductViewModel(val dao: MenuDao) : ViewModel() {

    val allProducts = dao.getAllProdotti()
    var nomeInserito = MutableStateFlow("")
    var categoriaSelezionata = MutableStateFlow("Antipasti")
    var sottocategoriaSelezionata = MutableStateFlow("Base")

    fun aggiungiProdotto() {
        val testo = nomeInserito.value.trim()
        if (testo.isNotEmpty()) {
            val nomeFormattato = testo.lowercase().replaceFirstChar { it.uppercase() }
            viewModelScope.launch {
                dao.insertProdotto(Prodotto(nome = nomeFormattato, categoria = categoriaSelezionata.value, sottocategoria = sottocategoriaSelezionata.value))
                nomeInserito.value = ""
            }
        }
    }

    fun aggiungiIngredienteVeloce(nome: String, categoria: String, sottocategoria: String) {
        if (nome.isBlank()) return
        val nomeFormattato = nome.trim().lowercase().replaceFirstChar { it.uppercase() }
        viewModelScope.launch {
            dao.insertProdotto(Prodotto(nome = nomeFormattato, categoria = categoria, sottocategoria = sottocategoria))
        }
    }

    fun eliminaProdotto(prodotto: Prodotto) {
        viewModelScope.launch { dao.deleteProdotto(prodotto) }
    }

    suspend fun esportaDatiInJson(): String {
        val root = JSONObject()
        val prodottiArray = JSONArray()

        dao.getAllProdottiStatic().forEach { p ->
            val obj = JSONObject()
            obj.put("nome", p.nome)
            obj.put("categoria", p.categoria)
            obj.put("sottocategoria", p.sottocategoria)
            prodottiArray.put(obj)
        }

        val correzioniObj = JSONObject()
        dao.getAllCorrezioniStatic().forEach { c ->
            correzioniObj.put(c.originale, c.corretto)
        }

        root.put("prodotti", prodottiArray)
        root.put("correzioni", correzioniObj)
        return root.toString(2)
    }

    fun importaDatiDaJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)
                if (root.has("prodotti")) {
                    val array = root.getJSONArray("prodotti")
                    val listaP = mutableListOf<Prodotto>()
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        listaP.add(Prodotto(nome = o.getString("nome"), categoria = o.getString("categoria"), sottocategoria = o.getString("sottocategoria")))
                    }
                    dao.insertProdotti(listaP)
                }
                if (root.has("correzioni")) {
                    val obj = root.getJSONObject("correzioni")
                    val listaC = mutableListOf<Correzione>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        listaC.add(Correzione(k, obj.getString(k)))
                    }
                    dao.insertCorrezioni(listaC)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}