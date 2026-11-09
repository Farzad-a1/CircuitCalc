package com.circuitcalc.electronics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitcalc.electronics.domain.Calculator
import com.circuitcalc.electronics.domain.CalculatorEngine
import com.circuitcalc.electronics.domain.ConversionKind
import com.circuitcalc.electronics.domain.EngineeringCategory
import com.circuitcalc.electronics.domain.EngineeringUnitConverter

@Composable
fun ConvertersPage(onBack: () -> Unit, onTool: (Calculator) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { id -> EngineeringUnitConverter.categories.firstOrNull { it.id == id } }
    if (selected != null) {
        UnitConverterDetail(selected) { selectedId = null }
    } else {
        val categories = EngineeringUnitConverter.searchCategories(query)
        val oldTools = CalculatorEngine.calculators.filter {
            it.category == "Converters" && it.name.contains(query.trim(), ignoreCase = true)
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Converters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Offline unit conversions and existing tools", color = ConverterMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                placeholder = { Text("Search conversion categories or tools") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = ConverterOrange) },
                shape = RoundedCornerShape(16.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (categories.isNotEmpty()) item { Text("Unit conversions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ConverterInk) }
                items(categories, key = { it.id }) { category -> ConversionCategoryCard(category) { selectedId = category.id } }
                if (oldTools.isNotEmpty()) item { Text("Existing converter tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ConverterInk, modifier = Modifier.padding(top = 10.dp)) }
                items(oldTools, key = { it.name }) { tool -> LegacyConverterRow(tool) { onTool(tool) } }
                if (categories.isEmpty() && oldTools.isEmpty()) item { Text("No matching converters", color = ConverterMuted, modifier = Modifier.padding(12.dp)) }
            }
        }
    }
}

@Composable
private fun ConversionCategoryCard(category: EngineeringCategory, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), border = BorderStroke(0.7.dp, ConverterBorder), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ConverterInk)
                Text(category.description, style = MaterialTheme.typography.bodySmall, color = ConverterMuted)
                Text(category.units.joinToString("  ·  ") { it.symbol }, style = MaterialTheme.typography.labelMedium, color = ConverterOrange, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            Text("›", fontSize = 30.sp, color = ConverterOrange, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun LegacyConverterRow(tool: Calculator, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), border = BorderStroke(0.7.dp, ConverterBorder)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("↔", color = ConverterOrange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tool.name, fontWeight = FontWeight.Bold, color = ConverterInk)
                Text(tool.formula, color = ConverterMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("›", fontSize = 28.sp, color = ConverterOrange)
        }
    }
}

@Composable
private fun UnitConverterDetail(category: EngineeringCategory, onBack: () -> Unit) {
    var input by remember(category.id) { mutableStateOf(if (category.kind == ConversionKind.AWG) "14" else "1") }
    var fromUnit by remember(category.id) { mutableStateOf(category.defaultFrom) }
    var toUnit by remember(category.id) { mutableStateOf(category.defaultTo) }
    val context = LocalContext.current
    val output = remember(category.id, input, fromUnit, toUnit) {
        if (input.isBlank()) null else runCatching {
            val parsed = EngineeringUnitConverter.parseInput(input, category.id, fromUnit)
                ?: throw IllegalArgumentException(if (category.kind == ConversionKind.AWG && fromUnit == "AWG") "Enter a valid AWG size, such as 14 or 2/0." else "Enter a valid number.")
            val converted = EngineeringUnitConverter.convert(parsed, category.id, fromUnit, toUnit)
            EngineeringUnitConverter.formatValue(converted, category.id, toUnit)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(category.description, style = MaterialTheme.typography.bodySmall, color = ConverterMuted)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("From", style = MaterialTheme.typography.labelLarge, color = ConverterMuted, modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(if (fromUnit == "AWG") "e.g. 14 or 2/0" else "Enter value") },
                    keyboardOptions = KeyboardOptions(keyboardType = if (category.kind == ConversionKind.AWG && fromUnit == "AWG") KeyboardType.Text else KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )
                UnitSelector(selected = fromUnit, options = category.units.map { it.symbol }, onSelect = { fromUnit = it })
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                FilledTonalIconButton(onClick = {
                    val currentResult = output?.getOrNull()
                    if (currentResult != null) input = "$currentResult"
                    val oldFrom = fromUnit
                    fromUnit = toUnit
                    toUnit = oldFrom
                }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = ConverterPale, contentColor = ConverterOrange)) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap units")
                }
            }
            Text("To", style = MaterialTheme.typography.labelLarge, color = ConverterMuted, modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
            UnitSelector(selected = toUnit, options = category.units.map { it.symbol }, onSelect = { toUnit = it }, modifier = Modifier.fillMaxWidth())
            Card(Modifier.fillMaxWidth().padding(top = 16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = ConverterPale)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Result", color = ConverterOrange, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val value = output?.getOrNull()
                            if (value != null) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("CircuitCalc conversion", "$value $toUnit"))
                                Toast.makeText(context, "Copied result", Toast.LENGTH_SHORT).show()
                            }
                        }, enabled = output?.isSuccess == true) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy result", tint = ConverterOrange) }
                    }
                    when {
                        output == null -> Text("Enter a value to convert.", color = ConverterMuted)
                        output.isFailure -> Text(output.exceptionOrNull()?.message ?: "Unable to convert this value.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        else -> Text("${output.getOrThrow()} $toUnit", color = ConverterOrange, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    if (category.kind == ConversionKind.AWG && toUnit == "AWG") {
                        Text("Diameter converts to the nearest standard nominal AWG size.", color = ConverterMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (category.kind == ConversionKind.TEMPERATURE) {
                Text("Temperatures below absolute zero are rejected.", color = ConverterMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 9.dp))
            }
            if (category.kind == ConversionKind.AWG) {
                Text("Nominal solid conductor diameters, AWG 4/0 through 40. Real conductor diameter varies with manufacturing tolerance and strand construction.", color = ConverterMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 9.dp))
            }
        }
    }
}

@Composable
private fun UnitSelector(selected: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Text(selected, color = ConverterInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("  ▾", color = ConverterMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { unit ->
                DropdownMenuItem(text = { Text(unit) }, onClick = { onSelect(unit); expanded = false })
            }
        }
    }
}
