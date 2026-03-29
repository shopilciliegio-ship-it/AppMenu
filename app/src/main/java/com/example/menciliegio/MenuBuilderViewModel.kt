package com.example.menciliegio

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// Definizione del supporto piatto
data class PiattoMenu(
    val originaleGrezzo: String,
    val nomeVisualizzato: String
)

class MenuBuilderViewModel(private val dao: MenuDao) : ViewModel() {

    val menuFinale = mutableStateMapOf(
        "Antipasti" to mutableStateListOf<PiattoMenu>(),
        "Primi" to mutableStateListOf<PiattoMenu>(),
        "Secondi" to mutableStateListOf<PiattoMenu>(),
        "Contorni" to mutableStateListOf<PiattoMenu>(),
        "Dolci" to mutableStateListOf<PiattoMenu>()
    )

    val selectedBase = mutableStateListOf<String>()
    val selectedExtra = mutableStateListOf<String>()
    val prezzoPersona = mutableStateOf("")
    val linguaCorrente = mutableStateOf("IT")

    fun generaPiatto(categoria: String) {
        if (selectedBase.isEmpty() && selectedExtra.isEmpty()) return

        val baseStr = selectedBase.joinToString(" e ")
        val extraStr = selectedExtra.joinToString(" e ")
        val combinazioneGrezza = if (baseStr.isNotEmpty() && extraStr.isNotEmpty()) "$baseStr con $extraStr"
        else baseStr + extraStr

        viewModelScope.launch {
            val nomeBello = dao.getNomeCorretto(combinazioneGrezza) ?: combinazioneGrezza
            menuFinale[categoria]?.add(PiattoMenu(combinazioneGrezza, nomeBello))
            selectedBase.clear()
            selectedExtra.clear()
        }
    }

    fun rinominaESalvaRegola(
        categoria: String,
        piattoVecchio: PiattoMenu,
        nuovoNome: String,
        context: Context,
        productViewModel: ProductViewModel
    ) {
        viewModelScope.launch {
            dao.insertCorrezione(Correzione(piattoVecchio.originaleGrezzo, nuovoNome))
            val lista = menuFinale[categoria]
            val index = lista?.indexOf(piattoVecchio) ?: -1
            if (index != -1) {
                lista?.set(index, PiattoMenu(piattoVecchio.originaleGrezzo, nuovoNome))
            }
            // AGGIUNTO: Sincronizza il cloud dopo la rinomina
            productViewModel.syncDatabaseToCloud(context)
        }
    }

    fun rimuoviPiatto(categoria: String, piatto: PiattoMenu) {
        menuFinale[categoria]?.remove(piatto)
    }

    // FUNZIONE PER SALVARE IL MENU NEL DATABASE (ARCHIVIO)
    fun salvaMenuNelDB(data: String, servizio: String, nomeExtra: String) {
        val idMenu = "$data-$servizio-$nomeExtra"

        // Trasformiamo la mappa dei piatti in una stringa JSON per il database
        val rootJson = JSONObject()
        menuFinale.forEach { (cat, lista) ->
            val array = JSONArray()
            lista.forEach { array.put(it.nomeVisualizzato) }
            rootJson.put(cat, array)
        }

        viewModelScope.launch {
            dao.saveMenu(MenuSalvato(
                id = idMenu,
                data = data,
                giorno = "", // Calcolabile se necessario
                servizio = servizio,
                nomeMenu = nomeExtra,
                prezzo = prezzoPersona.value,
                itemsJson = rootJson.toString()
            ))
        }
    }
    fun caricaDaJson(jsonString: String, linguaScelta: String) {
        linguaCorrente.value = linguaScelta // Salviamo la lingua scelta
        try {
            val root = JSONObject(jsonString)
            // Scegliamo quale nodo leggere: "menu_it" o "menu_en"
            val menuKey = if (linguaScelta == "IT") "menu_it" else "menu_en"

            // Usiamo optJSONObject per evitare crash se la chiave non esiste
            val piattiJson = root.optJSONObject(menuKey) ?: return

            // 1. NON svuotiamo la mappa (menuFinale.clear()),
            // ma svuotiamo solo le SINGOLE liste al suo interno.
            // Così manteniamo i collegamenti con la UI di Compose.
            menuFinale.forEach { (_, lista) ->
                lista.clear()
            }

            // 2. Ripopoliamo le liste
            piattiJson.keys().forEach { categoria ->
                val array = piattiJson.getJSONArray(categoria)

                // Cerchiamo la lista corrispondente nella nostra mappa
                val listaDestinazione = menuFinale[categoria]

                for (i in 0 until array.length()) {
                    val nomeCaricato = array.getString(i)

                    // IMPORTANTE: Quando carichiamo un menù salvato, non abbiamo più
                    // gli ingredienti "grezzi" (base + extra), quindi usiamo
                    // il nome visualizzato per entrambi i campi del PiattoMenu.
                    listaDestinazione?.add(
                        PiattoMenu(
                            originaleGrezzo = nomeCaricato,
                            nomeVisualizzato = nomeCaricato
                        )
                    )
                }
            }

            // 3. Carichiamo il prezzo
            prezzoPersona.value = root.optString("price", "")

        } catch (e: Exception) { e.printStackTrace() }
    }
}