package com.example.menciliegio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    productViewModel: ProductViewModel
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isTablet = screenWidth >= 600

// Tutto basato su percentuale dello schermo invece di dp fissi!
    val logoSize = (screenWidth * 0.20f).dp          // 20% della larghezza
    val titleSize = (screenWidth * 0.035f).sp         // 3.5% della larghezza
    val buttonFontSize = (screenWidth * 0.025f).sp    // 2.5% della larghezza
    val buttonHeight = (screenHeight * 0.09f).dp      // 9% dell'altezza
    val buttonPadding = (screenWidth * 0.04f).dp      // 4% della larghezza
    val spacerLarge = (screenHeight * 0.04f).dp       // 4% dell'altezza
    val spacerMedium = (screenHeight * 0.025f).dp     // 2.5% dell'altezza
    val contentMaxWidth = if (isTablet) (screenWidth * 0.55f).dp else screenWidth.dp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = contentMaxWidth)
                .padding(horizontal = buttonPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ciliegio_trasparente),
                contentDescription = "Logo Il Ciliegio",
                modifier = Modifier.size(logoSize)
            )

            Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 16.dp))

            // Titolo
            Text(
                text = "Menù Agriturismo",
                color = OroCiliegio,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(if (isTablet) 48.dp else 32.dp))

            // Bottone Gestione Prodotti
            Button(
                onClick = { onNavigate("prodotti") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
            ) {
                Text(
                    "Gestione Prodotti",
                    fontSize = buttonFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            // Bottone Compila Menù
            Button(
                onClick = { onNavigate("compila_header") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(containerColor = OroCiliegio)
            ) {
                Text(
                    "Compila Menù",
                    fontSize = buttonFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            // Bottone Archivio
            Button(
                onClick = { onNavigate("archivio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    "Archivio Menù",
                    fontSize = buttonFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}