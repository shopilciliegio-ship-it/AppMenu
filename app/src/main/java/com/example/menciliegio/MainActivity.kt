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
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(onNavigate = { navController.navigate(it) }) }
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