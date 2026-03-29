package com.example.menciliegio

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. Tabella dei PRODOTTI (gli ingredienti che gestisci in "Gestione Prodotti")
@Entity(tableName = "prodotti")
data class Prodotto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // L'ID viene creato automaticamente
    val nome: String,
    val categoria: String,     // Antipasti, Primi, ecc.
    val sottocategoria: String // Base o Extra
)

// 2. Tabella delle CORREZIONI (le regole di rinomina "Pasta + Pomodoro" -> "Mezze maniche...")
@Entity(tableName = "correzioni")
data class Correzione(
    @PrimaryKey val originale: String, // La combinazione grezza è la chiave (unica)
    val corretto: String              // Il nome "bello" che vuoi visualizzare
)

// 3. Tabella dei MENU SALVATI (l'archivio storico dei menù)
@Entity(tableName = "menu_salvati")
data class MenuSalvato(
    @PrimaryKey val id: String, // Esempio: "2026-03-27-pranzo-Standard"
    val data: String,
    val giorno: String,
    val servizio: String,
    val nomeMenu: String,
    val prezzo: String,
    val itemsJson: String // Qui dentro salviamo la lista dei piatti come testo JSON
)