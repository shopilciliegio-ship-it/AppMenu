package com.example.menciliegio

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

object MenuGenerator {

    // Funzione per generare il JPG (rimane simile, usata per IT e poi per EN finale)
    fun generateMenuJpg(
        context: Context,
        piatti: Map<String, List<String>>,
        testoHeader: String,
        prezzo: String,
        isEnglish: Boolean
    ): Bitmap {
        val width = 650
        val height = 919
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val sfondoRaw = BitmapFactory.decodeResource(context.resources, R.drawable.prova5)
        canvas.drawBitmap(Bitmap.createScaledBitmap(sfondoRaw, width, height, true), 0f, 0f, null)

        val logoRaw = BitmapFactory.decodeResource(context.resources, R.drawable.ciliegio_trasparente)
        if (logoRaw != null) {
            val logoW = 160
            val logoH = (logoRaw.height * logoW) / logoRaw.width
            canvas.drawBitmap(Bitmap.createScaledBitmap(logoRaw, logoW, logoH, true), (width - logoW) / 2f, 25f, null)
        }

        val paintTitolo = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 26f
        }

        canvas.drawText(testoHeader, width / 2f, 190f, paintTitolo)

        val paintPiatti = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 22f
        }

        var yPos = 230f
        val maxContentWidth = width - 80

        val categorie = listOf("Antipasti", "Primi", "Secondi", "Contorni", "Dolci")
        val traduzioniCat = mapOf("Antipasti" to "APPETIZERS", "Primi" to "FIRST COURSES", "Secondi" to "MAIN COURSES", "Contorni" to "SIDE DISHES", "Dolci" to "DESSERTS")

        categorie.forEach { cat ->
            val lista = piatti[cat]
            if (!lista.isNullOrEmpty()) {
                paintTitolo.textSize = 24f
                val titolo = if (isEnglish) traduzioniCat[cat] ?: cat.uppercase() else cat.uppercase()
                canvas.drawText(titolo, width / 2f, yPos, paintTitolo)
                yPos += 40f

                lista.forEach { piatto ->
                    val staticLayout = StaticLayout.Builder.obtain(piatto, 0, piatto.length, paintPiatti, maxContentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build()
                    canvas.save()
                    canvas.translate(40f, yPos)
                    staticLayout.draw(canvas)
                    canvas.restore()
                    yPos += staticLayout.height + 10f
                }
                yPos += 20f
            }
        }

        val prezzoFormattato = try { String.format("%.2f", prezzo.replace(",",".").toDouble()).replace(".",",") } catch(e:Exception) { prezzo }
        paintTitolo.textSize = 24f
        val rigaPrezzo = if (isEnglish) "FULL MENU PER PERSON € $prezzoFormattato" else "MENU' COMPLETO A PERSONA € $prezzoFormattato"
        canvas.drawText(rigaPrezzo, width / 2f, height - 80f, paintTitolo)

        paintTitolo.textSize = 18f
        paintTitolo.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val rigaBimbi = if (isEnglish) "KIDS MENU AVAILABLE ON REQUEST € 13,00" else "DISPONIBILI SU RICHIESTA MENU' BIMBI AD € 13,00"
        canvas.drawText(rigaBimbi, width / 2f, height - 45f, paintTitolo)

        return bitmap
    }

    // NUOVA FUNZIONE DI TRADUZIONE CON CONTROLLO REGOLE DB
    suspend fun translateMenuWithRules(
        piatti: Map<String, List<String>>,
        dao: MenuDao
    ): Map<String, List<String>> {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ITALIAN)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        val translator = Translation.getClient(options)

        return try {
            translator.downloadModelIfNeeded().await()
            val result = mutableMapOf<String, List<String>>()

            for ((cat, lista) in piatti) {
                result[cat] = lista.map { testo ->
                    // 1. Cerca nel DB se esiste una traduzione manuale (es. "Dolce della casa" -> "Homemade dessert")
                    val regolaSalvata = dao.getNomeCorretto(testo)

                    // 2. Se esiste la regola, usa quella. Altrimenti usa Google Translate
                    regolaSalvata ?: translator.translate(testo).await()
                }
            }
            result
        } catch (e: Exception) {
            piatti
        } finally {
            translator.close()
        }
    }
}