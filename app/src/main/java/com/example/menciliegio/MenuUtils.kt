package com.example.menciliegio

import java.text.SimpleDateFormat
import java.util.*

object MenuUtils {

    fun generaNomeFileBase(dataString: String, servizio: String, nomeExtra: String?): String {
        // 1. Capire che giorno è (lunedì, martedì...)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ITALIAN)
        val date = sdf.parse(dataString) ?: Date()
        val giornoSettimana = SimpleDateFormat("EEEE", Locale.ITALIAN).format(date).lowercase()

        // 2. Assegnare il numero in base al giorno e al servizio
        val prefisso = when (giornoSettimana) {
            "lunedì" -> if (servizio.contains("pranzo", true)) "02" else "03"
            "martedì" -> if (servizio.contains("pranzo", true)) "04" else "05"
            "mercoledì" -> if (servizio.contains("pranzo", true)) "06" else "07"
            "giovedì" -> if (servizio.contains("pranzo", true)) "08" else "09"
            "venerdì" -> if (servizio.contains("pranzo", true)) "10" else "11"
            "sabato" -> if (servizio.contains("pranzo", true)) "12" else "13"
            "domenica" -> if (servizio.contains("pranzo", true)) "14" else "15" // 15 per cena per non sovrascrivere
            else -> "00"
        }

        // 3. Comporre il nome
        var nomeBase = "$prefisso-$giornoSettimana-$servizio-$dataString"

        // Aggiunge il nome extra se esiste (togliendo spazi)
        if (!nomeExtra.isNullOrBlank()) {
            nomeBase += "-${nomeExtra.replace(" ", "_")}"
        }

        return nomeBase
    }
}