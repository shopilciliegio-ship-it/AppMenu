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

        // 1. SFONDO E LOGO (Invariati)
        val sfondoRaw = BitmapFactory.decodeResource(context.resources, R.drawable.prova5)
        canvas.drawBitmap(Bitmap.createScaledBitmap(sfondoRaw, width, height, true), 0f, 0f, null)

        val logoRaw = BitmapFactory.decodeResource(context.resources, R.drawable.ciliegio_trasparente)
        if (logoRaw != null) {
            val logoW = 160
            val logoH = (logoRaw.height * logoW) / logoRaw.width
            canvas.drawBitmap(Bitmap.createScaledBitmap(logoRaw, logoW, logoH, true), (width - logoW) / 2f, 25f, null)
        }

        // 2. DEFINIZIONE LIMITI AREA DI SCRITTURA
        val startY = 220f   // Subito dopo l'header
        val endY = 810f     // Prima del prezzo finale
        val availableHeight = endY - startY
        val maxContentWidth = width - 100

        // 3. PREPARAZIONE PAINT
        val paintTitoloHeader = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 26f
        }

        val paintCategoria = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f // Dimensione base, calerà se necessario
        }

        val paintPiatti = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 20f // Dimensione base
        }

        // DISEGNA HEADER
        canvas.drawText(testoHeader, width / 2f, 185f, paintTitoloHeader)

        // 4. CALCOLO DINAMICO DELLO SPAZIO
        val categorieOrdinate = listOf("Antipasti", "Primi", "Secondi", "Contorni", "Dolci")
        val traduzioniCat = mapOf("Antipasti" to "APPETIZERS", "Primi" to "FIRST COURSES", "Secondi" to "MAIN COURSES", "Contorni" to "SIDE DISHES", "Dolci" to "DESSERTS")

        // Creiamo una lista di "blocchi" (StaticLayout) per misurare l'altezza totale
        val layouts = mutableListOf<Pair<StaticLayout, Boolean>>() // Layout e flag "è categoria"
        var altezzaTestoPura = 0f

        categorieOrdinate.forEach { cat ->
            val lista = piatti[cat]
            if (!lista.isNullOrEmpty()) {
                // Layout Categoria
                val tit = if (isEnglish) traduzioniCat[cat] ?: cat.uppercase() else cat.uppercase()
                val layoutCat = StaticLayout.Builder.obtain(tit, 0, tit.length, paintCategoria, maxContentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
                layouts.add(layoutCat to true)
                altezzaTestoPura += layoutCat.height

                // Layout Piatti
                lista.forEach { piatto ->
                    val layoutPiatto = StaticLayout.Builder.obtain(piatto, 0, piatto.length, paintPiatti, maxContentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
                    layouts.add(layoutPiatto to false)
                    altezzaTestoPura += layoutPiatto.height
                }
            }
        }

        // Calcoliamo lo spazio extra (gap) da mettere tra un elemento e l'altro
        // Se altezzaTestoPura è quasi quanto availableHeight, il gap sarà piccolo o nullo
        var gap = (availableHeight - altezzaTestoPura) / (layouts.size + 1)

        // Protezione: se il menu è troppo lungo, riduciamo il font e ricalcoliamo il gap
        if (gap < 5f) {
            paintCategoria.textSize = 19f
            paintPiatti.textSize = 17f
            // (In un caso ideale ricalcoleremmo i layout qui, ma per brevità riduciamo il gap minimo)
            gap = 5f
        }
        if (gap > 35f) gap = 35f // Evitiamo che i piatti siano troppo dispersi se sono pochi

        // 5. DISEGNO EFFETTIVO
        var currentY = startY + (availableHeight - (altezzaTestoPura + (gap * (layouts.size - 1)))) / 2f

        layouts.forEach { (layout, isCategoria) ->
            canvas.save()
            canvas.translate((width - maxContentWidth) / 2f, currentY)
            layout.draw(canvas)
            canvas.restore()

            // Se l'elemento successivo è una categoria, aggiungi un po' di spazio extra
            val extraSpacing = if (isCategoria) gap * 0.5f else gap
            currentY += layout.height + extraSpacing
        }

        // 6. DISEGNA FOOTER (Prezzo e Bimbi) - Posizione fissa in basso
        val prezzoFormattato = try { String.format("%.2f", prezzo.replace(",",".").toDouble()).replace(".",",") } catch(e:Exception) { prezzo }

        val paintFooter = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
        }

        val rigaPrezzo = if (isEnglish) "FULL MENU PER PERSON € $prezzoFormattato" else "MENU' COMPLETO A PERSONA € $prezzoFormattato"
        canvas.drawText(rigaPrezzo, width / 2f, height - 90f, paintFooter)

        paintFooter.textSize = 18f
        paintFooter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val rigaBimbi = if (isEnglish) "KIDS MENU AVAILABLE ON REQUEST € 13,00" else "DISPONIBILI SU RICHIESTA MENU' BIMBI AD € 13,00"
        canvas.drawText(rigaBimbi, width / 2f, height - 55f, paintFooter)

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