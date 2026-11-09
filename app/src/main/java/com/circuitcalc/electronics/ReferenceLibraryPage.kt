package com.circuitcalc.electronics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitcalc.electronics.domain.ReferenceCatalog
import com.circuitcalc.electronics.domain.ReferenceCategory
import com.circuitcalc.electronics.domain.ReferenceSection

@Composable
fun ReferenceLibraryPage(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = selectedId?.let(ReferenceCatalog::category)
    if (selected != null) {
        ReferenceDetailPage(selected, onBack = { selectedId = null })
    } else {
        val matches = ReferenceCatalog.search(query)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Column(Modifier.weight(1f)) {
                    Text("References", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Offline electronics reference library", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                placeholder = { Text("Search formulas, codes, packages…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Orange) },
                shape = RoundedCornerShape(16.dp)
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    Text("Reference categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 3.dp))
                }
                items(matches, key = { it.id }) { category ->
                    ReferenceCategoryCard(category) { selectedId = category.id }
                }
                if (matches.isEmpty()) item { Text("No matching reference topics", color = Muted, modifier = Modifier.padding(12.dp)) }
            }
        }
    }
}

@Composable
private fun ReferenceCategoryCard(category: ReferenceCategory, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), border = androidx.compose.foundation.BorderStroke(0.7.dp, SubtleBorder), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(PaleOrange), contentAlignment = Alignment.Center) {
                Text(categoryMark(category.id), color = Orange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text(category.summary, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${category.sections.size} topics · Offline", style = MaterialTheme.typography.labelSmall, color = Orange, modifier = Modifier.padding(top = 4.dp))
            }
            Text("›", fontSize = 28.sp, color = Orange, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

private fun categoryMark(id: String) = when (id) {
    "formulas" -> "ƒx"
    "resistors" -> "R"
    "capacitors" -> "C"
    "logic" -> "Y"
    "symbols" -> "⌁"
    "si" -> "10ⁿ"
    "packages" -> "IC"
    else -> "↗"
}

@Composable
private fun ReferenceDetailPage(category: ReferenceCategory, onBack: () -> Unit) {
    var expandedId by remember(category.id) { mutableStateOf(category.sections.firstOrNull()?.id) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back to references") }
            Column(Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Offline content · ${category.sections.size} topics", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item {
                Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = PaleOrange)) {
                    Text(category.summary, modifier = Modifier.fillMaxWidth().padding(14.dp), color = Ink, style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(category.sections, key = { it.id }) { section ->
                val expanded = section.id == expandedId
                ReferenceSectionCard(section, expanded) { expandedId = if (expanded) null else section.id }
            }
            if (category.id == "symbols") item {
                Text("Convention note: the most visible IEC/ANSI distinction is the fixed-resistor drawing (rectangle versus zigzag). Other symbols shown here are simplified; use the convention stated by the schematic.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            }
            if (category.id == "datasheets") item {
                Text("Reference text is stored in the app. Datasheets are external documents and need an internet connection to open.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ReferenceSectionCard(section: ReferenceSection, expanded: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), border = androidx.compose.foundation.BorderStroke(0.6.dp, SubtleBorder)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (section.symbolId != null) {
                    CircuitSymbolVector(section.symbolId, Modifier.size(width = 54.dp, height = 40.dp))
                    Spacer(Modifier.width(10.dp))
                } else if (section.gate != null) {
                    LogicGateVector(section.title.removeSuffix(" gate"), Modifier.size(width = 54.dp, height = 40.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                    Text(section.summary, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse" else "Expand", tint = Orange)
            }
            if (expanded) {
                HorizontalDivider(color = SubtleBorder)
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.body.forEach { paragraph -> Text(paragraph, color = Ink, style = MaterialTheme.typography.bodyMedium) }
                    if (section.tableRows.isNotEmpty()) ReferenceTable(section)
                    section.source?.let { ReferenceSourceButton(it.label, it.manufacturer, it.url) }
                }
            }
        }
    }
}

@Composable
private fun ReferenceTable(section: ReferenceSection) {
    val largeCell = section.tableRows.size == 1 && section.tableRows.first().any { it.length > 120 }
    if (largeCell) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PaleOrange).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            section.tableHeaders.forEach { Text(it, color = Orange, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            section.tableRows.flatten().forEach { Text(it, color = Ink, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp) }
        }
    } else {
        Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(10.dp)).background(PaleOrange).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                section.tableHeaders.forEach { Text(it, modifier = Modifier.widthIn(min = 52.dp).padding(vertical = 4.dp), color = Orange, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
            }
            section.tableRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Top) {
                    row.forEach { cell ->
                        Text(cell, modifier = Modifier.widthIn(min = 52.dp, max = 230.dp).padding(vertical = 4.dp), color = Ink, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceSourceButton(label: String, manufacturer: String?, url: String) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Online source${manufacturer?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.weight(1f))
        TextButton(onClick = { runCatching { uriHandler.openUri(url) } }, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(label, color = Orange, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(3.dp))
            Icon(Icons.Default.OpenInNew, contentDescription = "Opens online", tint = Orange, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LogicGateVector(gate: String, modifier: Modifier = Modifier) {
    val strokeColor = Orange
    Canvas(modifier) {
        val sx = size.width / 100f
        val sy = size.height / 60f
        val stroke = Stroke(width = 2.5.dp.toPx())
        val gateName = gate.uppercase()
        drawLine(strokeColor, Offset(0f, 19f * sy), Offset(24f * sx, 19f * sy), stroke.width)
        if (gateName != "NOT") drawLine(strokeColor, Offset(0f, 41f * sy), Offset(24f * sx, 41f * sy), stroke.width)
        val body = Path().apply {
            when (gateName) {
                "AND", "NAND" -> { moveTo(24f*sx, 8f*sy); lineTo(48f*sx, 8f*sy); quadraticTo(76f*sx, 8f*sy, 76f*sx, 30f*sy); quadraticTo(76f*sx, 52f*sy, 48f*sx, 52f*sy); lineTo(24f*sx, 52f*sy); close() }
                "OR", "NOR", "XOR", "XNOR" -> { moveTo(24f*sx, 8f*sy); quadraticTo(42f*sx, 30f*sy, 24f*sx, 52f*sy); quadraticTo(54f*sx, 49f*sy, 76f*sx, 30f*sy); quadraticTo(54f*sx, 11f*sy, 24f*sx, 8f*sy); close() }
                else -> { moveTo(24f*sx, 8f*sy); lineTo(55f*sx, 30f*sy); lineTo(24f*sx, 52f*sy); close() }
            }
        }
        drawPath(body, strokeColor, style = stroke)
        if (gateName == "XOR" || gateName == "XNOR") {
            val extra = Path().apply { moveTo(17f*sx, 8f*sy); quadraticTo(35f*sx, 30f*sy, 17f*sx, 52f*sy) }
            drawPath(extra, strokeColor, style = stroke)
        }
        val bubble = gateName == "NAND" || gateName == "NOR" || gateName == "XNOR" || gateName == "NOT"
        val outputX = if (gateName == "NOT") 61f else 76f
        if (bubble) drawCircle(strokeColor, 3.3f * sx, Offset(outputX * sx, 30f * sy), style = stroke)
        drawLine(strokeColor, Offset((outputX + if (bubble) 4f else 0f) * sx, 30f * sy), Offset(100f*sx, 30f*sy), stroke.width)
    }
}

@Composable
private fun CircuitSymbolVector(symbolId: String, modifier: Modifier = Modifier) {
    val symbolColor = Orange
    Canvas(modifier) {
        val sx = size.width / 100f
        val sy = size.height / 60f
        val stroke = Stroke(width = 2.5.dp.toPx())
        val c = symbolColor
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(c, Offset(x1*sx, y1*sy), Offset(x2*sx, y2*sy), stroke.width)
        when (symbolId) {
            "resistor" -> {
                line(0f,30f,22f,30f); line(78f,30f,100f,30f)
                val p = Path().apply { moveTo(22f*sx,18f*sy); lineTo(78f*sx,18f*sy); lineTo(78f*sx,42f*sy); lineTo(22f*sx,42f*sy); close() }
                drawPath(p,c,style=stroke)
            }
            "capacitor" -> { line(0f,30f,43f,30f); line(43f,10f,43f,50f); line(57f,10f,57f,50f); line(57f,30f,100f,30f) }
            "inductor" -> { line(0f,30f,15f,30f); repeat(4) { i -> drawArc(c, 180f, 180f, false, Offset((15f+i*17f)*sx, 16f*sy), Size(17f*sx,28f*sy), style=stroke) }; line(83f,30f,100f,30f) }
            "diode", "led" -> {
                line(0f,30f,28f,30f); line(72f,30f,100f,30f)
                val p=Path().apply { moveTo(30f*sx,12f*sy); lineTo(30f*sx,48f*sy); lineTo(67f*sx,30f*sy); close() }
                drawPath(p,c,style=stroke); line(69f,10f,69f,50f)
                if(symbolId=="led") { line(51f,12f,68f,2f); line(62f,2f,68f,2f); line(68f,2f,68f,8f); line(59f,24f,77f,14f); line(71f,14f,77f,14f); line(77f,14f,77f,20f) }
            }
            "transistor" -> {
                drawCircle(c, 21f*sx, Offset(52f*sx,30f*sy), style=stroke)
                line(0f,30f,31f,30f); line(31f,15f,31f,45f); line(31f,19f,47f,19f); line(47f,19f,67f,6f); line(31f,41f,47f,41f); line(47f,41f,67f,54f)
                line(59f,49f,67f,54f); line(62f,41f,67f,54f); line(67f,6f,82f,6f); line(67f,54f,82f,54f)
            }
            "ground" -> { line(50f,5f,50f,23f); line(24f,24f,76f,24f); line(32f,34f,68f,34f); line(41f,44f,59f,44f) }
            "battery" -> { line(0f,30f,32f,30f); line(32f,8f,32f,52f); line(42f,17f,42f,43f); line(58f,17f,58f,43f); line(68f,8f,68f,52f); line(68f,30f,100f,30f) }
            "switch" -> { line(0f,38f,32f,38f); line(68f,38f,100f,38f); drawCircle(c,3f*sx,Offset(32f*sx,38f*sy)); drawCircle(c,3f*sx,Offset(68f*sx,38f*sy)); line(35f,36f,61f,16f) }
            "transformer" -> {
                line(0f,30f,14f,30f); line(86f,30f,100f,30f); line(49f,5f,49f,55f); line(55f,5f,55f,55f)
                repeat(3) { i -> drawArc(c, 270f, 180f, false, Offset((9f+i*12f)*sx,(9f+i*14f)*sy),Size(22f*sx,15f*sy),style=stroke); drawArc(c,90f,180f,false,Offset((57f+i*12f)*sx,(9f+i*14f)*sy),Size(22f*sx,15f*sy),style=stroke) }
            }
        }
    }
}
