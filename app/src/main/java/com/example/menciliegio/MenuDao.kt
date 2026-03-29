package com.example.menciliegio

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    // --- TABELLA PRODOTTI (Ingredienti base ed extra) ---
    @Query("SELECT * FROM prodotti ORDER BY categoria, nome")
    fun getAllProdotti(): Flow<List<Prodotto>>

    @Query("SELECT * FROM prodotti")
    suspend fun getAllProdottiStatic(): List<Prodotto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProdotti(prodotti: List<Prodotto>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProdotto(prodotto: Prodotto)

    @Delete
    suspend fun deleteProdotto(prodotto: Prodotto)


    // --- TABELLA CORREZIONI (Regole di rinomina: "Pasta+Pomodoro" -> "Mezze maniche...") ---
    @Query("SELECT corretto FROM correzioni WHERE originale = :orig LIMIT 1")
    suspend fun getNomeCorretto(orig: String): String?

    @Query("SELECT * FROM correzioni")
    suspend fun getAllCorrezioniStatic(): List<Correzione>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrezione(correzione: Correzione)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrezioni(correzioni: List<Correzione>)


    // --- TABELLA ARCHIVIO (I menù già salvati e completati) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMenu(menu: MenuSalvato)

    @Query("SELECT * FROM menu_salvati ORDER BY data DESC")
    suspend fun getAllMenuSalvati(): List<MenuSalvato>
}