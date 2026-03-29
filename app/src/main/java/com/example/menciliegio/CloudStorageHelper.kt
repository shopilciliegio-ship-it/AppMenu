package com.example.menciliegio

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object CloudStorageHelper {
    private const val PREFS_NAME = "CloudPrefs"
    private const val KEY_FOLDER_URI = "folder_uri"

    fun saveFolderUri(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
    }

    fun getFolderUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_FOLDER_URI, null)
        return uriString?.let { Uri.parse(it) }
    }

    // Funzione per scrivere un file (ByteArray) nella cartella selezionata
    fun writeFileToCloud(context: Context, fileName: String, mimeType: String, content: ByteArray): Boolean {
        val folderUri = getFolderUri(context) ?: return false
        val root = DocumentFile.fromTreeUri(context, folderUri) ?: return false

        return try {
            // Cerchiamo se il file esiste già
            val existingFile = root.findFile(fileName)

            // Se esiste usiamo quello, altrimenti lo creiamo
            val fileUri = existingFile?.uri ?: root.createFile(mimeType, fileName)?.uri
            ?: return false

            // "wt" sta per Write-Truncate: svuota il file e scrive da zero (sovrascrive)
            context.contentResolver.openOutputStream(fileUri, "wt")?.use { outputStream ->
                outputStream.write(content)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    fun listJsonFiles(context: Context): List<DocumentFile> {
        val folderUri = getFolderUri(context) ?: return emptyList()
        val root = DocumentFile.fromTreeUri(context, folderUri)
        // Prende solo i file che finiscono con .json e li ordina dal più recente
        return root?.listFiles()
            ?.filter { it.name?.endsWith(".json") == true }
            ?.sortedByDescending { it.name } ?: emptyList()
    }

    fun readFileContent(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}