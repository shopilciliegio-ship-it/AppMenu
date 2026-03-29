package com.example.menciliegio

// Questa classe serve a salvare tutto in un unico file
data class MenuCompleto(
    val data: String,
    val servizio: String,
    val nomeExtra: String?,
    val portateIT: Map<String, String>, // Esempio: "Primo" -> "Pasta"
    val portateEN: Map<String, String>  // Esempio: "First Course" -> "Pasta"
)