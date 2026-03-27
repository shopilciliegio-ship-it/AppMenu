package com.example.menciliegio

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
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

    @Query("SELECT corretto FROM correzioni WHERE originale = :orig LIMIT 1")
    suspend fun getNomeCorretto(orig: String): String?

    @Query("SELECT * FROM correzioni")
    suspend fun getAllCorrezioniStatic(): List<Correzione>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrezioni(correzioni: List<Correzione>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrezione(correzione: Correzione)

    // NUOVA FUNZIONE PER L'ARCHIVIO
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMenu(menu: MenuSalvato)
}