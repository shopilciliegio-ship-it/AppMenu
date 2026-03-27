package com.example.menciliegio

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MenuHeaderScreen(onConfirm: (String, String, String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var dataSelezionata by remember { mutableStateOf(calendar.time) }
    var servizioSelezionato by remember { mutableStateOf("Pranzo") }
    var nomeMenuExtra by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
    val dayNameFormat = SimpleDateFormat("EEEE", Locale.ITALY)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            dataSelezionata = cal.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("NUOVO MENÙ", color = OroCiliegio, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(30.dp))
        Text("📅 Data del menù:", color = Color.White, fontSize = 18.sp)
        Button(onClick = { datePickerDialog.show() }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
            Text(dateFormat.format(dataSelezionata), color = OroCiliegio)
        }
        Text(dayNameFormat.format(dataSelezionata).replaceFirstChar { it.uppercase() }, color = OroCiliegio, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = servizioSelezionato == "Pranzo", onClick = { servizioSelezionato = "Pranzo" }, colors = RadioButtonDefaults.colors(selectedColor = OroCiliegio))
            Text("Pranzo", color = Color.White)
            Spacer(modifier = Modifier.width(20.dp))
            RadioButton(selected = servizioSelezionato == "Cena", onClick = { servizioSelezionato = "Cena" }, colors = RadioButtonDefaults.colors(selectedColor = OroCiliegio))
            Text("Cena", color = Color.White)
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = nomeMenuExtra,
            onValueChange = { nomeMenuExtra = it },
            label = { Text("Nome menù (opzionale)", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = OroCiliegio)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = { onConfirm(dateFormat.format(dataSelezionata), servizioSelezionato, nomeMenuExtra) }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = OroCiliegio)) {
            Text("CONFERMA", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onBack) { Text("ANNULLA", color = Color.Red) }
    }
}