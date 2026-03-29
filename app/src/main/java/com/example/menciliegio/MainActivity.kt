package com.example.menciliegio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box // Aggiunto per sicurezza
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.menuDao()


        val productViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProductViewModel(dao) as T
            }
        })[ProductViewModel::class.java]

        val builderViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MenuBuilderViewModel(dao) as T
            }
        })[MenuBuilderViewModel::class.java]

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current

            // Launcher per selezionare la cartella cloud (OneDrive)
            val folderLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    // Chiediamo il permesso permanente ad Android
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(it, takeFlags)

                    // Salviamo l'URI nelle preferenze
                    CloudStorageHelper.saveFolderUri(context, it)
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        // Usiamo un Box per mettere il tastino piccolo sopra la HomeScreen senza rompere il layout
                        Box(modifier = Modifier.fillMaxSize()) {
                            // La tua schermata principale
                            HomeScreen(
                                onNavigate = { navController.navigate(it) },
                                productViewModel = productViewModel // <--- AGGIUNGI QUESTA RIGA
                            )

                            // Il tastino per il Cloud posizionato in basso al centro
                            TextButton(
                                onClick = { folderLauncher.launch(null) },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter) // Ora Alignment verrà riconosciuto
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = "Imposta Cartella Condivisa",
                                    color = Color.Gray,
                                    fontSize = 12.sp // Ora sp verrà riconosciuto
                                )
                            }
                        }
                    }
                    composable("archivio") {
                        ArchiveScreen(
                            onBack = { navController.popBackStack() },
                            onFileSelected = { jsonContent, _, _, _, lingua ->
                                val root = JSONObject(jsonContent)
                                val data = root.getString("date")
                                val servizio = root.getString("service")
                                val nome = root.optString("extra_name", "Standard")

                                // 1. Carichiamo i dati nel ViewModel
                                builderViewModel.caricaDaJson(jsonContent, lingua)

                                // 2. Navighiamo al builder (che troverà i dati già pronti)
                                navController.navigate("builder/$data/$servizio/$nome")
                            }
                        )
                    }
                    // ... (altre rotte invariate)
                    composable("prodotti") { ProductManagerScreen(productViewModel, onBack = { navController.popBackStack() }) }
                    composable("compila_header") {
                        MenuHeaderScreen(
                            onConfirm = { d, s, n ->
                                val nome = if (n.isEmpty()) "Standard" else n
                                navController.navigate("builder/$d/$s/$nome")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "builder/{data}/{servizio}/{nome}",
                        arguments = listOf(
                            navArgument("data") { type = NavType.StringType },
                            navArgument("servizio") { type = NavType.StringType },
                            navArgument("nome") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        MenuBuilderScreen(
                            data = backStackEntry.arguments?.getString("data") ?: "",
                            servizio = backStackEntry.arguments?.getString("servizio") ?: "",
                            nomeMenu = backStackEntry.arguments?.getString("nome") ?: "",
                            productViewModel = productViewModel,
                            builderViewModel = builderViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}