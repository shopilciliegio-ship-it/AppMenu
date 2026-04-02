package com.example.menciliegio

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.microsoft.graph.requests.GraphServiceClient
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class MainActivity : ComponentActivity() {

    // Cambiato a SingleAccount per semplicità e coerenza con il file JSON
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var graphClient: GraphServiceClient<okhttp3.Request>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. INIZIALIZZAZIONE DATABASE E VIEWMODEL ---
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

        // --- 2. INIZIALIZZAZIONE MICROSOFT AUTH (MSAL) ---
        // Nota: ho corretto R.raw.auth_config_single_account (senza .json)
        PublicClientApplication.createSingleAccountPublicClientApplication(
            this,
            R.raw.`auth_config_single_account`,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    Log.d("MSAL", "App Microsoft inizializzata correttamente!")
                }

                override fun onError(exception: MsalException) {
                    Log.e("MSAL", "Errore configurazione: ${exception.message}")
                }
            })

        // --- 3. INTERFACCIA UTENTE ---
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current

            val folderLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(it, takeFlags)
                    CloudStorageHelper.saveFolderUri(context, it)
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        Box(modifier = Modifier.fillMaxSize()) {
                            HomeScreen(
                                onNavigate = { navController.navigate(it) },
                                productViewModel = productViewModel
                            )

                            TextButton(
                                onClick = { folderLauncher.launch(null) },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            ) {
                                Text("Imposta Cartella Condivisa", color = Color.Gray, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { signInToSharePoint() },
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Login SharePoint", fontSize = 10.sp)
                            }
                        }
                    }
                    // ... (restanti composable rimangono uguali)
                    composable("archivio") {
                        ArchiveScreen(
                            onBack = { navController.popBackStack() },
                            onFileSelected = { jsonContent, _, _, _, lingua ->
                                val root = JSONObject(jsonContent)
                                val data = root.getString("date")
                                val servizio = root.getString("service")
                                val nome = root.optString("extra_name", "Standard")
                                builderViewModel.caricaDaJson(jsonContent, lingua)
                                navController.navigate("builder/$data/$servizio/$nome")
                            }
                        )
                    }

                    composable("prodotti") {
                        ProductManagerScreen(productViewModel, onBack = { navController.popBackStack() })
                    }

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

    fun signInToSharePoint() {
        val app = mSingleAccountApp
        if (app == null) {
            Toast.makeText(this, "Attendi: inizializzazione in corso...", Toast.LENGTH_SHORT).show()
            return
        }

        // Scope semplificati per SharePoint/OneDrive
        val scopes = arrayOf("Files.ReadWrite.All")

        app.signIn(this, "", scopes, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                val accessToken = authenticationResult.accessToken

                // Inizializzazione client Graph
                graphClient = GraphServiceClient.builder()
                    .authenticationProvider { CompletableFuture.completedFuture(accessToken) }
                    .buildClient()

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Login SharePoint Riuscito!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(exception: MsalException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Errore: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancel() {
                Log.d("SharePoint", "Login annullato")
            }
        })
    }
}