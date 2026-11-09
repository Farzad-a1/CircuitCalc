package com.circuitcalc.electronics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circuitcalc.electronics.domain.Calculator
import com.circuitcalc.electronics.domain.CalculatorEngine
import com.circuitcalc.electronics.domain.FieldInputType
import com.circuitcalc.electronics.domain.UnitConversion

class MainActivity : ComponentActivity() {
    private val model: CircuitCalcViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CircuitCalcApp(model) }
    }
}

@Composable private fun CircuitCalcApp(model: CircuitCalcViewModel) {
    val dark by model.darkMode.collectAsStateWithLifecycle()
    val palette = if (dark) CircuitCalcDarkPalette else CircuitCalcLightPalette
    val scheme = if (dark) darkColorScheme(
        primary = palette.primary, onPrimary = Color(0xFF321707), primaryContainer = palette.iconContainer,
        onPrimaryContainer = palette.onSurface, secondary = palette.secondary, onSecondary = Color(0xFF202024),
        background = palette.background, onBackground = palette.onSurface, surface = palette.surface,
        onSurface = palette.onSurface, surfaceVariant = Color(0xFF303034), onSurfaceVariant = palette.secondary,
        surfaceDim = palette.background, surfaceBright = palette.surface,
        surfaceContainerLowest = palette.background, surfaceContainerLow = palette.surface,
        surfaceContainer = palette.surface, surfaceContainerHigh = palette.surface,
        surfaceContainerHighest = palette.surface,
        outline = palette.outline, error = Color(0xFFFFB4AB)
    ) else lightColorScheme(
        primary = palette.primary, background = palette.background, surface = palette.surface,
        onBackground = palette.onSurface, onSurface = palette.onSurface, secondary = palette.secondary,
        outline = palette.outline
    )
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? ComponentActivity)?.window
        window?.statusBarColor = palette.background.toArgb()
        window?.navigationBarColor = palette.bottomNavigation.toArgb()
        window?.let { WindowCompat.getInsetsController(it, it.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        } }
    }
    CompositionLocalProvider(LocalCircuitCalcPalette provides palette) {
        MaterialTheme(colorScheme = scheme) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { CircuitRoot(model) }
        }
    }
}

@Composable private fun CircuitRoot(model: CircuitCalcViewModel) {
    var tab by remember { mutableStateOf("Home") }; var selected by remember { mutableStateOf<Calculator?>(null) }; var search by remember { mutableStateOf("") }
    val favorites by model.favorites.collectAsStateWithLifecycle(); val saved by model.saved.collectAsStateWithLifecycle(); val dark by model.darkMode.collectAsStateWithLifecycle(); val context=LocalContext.current
    Scaffold(containerColor=MaterialTheme.colorScheme.background, bottomBar={ if(selected==null) BottomBar(tab){tab=it} }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (selected != null) CalculatorPage(selected!!, favorites.contains(selected!!.name), { model.toggleFavorite(selected!!.name) }, { values,result-> model.save(selected!!.name,values,result);Toast.makeText(context,"Saved calculation",Toast.LENGTH_SHORT).show() }, {selected=null})
            else when(tab) {
                "Home" -> HomePage(onCategory={tab=it}, onTool={selected=it}, onAll={tab="Tools"}, query=search, onQuery={search=it}, favorites=favorites)
                "Tools" -> ToolsPage(query=search,onQuery={search=it},favorites=favorites,onTool={selected=it},onCategory={tab=it})
                "Saved" -> SavedPage(saved=saved,favorites=favorites,onDelete={model.delete(it)},onTool={name->selected=CalculatorEngine.calculators.find{it.name==name}},onFavorite={model.toggleFavorite(it)})
                "Pinouts" -> PinoutLibraryPage(onBack={tab="Tools"})
                "Converters" -> ConvertersPage(onBack={tab="Tools"},onTool={selected=it})
                "References" -> ReferenceLibraryPage(onBack={tab="Tools"})
                "Components","Calculators" -> LibraryPage(tab,onTool={selected=it},onBack={tab="Tools"})
                else -> SettingsPage(dark=dark,onDark={model.setDark(it)})
            }
        }
    }
}

@Composable private fun BottomBar(current:String,onSelect:(String)->Unit) {
    NavigationBar(containerColor=LocalCircuitCalcPalette.current.bottomNavigation,windowInsets=NavigationBarDefaults.windowInsets) {
        listOf("Home" to Icons.Default.Home,"Tools" to Icons.Default.Apps,"Saved" to Icons.Default.BookmarkBorder,"Settings" to Icons.Default.Settings).forEach { (name,icon)->
            NavigationBarItem(selected=current==name,onClick={onSelect(name)},icon={Icon(icon,null)},label={Text(name)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Orange,selectedTextColor=Orange,unselectedIconColor=Muted,unselectedTextColor=Muted,indicatorColor=PaleOrange))
        }
    }
}

@Composable private fun SearchHeader(title:String,query:String,onQuery:(String)->Unit,showSearch:Boolean=true) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
        if(expanded) OutlinedTextField(query,onQuery,Modifier.weight(1f),singleLine=true,placeholder={Text("Search tools")},leadingIcon={Icon(Icons.Default.Search,null)})
        else Column(Modifier.weight(1f)) {
            if(title=="CircuitCalc") Text(buildAnnotatedString { pushStyle(SpanStyle(color=Ink));append("Circuit");pushStyle(SpanStyle(color=Orange));append("Calc");pop();pop() },fontSize=28.sp,fontWeight=FontWeight.Bold,lineHeight=32.sp)
            else Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onBackground)
            if(title=="CircuitCalc")Text("Electronics Tools & Calculators",fontSize=15.sp,color=Muted,lineHeight=20.sp)
        }
        if(showSearch) IconButton(onClick={expanded=!expanded;if(!expanded)onQuery("")}){Icon(if(expanded)Icons.Default.Close else Icons.Default.Search,null,tint=Ink)}
    }
}

@Composable private fun HomePage(onCategory:(String)->Unit,onTool:(Calculator)->Unit,onAll:()->Unit,query:String,onQuery:(String)->Unit,favorites:Set<String>) {
    val popular = if(query.isBlank()) listOf("Ohm’s Law","LED Series Resistor","Voltage Divider") else CalculatorEngine.calculators.filter{it.name.contains(query,true)}.map{it.name}
    Column(Modifier.fillMaxSize()) { SearchHeader("CircuitCalc",query,onQuery)
        LazyColumn(contentPadding=PaddingValues(start=16.dp,end=16.dp,top=14.dp,bottom=16.dp)) {
            item { val categories=listOf("Calculators" to Icons.Default.Calculate,"Components" to Icons.Default.Memory,"Pinouts" to Icons.Default.SettingsInputComponent,"References" to Icons.Default.MenuBook,"Converters" to Icons.Default.SwapHoriz,"Tools" to Icons.Default.Build)
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { categories.chunked(3).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { row.forEach { (name,icon)-> Card(onClick={onCategory(name)},modifier=Modifier.weight(1f).height(110.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=CardWhite),border=BorderStroke(0.7.dp,SubtleBorder),elevation=CardDefaults.cardElevation(defaultElevation=2.dp,pressedElevation=1.dp)) { Column(Modifier.fillMaxSize().padding(vertical=8.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) { Box(Modifier.size(56.dp).clip(RoundedCornerShape(22.dp)).background(PaleOrange),contentAlignment=Alignment.Center){Icon(icon,null,tint=Orange,modifier=Modifier.size(34.dp))};Spacer(Modifier.height(7.dp));Text(name,fontSize=15.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis) } } } } } }
            }
            item { Row(Modifier.fillMaxWidth().padding(top=22.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically) { Text("Popular Tools",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f));TextButton(onClick=onAll,contentPadding=PaddingValues(horizontal=4.dp)){Text("See All ›",fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=Orange)} } }
            items(popular,key={it}) { name -> val calc=CalculatorEngine.calculators.firstOrNull{it.name==name};if(calc!=null) HomePopularToolCard(calc){onTool(calc)} else Text("No tools found",color=Muted,modifier=Modifier.padding(vertical=12.dp)) }
        }
    }
}

@Composable private fun ToolsPage(query:String,onQuery:(String)->Unit,favorites:Set<String>,onTool:(Calculator)->Unit,onCategory:(String)->Unit) {
    var category by remember { mutableStateOf("All") }
    val cats=listOf("All","Calculators","Components","Converters")
    val filtered=CalculatorEngine.calculators.filter { (category=="All"||it.category==category) && it.name.contains(query,true) }
    Column { SearchHeader("Tools",query,onQuery); ScrollableTabRow(selectedTabIndex=cats.indexOf(category),containerColor=MaterialTheme.colorScheme.surface,edgePadding=12.dp) { cats.forEach { name->Tab(selected=category==name,onClick={category=name},text={Text(name)},selectedContentColor=Orange) } }
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(filtered) { calc->ToolRow(calc,favorites.contains(calc.name)){onTool(calc)} }
            item { Spacer(Modifier.height(10.dp));SectionTitle("Reference libraries") }
            items(listOf("Components","Pinouts","References","Converters")) { name->ToolRow(name,null){onCategory(name)} }
        }
    }
}

@Composable private fun ToolRow(calc:Calculator,isFavorite:Boolean,onClick:()->Unit) { ToolRow(calc.name,if(isFavorite)"★ ${calc.category}" else calc.category,onClick) }
@Composable private fun HomePopularToolCard(calc:Calculator,onClick:()->Unit) {
    val icon=when(calc.name){"LED Series Resistor"->Icons.Default.Waves;else->null}
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=CardWhite),border=BorderStroke(0.7.dp,SubtleBorder),elevation=CardDefaults.cardElevation(defaultElevation=2.dp,pressedElevation=1.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min=82.dp).padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(PaleOrange),contentAlignment=Alignment.Center) { if(icon==null)Text("Ω",color=Orange,fontSize=38.sp,fontWeight=FontWeight.Bold,lineHeight=40.sp) else Icon(icon,null,tint=Orange,modifier=Modifier.size(38.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(calc.name,fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Calculators",fontSize=15.sp,color=Muted) }
            Icon(Icons.Default.ChevronRight,null,tint=Ink,modifier=Modifier.size(28.dp))
        }
    }
}
@Composable private fun ToolRow(title:String,subtitle:String?,onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(PaleOrange),contentAlignment=Alignment.Center){Text(if(title.contains("Resistor"))"〰" else "Ω",color=Orange,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);if(subtitle!=null)Text(subtitle,color=Muted,style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.ChevronRight,null) }
    }
}

@Composable private fun CalculatorPage(calc:Calculator,favorite:Boolean,onFavorite:()->Unit,onSave:(Map<String,String>,String)->Unit,onBack:()->Unit) {
    val context=LocalContext.current; val values= remember(calc.name){mutableStateMapOf<String,String>().apply{calc.fields.forEach{put(it.name,it.initial)}}}; val units=remember(calc.name){mutableStateMapOf<String,String>().apply{calc.fields.forEach{put(it.name,it.unit)}}}; var result by remember(calc.name){mutableStateOf("")};var error by remember(calc.name){mutableStateOf("")}
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)};Text(calc.name,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);IconButton(onClick=onFavorite){Icon(if(favorite)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=Orange)}}
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=16.dp)) {
            Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=PaleOrange),modifier=Modifier.fillMaxWidth().padding(bottom=14.dp)){Column(Modifier.padding(16.dp)){Text("Formula",color=Orange,fontWeight=FontWeight.Bold);Text(calc.formula,color=Ink,style=MaterialTheme.typography.bodyLarge)}}
            if (calc.name == "Resistor Color Code (5/6-band)") {
                val bandCount = values["Band count"]?.toIntOrNull() ?: 5
                ResistorColorPreview(calc.fields.filter { it.name.startsWith("Band ") && (it.name != "Band 6 TCR (ppm/K)" || bandCount == 6) }.map { values[it.name].orEmpty() })
            }
            calc.fields.filter { it.name != "Band 6 TCR (ppm/K)" || values["Band count"] == "6" }.forEach { field->
                val choices=UnitConversion.choices(field)
                Row(Modifier.fillMaxWidth().padding(bottom=9.dp),verticalAlignment=Alignment.CenterVertically) {
                    if (field.options.isNotEmpty()) {
                        Text(field.name,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=Ink)
                        var expanded by remember(calc.name,field.name){mutableStateOf(false)}
                        Box {
                            TextButton(onClick={expanded=true},contentPadding=PaddingValues(horizontal=8.dp)){Text(values[field.name].orEmpty(),color=Orange,maxLines=1);Icon(Icons.Default.ArrowDropDown,null,tint=Orange)}
                            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { field.options.forEach { option->DropdownMenuItem(text={Text(option)},onClick={values.apply{put(field.name,option)}.let{expanded=false;error=""}}) } }
                        }
                    } else if(field.inputType == FieldInputType.TEXT) {
                        OutlinedTextField(value=values[field.name].orEmpty(),onValueChange={values[field.name]=it;error=""},modifier=Modifier.fillMaxWidth(),label={Text(field.name)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text),shape=RoundedCornerShape(12.dp))
                    } else {
                        OutlinedTextField(value=values[field.name].orEmpty(),onValueChange={values[field.name]=it;error=""},modifier=Modifier.weight(1f),label={Text(field.name)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),shape=RoundedCornerShape(12.dp))
                    }
                    if(field.options.isEmpty() && field.inputType != FieldInputType.TEXT && choices.size>1) {
                        var expanded by remember(calc.name,field.name){mutableStateOf(false)}
                        Box {
                            TextButton(onClick={expanded=true},contentPadding=PaddingValues(horizontal=6.dp)){Text(units[field.name].orEmpty(),color=Orange,maxLines=1);Icon(Icons.Default.ArrowDropDown,null,tint=Orange)}
                            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { choices.forEach { choice->DropdownMenuItem(text={Text(choice.label)},onClick={units.apply{put(field.name,choice.label)}.let{expanded=false}}) } }
                        }
                    } else if(field.options.isEmpty() && field.inputType != FieldInputType.TEXT && field.unit.isNotBlank()) Text(field.unit,color=Muted,modifier=Modifier.padding(start=8.dp,end=4.dp))
                }
            }
            Button(onClick={try{
                val textValues=mutableMapOf<String,String>()
                val numeric=calc.fields.filter { it.name != "Band 6 TCR (ppm/K)" || values["Band count"] == "6" }.mapNotNull { field ->
                    val raw=values[field.name].orEmpty()
                    when {
                        field.options.isNotEmpty() || field.inputType == FieldInputType.TEXT -> { textValues[field.name]=raw; null }
                        raw.isBlank() && field.name.contains("optional",true) -> field.name to 0.0
                        raw.isBlank() -> null
                        else -> {val parsed=raw.toDoubleOrNull()?:throw IllegalArgumentException("Enter valid numbers for all filled fields.");field.name to UnitConversion.toEngineValue(parsed,field,units[field.name].orEmpty())}
                    }
                }.toMap()
                result=CalculatorEngine.calculate(calc.name,numeric,textValues);error=""
            }catch(e:Exception){error=e.message?:"Unable to calculate."}},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text("Calculate",fontWeight=FontWeight.Bold)}
            if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=8.dp))
            if(result.isNotBlank()) Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=PaleOrange),modifier=Modifier.fillMaxWidth().padding(top=14.dp)){Column(Modifier.padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Result",Modifier.weight(1f),color=Orange,fontWeight=FontWeight.Bold);IconButton(onClick={val clip=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;clip.setPrimaryClip(ClipData.newPlainText("CircuitCalc result",result));Toast.makeText(context,"Copied result",Toast.LENGTH_SHORT).show()}){Icon(Icons.Default.ContentCopy,null,tint=Orange)}};Text(result,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Orange);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={onSave(values.toMap(),result)}){Icon(Icons.Default.BookmarkBorder,null);Text(" Save")}}} }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={calc.fields.forEach{values[it.name]=it.initial;units[it.name]=it.unit};result="";error=""},modifier=Modifier.weight(1f).height(50.dp)){Icon(Icons.Default.Refresh,null);Text(" Reset")};Button(onClick={if(result.isNotBlank())onSave(values.mapValues{(name,raw)->"$raw ${units[name].orEmpty()}"},result)else Toast.makeText(context,"Calculate first",Toast.LENGTH_SHORT).show()},modifier=Modifier.weight(1f).height(50.dp),shape=RoundedCornerShape(14.dp)){Icon(Icons.Default.BookmarkBorder,null);Text(" Save")}}
    }
}

@Composable private fun ResistorColorPreview(bands:List<String>) {
    val colors = mapOf("Black" to Color(0xFF202124), "Brown" to Color(0xFF795548), "Red" to Color(0xFFD32F2F), "Orange" to Color(0xFFEF6C00), "Yellow" to Color(0xFFFBC02D), "Green" to Color(0xFF388E3C), "Blue" to Color(0xFF1976D2), "Violet" to Color(0xFF7B1FA2), "Gray" to Color(0xFF9E9E9E), "White" to Color(0xFFF5F5F5), "Gold" to Color(0xFFD4AF37), "Silver" to Color(0xFFB0BEC5), "Pink" to Color(0xFFEC407A))
    Canvas(Modifier.fillMaxWidth().height(74.dp).padding(vertical=4.dp)) {
        val centerY = size.height / 2
        drawLine(Color(0xFF9E9E9E), Offset(size.width * .05f, centerY), Offset(size.width * .95f, centerY), strokeWidth = 6.dp.toPx())
        val bodyLeft = size.width * .18f
        val bodyWidth = size.width * .64f
        val bodyHeight = size.height * .68f
        drawRoundRect(Color(0xFFD9A56D), Offset(bodyLeft, centerY - bodyHeight / 2), Size(bodyWidth, bodyHeight), cornerRadius = CornerRadius(bodyHeight * .45f))
        val shown = bands.take(6)
        shown.forEachIndexed { index, name ->
            val x = bodyLeft + bodyWidth * (.18f + index * (.64f / maxOf(1, shown.size - 1)))
            drawRect(colors[name] ?: Color.DarkGray, Offset(x, centerY - bodyHeight * .42f), Size(8.dp.toPx(), bodyHeight * .84f))
        }
    }
}

@Composable private fun SavedPage(saved:List<com.circuitcalc.electronics.data.local.SavedCalculation>,favorites:Set<String>,onDelete:(Long)->Unit,onTool:(String)->Unit,onFavorite:(String)->Unit) {
    var mode by remember { mutableStateOf("Calculations") }
    Column { SearchHeader("Saved", "", {}, false);Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Calculations","Favorites").forEach{ FilterChip(selected=mode==it,onClick={mode=it},label={Text(it)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Orange,selectedLabelColor=MaterialTheme.colorScheme.onPrimary)) }}
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(mode=="Calculations") items(saved){item->Card(shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).clickable{onTool(item.calculator)}){Text(item.calculator,fontWeight=FontWeight.Bold);Text(item.result,color=Orange);Text(item.inputs,color=Muted,style=MaterialTheme.typography.bodySmall)};IconButton(onClick={onDelete(item.id)}){Icon(Icons.Default.DeleteOutline,null)}}}}
            else items(favorites.toList()){name->ToolRow(name,"Favorite") { onTool(name) } }
            if((mode=="Calculations"&&saved.isEmpty())||(mode=="Favorites"&&favorites.isEmpty()))item{Text("Nothing saved yet",color=Muted,modifier=Modifier.padding(16.dp))}
        }
    }
}

@Composable private fun SettingsPage(dark:Boolean,onDark:(Boolean)->Unit) {
    Column(Modifier.fillMaxSize()) { SearchHeader("Settings","",{},false);Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("Theme");Card(shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(16.dp)){Text("Appearance",fontWeight=FontWeight.Bold);Row(verticalAlignment=Alignment.CenterVertically){Text("Dark mode",Modifier.weight(1f));Switch(dark,{onDark(it)},colors=SwitchDefaults.colors(checkedThumbColor=MaterialTheme.colorScheme.onPrimary,checkedTrackColor=Orange))}}}
        SectionTitle("About");Card(shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(16.dp)){Text("CircuitCalc",fontWeight=FontWeight.Bold);Text("Electronics tools and calculators · v1.0",color=Muted);Text("All calculators work offline. Confirm component ratings and datasheets before building circuits.",color=Muted,modifier=Modifier.padding(top=8.dp))}}
    } }
}

private data class ReferenceItem(val name:String,val subtitle:String,val details:String,val source:String="")
@Composable private fun LibraryPage(section:String,onTool:(Calculator)->Unit,onBack:()->Unit) {
    var selectedReference by remember { mutableStateOf<String?>(null) }
    var selectedComponent by remember { mutableStateOf<String?>(null) }
    val components=listOf("Resistors" to "Color bands, E-series, SMD codes and resistor networks","Capacitors" to "Series/parallel networks, energy and charge relationships","Inductors" to "Reactance, RLC impedance and ideal component relationships","Diodes" to "LED and diode string resistor calculators","Transistors" to "BJT calculations","MOSFETs" to "Conduction loss calculation","Op-Amps" to "Ideal inverting and non-inverting gain","ICs" to "555 monostable/astable and op-amp tools","Sensors" to "Check exact manufacturer datasheets","Power Devices" to "Check exact manufacturer datasheets")
    val references=listOf("Ohm’s Law & Formulas" to "Equations used by the 40 offline calculators", "Electronic Formulas" to "Ideal component and circuit equations", "Components Table" to "Ratings and tolerances", "SMD Package Codes" to "Markings are manufacturer-specific", "Logic Gates" to "Logic gate behavior", "SI Units & Prefixes" to "Metric prefixes", "Circuit Symbols" to "IEC and ANSI conventions", "Verified datasheet links" to "Arduino, Espressif, Microchip and Texas Instruments references")
    val referenceDetails=mapOf(
        "Ohm’s Law & Formulas" to "V = I × R.\nP = V × I = I²R = V²/R.\nEnter two known values to solve for the third; the app reports voltage, current, resistance and power.",
        "Electronic Formulas" to "RC time constant: τ = RC.\nInductive reactance: XL = 2πfL.\nCapacitive reactance: XC = 1/(2πfC).\nLC resonance: f₀ = 1/(2π√LC).",
        "Components Table" to "Component ratings depend on the exact manufacturer part and package. Use the device datasheet for tolerances, voltage/current limits, temperature ratings, and derating.",
        "SMD Package Codes" to "Top markings are not globally unique. Verify the manufacturer, package, and full part number before relying on an SMD marking.",
        "Logic Gates" to "AND: output 1 only when both inputs are 1. OR: output 1 when either input is 1. NOT: output is the inverse. XOR: output 1 when inputs differ. NAND and NOR are the inverses of AND and OR.",
        "SI Units & Prefixes" to "pico (p) 10⁻¹²; nano (n) 10⁻⁹; micro (µ) 10⁻⁶; milli (m) 10⁻³; kilo (k) 10³; mega (M) 10⁶; giga (G) 10⁹.",
        "Circuit Symbols" to "Common symbols vary between IEC and ANSI drawing conventions. Confirm the standard used by the schematic before interpreting a symbol.",
        "Verified datasheet links" to "Arduino Uno Rev3: https://docs.arduino.cc/hardware/uno-rev3\n\nESP32-DevKitC V4: https://docs.espressif.com/projects/esp-dev-kits/en/latest/esp32/esp32-devkitc/user_guide.html\n\nATmega328P-PU: https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-7810-Automotive-Microcontrollers-ATmega328P_Datasheet.pdf\n\nTI NE555: https://www.ti.com/lit/ds/symlink/ne555.pdf"
    )
    val relatedComponents=mapOf("Resistors" to listOf("Resistor Color Code (4-band)","Resistor Color Code (5/6-band)","SMD Resistor Decoder","Nearest E-Series Resistor","Series / Parallel Resistors","Resistor Power Rating"),"Capacitors" to listOf("Capacitor from Charge","Capacitor Energy","Parallel Capacitors","Series Capacitors","RC Filter Cutoff","AC RLC Impedance (Series)"),"Inductors" to listOf("Inductive Reactance","RC / RL Time Constant","LC Resonant Frequency","AC RLC Impedance (Series)"),"Diodes" to listOf("LED Series Resistor","Diode String Resistor","Zener Series Resistor"),"Transistors" to listOf("BJT Base Resistor"),"MOSFETs" to listOf("MOSFET Conduction Loss"),"Op-Amps" to listOf("Op-Amp Non-Inverting Gain","Inverting Op-Amp Calculator"),"ICs" to listOf("555 Astable Frequency","555 Monostable Calculator","Op-Amp Non-Inverting Gain","Inverting Op-Amp Calculator"))
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)};Text(section,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        if(section=="Calculators") LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(CalculatorEngine.calculators.filter{it.category=="Calculators"}){calc->ToolRow(calc,false){onTool(calc)}}}
        else if(section=="Components") LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(components){(name,desc)->ToolRow(name,desc){selectedComponent=name}}}
        else LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(references){(name,desc)->ToolRow(name,desc){selectedReference=name}}}
    }
    selectedReference?.let { title -> AlertDialog(onDismissRequest={selectedReference=null},title={Text(title,fontWeight=FontWeight.Bold)},text={Text(referenceDetails[title].orEmpty())},confirmButton={TextButton(onClick={selectedReference=null}){Text("Close",color=Orange)}}) }
    selectedComponent?.let { category ->
        val tools=relatedComponents[category].orEmpty().mapNotNull { name -> CalculatorEngine.calculators.firstOrNull{it.name==name} }
        AlertDialog(onDismissRequest={selectedComponent=null},title={Text(category,fontWeight=FontWeight.Bold)},text={if(tools.isEmpty())Text("Check the exact component datasheet for pin functions, ratings, and operating limits. This offline catalog does not provide unverified device-specific data.") else Column { Text("Related tools",color=Muted,modifier=Modifier.padding(bottom=6.dp));tools.forEach { tool->TextButton(onClick={selectedComponent=null;onTool(tool)},contentPadding=PaddingValues(vertical=4.dp)){Text(tool.name,color=Orange)} } }},confirmButton={TextButton(onClick={selectedComponent=null}){Text("Close",color=Orange)}})
    }
}

@Composable private fun SectionTitle(text:String){Text(text,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=6.dp))}
