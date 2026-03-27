package com.example.menciliegio

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prodotti")
data class Prodotto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val categoria: String,
    val sottocategoria: String
)

@Entity(tableName = "correzioni")
data class Correzione(
    @PrimaryKey val originale: String,
    val corretto: String
)

@Entity(tableName = "menu_salvati")
data class MenuSalvato(
    @PrimaryKey val id: String,
    val data: String,
    val giorno: String,
    val servizio: String,
    val nomeMenu: String?,
    val prezzo: String?,
    val itemsJson: String
)