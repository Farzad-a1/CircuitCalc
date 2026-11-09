package com.circuitcalc.electronics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitcalc.electronics.domain.PinDiagram
import com.circuitcalc.electronics.domain.PinFunction
import com.circuitcalc.electronics.domain.PinRole
import com.circuitcalc.electronics.domain.PinoutCatalog
import com.circuitcalc.electronics.domain.PinoutCategory
import com.circuitcalc.electronics.domain.PinoutEntry

@Composable
fun PinoutLibraryPage(onBack: () -> Unit) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<PinoutCategory?>(null) }
    val entry = selectedId?.let { id -> PinoutCatalog.entries.firstOrNull { it.id == id } }
    if (entry != null) PinoutDetailPage(entry, onBack = { selectedId = null })
    else {
        val filtered = PinoutCatalog.search(query, category)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Pinouts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Verified offline component library", color = PinMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true, placeholder = { Text("Search component or part number") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PinOrange) },
                shape = RoundedCornerShape(16.dp)
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") }, colors = pinChipColors())
                PinoutCategory.entries.forEach { item ->
                    FilterChip(selected = category == item, onClick = { category = if (category == item) null else item }, label = { Text(item.label, maxLines = 1) }, colors = pinChipColors())
                }
            }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (filtered.isEmpty()) item { Text("No matching pinouts", color = PinMuted, modifier = Modifier.padding(16.dp)) }
                items(filtered, key = { it.id }) { item -> PinoutCard(item) { selectedId = item.id } }
            }
        }
    }
}

@Composable
private fun pinChipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = PinOrange, selectedLabelColor = MaterialTheme.colorScheme.onPrimary, selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary)

@Composable
private fun PinoutCard(entry: PinoutEntry, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), border = androidx.compose.foundation.BorderStroke(0.7.dp, PinLine), elevation = CardDefaults.cardElevation(2.dp)) {
        BoxWithConstraints {
            if (maxWidth < 390.dp) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(entry.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = PinInk)
                    Text(entry.packageOrRevision, color = PinMuted, style = MaterialTheme.typography.bodySmall)
                    Surface(color = PinPaleOrange, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 7.dp)) { Text(entry.category.label, color = PinOrange, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                    Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${entry.pinCount} pins", color = PinMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        PinoutDiagram(entry, preview = true, modifier = Modifier.width(94.dp).height(68.dp))
                    }
                }
            } else Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = PinInk)
                    Text(entry.packageOrRevision, color = PinMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = PinPaleOrange, shape = RoundedCornerShape(50)) { Text(entry.category.label, color = PinOrange, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                        Text("${entry.pinCount} pins", color = PinMuted, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.width(8.dp))
                PinoutDiagram(entry, preview = true, modifier = Modifier.width(94.dp).height(68.dp))
            }
        }
    }
}

@Composable
private fun PinoutDetailPage(entry: PinoutEntry, onBack: () -> Unit) {
    var pinQuery by remember(entry.id) { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val matchingPins = entry.pins.filter { pin -> pinQuery.isBlank() || pin.number.contains(pinQuery, true) || pin.label.contains(pinQuery, true) || pin.function.contains(pinQuery, true) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(entry.packageOrRevision, color = PinMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), border = androidx.compose.foundation.BorderStroke(0.7.dp, PinLine)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pinout diagram", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${entry.pinCount} pins", color = PinMuted, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(entry.diagramView, color = PinMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                        PinoutDiagram(entry, preview = false, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            RoleLegend("Power", PinOrange); RoleLegend("Ground", PinGround); RoleLegend("Signal", PinSignal)
                        }
                    }
                }
            }
            if (entry.cautions.isNotEmpty()) item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PinPaleOrange)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Electrical cautions", color = PinOrange, fontWeight = FontWeight.Bold)
                        entry.cautions.forEach { Text("• $it", color = PinInk, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                    }
                }
            }
            item {
                Text("Pin functions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = pinQuery, onValueChange = { pinQuery = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, placeholder = { Text("Search pin number or function") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = PinOrange) }, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            }
            items(matchingPins, key = { "${entry.id}-${it.header}-${it.number}" }) { pin -> PinFunctionRow(pin) }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), border = androidx.compose.foundation.BorderStroke(0.7.dp, PinLine)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Official documentation", fontWeight = FontWeight.Bold)
                        Text(entry.sourceName, color = PinMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp))
                        TextButton(onClick = { runCatching { uriHandler.openUri(entry.datasheetUrl) } }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                            Text("Open manufacturer document", color = PinOrange); Spacer(Modifier.width(5.dp)); Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PinOrange, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color)); Text(label, style = MaterialTheme.typography.labelSmall, color = PinMuted)
    }
}

@Composable
private fun PinFunctionRow(pin: PinFunction) {
    val tint = when (pin.role) { PinRole.POWER -> PinOrange; PinRole.GROUND -> PinGround; else -> PinInk }
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), border = androidx.compose.foundation.BorderStroke(0.6.dp, PinLine)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (pin.role == PinRole.SIGNAL || pin.role == PinRole.NC) PinPaleOrange else tint.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text(pin.number, color = tint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(buildString { if(pin.header.isNotBlank()) append("${pin.header} · "); append(pin.label) }, fontWeight = FontWeight.SemiBold, color = tint)
                Text(pin.function, color = PinMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (pin.role == PinRole.POWER || pin.role == PinRole.GROUND) Text(if (pin.role == PinRole.POWER) "POWER" else "GND", color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PinoutDiagram(entry: PinoutEntry, preview: Boolean, modifier: Modifier = Modifier) {
    when (entry.diagram) {
        PinDiagram.DIP -> DipDiagram(entry, preview, modifier)
        PinDiagram.TO220 -> ThreeLeadDiagram(entry, preview, modifier, "TO-220")
        PinDiagram.TO92 -> ThreeLeadDiagram(entry, preview, modifier, "TO-92")
        PinDiagram.BOARD -> BoardDiagram(entry, preview, modifier)
        PinDiagram.USB_A -> UsbDiagram(entry, preview, modifier)
    }
}

@Composable
private fun PinDot(pin: PinFunction, text: String, modifier: Modifier = Modifier) {
    val color = when (pin.role) { PinRole.POWER -> PinOrange; PinRole.GROUND -> PinGround; else -> PinSignal }
    Surface(modifier = modifier, color = color.copy(alpha = if(pin.role == PinRole.SIGNAL) 0.18f else 0.14f), shape = RoundedCornerShape(5.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.65f))) {
        Text(text, color = if(pin.role == PinRole.SIGNAL) PinInk else color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
    }
}

@Composable
private fun DipDiagram(entry: PinoutEntry, preview: Boolean, modifier: Modifier) {
    val half = entry.pins.size / 2
    val left = entry.pins.take(half)
    val right = entry.pins.drop(half).reversed()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(if (preview) 2.dp else 5.dp)) { left.forEach { PinDot(it, if(preview) it.number else "${it.number} ${it.label}", Modifier.widthIn(max = if(preview) 24.dp else 88.dp)) } }
        Column(Modifier.padding(horizontal = if(preview) 4.dp else 7.dp).width(if(preview) 18.dp else 40.dp).heightIn(min = if(preview) 48.dp else 100.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF30343A)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("◡", color = PinPaleOrange, fontSize = if(preview) 8.sp else 15.sp)
            if (!preview) Text(entry.partNumber, color = Color.White, fontSize = 9.sp, maxLines = 3)
        }
        Column(verticalArrangement = Arrangement.spacedBy(if (preview) 2.dp else 5.dp), horizontalAlignment = Alignment.End) { right.forEach { PinDot(it, if(preview) it.number else "${it.number} ${it.label}", Modifier.widthIn(max = if(preview) 24.dp else 88.dp)) } }
    }
}

@Composable
private fun ThreeLeadDiagram(entry: PinoutEntry, preview: Boolean, modifier: Modifier, bodyLabel: String) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = Color(0xFF30343A), shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 4.dp), modifier = Modifier.width(if(preview) 52.dp else 112.dp).height(if(preview) 30.dp else 62.dp)) {
            Box(contentAlignment = Alignment.Center) { if(!preview) Text(bodyLabel, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
        }
        Row(Modifier.width(if(preview) 76.dp else 150.dp), horizontalArrangement = Arrangement.SpaceBetween) { entry.pins.forEach { Text("│", color = PinGround, fontSize = 14.sp) } }
        Row(Modifier.width(if(preview) 108.dp else 216.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            entry.pins.forEach { pin -> PinDot(pin, if(preview) pin.number else "${pin.number} ${pin.label}") }
        }
        if(!preview) Text(if(entry.diagram == PinDiagram.TO220) "Metal tab: see pin table" else "Flat face orientation as documented", color = PinMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun BoardDiagram(entry: PinoutEntry, preview: Boolean, modifier: Modifier) {
    val grouped = entry.pins.groupBy { it.header }
    if(preview) {
        Column(modifier.background(PinDiagramInset, RoundedCornerShape(10.dp)).padding(5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${entry.pins.size} pins", color = PinOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { entry.pins.take(4).forEach { PinDot(it, it.label.take(3)) } }
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            grouped.entries.forEach { (header, pins) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(PinDiagramInset).padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(header, color = PinOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    pins.forEach { pin -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(pin.number, color = PinMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(22.dp))
                        PinDot(pin, pin.label, Modifier.weight(1f))
                    } }
                }
            }
        }
    }
}

@Composable
private fun UsbDiagram(entry: PinoutEntry, preview: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MATING FACE", color = PinMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF30343A)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            entry.pins.forEach { pin -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PinDot(pin, pin.number, Modifier.width(if(preview) 14.dp else 34.dp))
                if(!preview) Text(pin.label, color = Color.White, fontSize = 9.sp, maxLines = 1)
            } }
        }
        if(!preview) Text("Female receptacle · contact side facing viewer", color = PinMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    }
}
