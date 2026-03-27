package com.example.menciliegio

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray     // IMPORTANTE: aggiunta questa
import org.json.JSONObject    // IMPORTANTE: aggiunta questa

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

    fun rinominaESalvaRegola(categoria: String, piattoVecchio: PiattoMenu, nuovoNome: String) {
        viewModelScope.launch {
            dao.insertCorrezione(Correzione(piattoVecchio.originaleGrezzo, nuovoNome))
            val lista = menuFinale[categoria]
            val index = lista?.indexOf(piattoVecchio) ?: -1
            if (index != -1) {
                lista?.set(index, PiattoMenu(piattoVecchio.originaleGrezzo, nuovoNome))
            }
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
}