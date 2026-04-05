package com.example.menciliegio

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.microsoft.identity.client.exception.MsalUiRequiredException
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class MainActivity : ComponentActivity() {

    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var graphClient: GraphServiceClient<okhttp3.Request>? = null

    // ✅ Stato login condiviso tra MSAL e UI
    private var onLoginSuccess: (() -> Unit)? = null

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
        PublicClientApplication.createSingleAccountPublicClientApplication(
            this,
            R.raw.`auth_config_single_account`,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    Log.d("MSAL", "App Microsoft inizializzata correttamente!")

                    // ✅ Tenta subito il silent login all'avvio
                    tentaSilentLogin(productViewModel)
                }

                override fun onError(exception: MsalException) {
                    Log.e("MSAL", "Errore configurazione: ${exception.message}")
                }
            })

        // --- 3. INTERFACCIA UTENTE ---
        setContent {
            val navController = rememberNavController()

            // ✅ Stato reattivo per mostrare/nascondere il bottone login
            var loginEffettuato by remember { mutableStateOf(false) }

            // ✅ Collega il callback UI al silent login
            onLoginSuccess = { loginEffettuato = true }

            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        Box(modifier = Modifier.fillMaxSize()) {
                            HomeScreen(
                                onNavigate = { navController.navigate(it) },
                                productViewModel = productViewModel
                            )

                            // ✅ Mostra bottone Login solo se NON ancora loggato
                            if (!loginEffettuato) {
                                Button(
                                    onClick = {
                                        signInToSharePoint(
                                            productViewModel = productViewModel,
                                            onSuccess = { loginEffettuato = true }
                                        )
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                        .fillMaxWidth(0.8f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                ) {
                                    Text("Login SharePoint", fontSize = 14.sp)
                                }
                            } else {
                                // ✅ Dopo il login mostra indicatore connessione
                                Text(
                                    text = "☁️ OneDrive connesso",
                                    color = Color.Green,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
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
                                builderViewModel.caricaDaJson(jsonContent, lingua)
                                navController.navigate("builder/$data/$servizio/$nome")
                            }
                        )
                    }
                    composable("prodotti") {
                        ProductManagerScreen(
                            viewModel = productViewModel,
                            sharePointService = graphClient?.let { SharePointService(it) },
                            onBack = { navController.popBackStack() }
                        )
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

    // ✅ SILENT LOGIN: Tenta login automatico all'avvio senza UI
    private fun tentaSilentLogin(productViewModel: ProductViewModel) {
        val app = mSingleAccountApp ?: return

        Thread {
            try {
                // Controlla se esiste un account salvato
                val account = app.currentAccount?.currentAccount
                if (account == null) {
                    Log.d("MSAL", "Nessun account salvato → login manuale richiesto")
                    return@Thread
                }

                Log.d("MSAL", "Account trovato: ${account.username}, tento silent login...")

                // Acquisisce token in silenzio (senza UI)
                val scopes = arrayOf("Files.ReadWrite.All")
                val result = app.acquireTokenSilent(scopes, account.authority)
                val accessToken = result.accessToken

                // Crea il graphClient con il token ottenuto
                graphClient = GraphServiceClient.builder()
                    .authenticationProvider { CompletableFuture.completedFuture(accessToken) }
                    .buildClient()

                Log.d("MSAL", "Silent login riuscito!")

                // ✅ Notifica la UI che il login è avvenuto
                runOnUiThread {
                    onLoginSuccess?.invoke()
                    Toast.makeText(this, "☁️ Connesso a OneDrive!", Toast.LENGTH_SHORT).show()
                }

                // ✅ Scarica automaticamente il JSON da OneDrive
                val service = SharePointService(graphClient!!)
                val jsonContent = service.downloadJsonFromOneDrive("backup_ciliegio.json")
                if (jsonContent != null) {
                    productViewModel.importaDatiDaJson(jsonContent)
                    runOnUiThread {
                        Toast.makeText(this, "✅ Dati aggiornati da OneDrive!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("MSAL", "Nessun backup trovato su OneDrive")
                }

            } catch (e: MsalUiRequiredException) {
                // Token scaduto o non presente → serve login manuale, nessun errore
                Log.d("MSAL", "Token scaduto → login manuale richiesto")
            } catch (e: Exception) {
                Log.e("MSAL", "Errore silent login: ${e.message}")
            }
        }.start()
    }

    // Login manuale (quando l'utente preme il bottone)
    fun signInToSharePoint(
        productViewModel: ProductViewModel,
        onSuccess: (() -> Unit)? = null
    ) {
        val app = mSingleAccountApp
        if (app == null) {
            Toast.makeText(this, "Attendi: inizializzazione in corso...", Toast.LENGTH_SHORT).show()
            return
        }

        val scopes = arrayOf("Files.ReadWrite.All")
        app.signIn(this, "", scopes, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                val accessToken = authenticationResult.accessToken

                graphClient = GraphServiceClient.builder()
                    .authenticationProvider { CompletableFuture.completedFuture(accessToken) }
                    .buildClient()

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Login riuscito! Carico dati...", Toast.LENGTH_SHORT).show()
                    onSuccess?.invoke()
                }

                // Download automatico del JSON da OneDrive dopo il login manuale
                Thread {
                    try {
                        val service = SharePointService(graphClient!!)
                        val jsonContent = service.downloadJsonFromOneDrive("backup_ciliegio.json")
                        if (jsonContent != null) {
                            productViewModel.importaDatiDaJson(jsonContent)
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "✅ Dati caricati da OneDrive!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Nessun backup trovato su OneDrive", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Errore caricamento: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
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