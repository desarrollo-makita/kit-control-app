package com.makita.kitcontrolapp.ui.component

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyRow


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable


import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme


import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makita.kitcontrolapp.ui.theme.GreenMakita

import com.makita.ubiapp.KitItem
import com.makita.ubiapp.RetrofitClient
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


data class CodigoData(
    val item: String,
    val serieInicio: String,
    val serieHasta: String,
    val letraFabrica: String,
    val ean: String
)

@Composable
fun KitControlScreen() {
    var textoEscaneado by remember { mutableStateOf(TextFieldValue("")) }
    var clearRequested by rememberSaveable { mutableStateOf(false) }
    var codigoInvalido by remember { mutableStateOf(false) }
    val minCodigoLength = 55
    var showTable  by remember{ mutableStateOf(false) }

    var item by rememberSaveable { mutableStateOf("") }
    var serieInicio by rememberSaveable { mutableStateOf("") }
    var serieHasta by rememberSaveable { mutableStateOf("") }
    var letraFabrica by rememberSaveable {  mutableStateOf("") }
    var ean by rememberSaveable {  mutableStateOf("") }

    val listaCodigos = remember { mutableStateListOf<CodigoData>() }
    val listaKits = remember { mutableStateListOf<KitItem>() }

    var showCombo  by remember{ mutableStateOf(false) }
    val context = LocalContext.current



    var selectedKitData by remember { mutableStateOf<List<String>>(emptyList()) }
    // Valor seleccionado en el ComboBox
    var selectedItem by remember { mutableStateOf("") }

    fun onKitSelected(response: List<KitItem>) {
        // Mapeamos la lista para obtener solo el campo 'item'
        val simplifiedResponse = response.map { it.item }

        // Asignamos la lista de items
        selectedKitData = simplifiedResponse
        showCombo = true
        // Imprimimos solo los valores 'item' en el log
        Log.d("*MAKITA*", "Kit seleccionado --->cargo la lista: ${simplifiedResponse.joinToString(", ")}")
    }

    // Esto solo se ejecutará cuando el texto cambie realmente
    LaunchedEffect(textoEscaneado.text) {
        if (textoEscaneado.text.isNotEmpty()) {
            codigoInvalido = textoEscaneado.text.length != minCodigoLength
            Log.d("*MAKITA*", "[KitControlScreen] Se ingresa texto: ${textoEscaneado.text}")
            if (codigoInvalido) {
                textoEscaneado = TextFieldValue("")
            }else{
                val dataItem = parseCodigo(textoEscaneado.text)

                item =  dataItem.item
                serieInicio =  dataItem.serieInicio
                serieHasta =  dataItem.serieHasta
                letraFabrica =  dataItem.letraFabrica
                ean =  dataItem.ean

                Log.d("*MAKITA*", "[dataItem-selectedItem] $dataItem , $selectedItem")
                listaCodigos.add(dataItem)

                showTable = true
                textoEscaneado = TextFieldValue("")

                guardarCodigoEnArchivo(context, dataItem, selectedItem)

            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00909E),
                        Color(0xFF80CBC4)
                    )
                )
            )
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .background(Color.White, shape = RoundedCornerShape(30.dp)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Titulo()

            OutlinedTextField(
                value = textoEscaneado,
                onValueChange = { newTextFieldValue ->
                    Log.d("*MAKITA00*" , "newTextFieldValue $newTextFieldValue")
                    val nuevoTexto = newTextFieldValue.text.take(55).trim()

                    // Solo logea si el texto es diferente al anterior
                    if (nuevoTexto != textoEscaneado.text) {
                        Log.d("*MAKITA*", "[onValueChange] Nuevo valor recibido: $nuevoTexto")
                    }

                    // Actualiza el textoEscaneado con el nuevo valor
                    textoEscaneado = TextFieldValue(text = nuevoTexto)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                label = {
                    Text(
                        "Escanear Item",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenMakita
                        )
                    )
                },
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenMakita,
                    unfocusedBorderColor = GreenMakita,
                    focusedLabelColor = GreenMakita,
                    unfocusedLabelColor = GreenMakita,
                    cursorColor = GreenMakita
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Clear text",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                clearRequested = true
                            },
                        tint = GreenMakita
                    )
                }
            )

            if (codigoInvalido) {
                Text(
                    "Código incorrecto, intente nuevamente",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

            }

            if(showTable){
                MostrarListaDeCodigos(listaCodigos,
                    onKitSelected = { response ->
                        onKitSelected(response) // Actualiza el estado en el padre
                    })
            }

            if(showCombo){
                ClassicComboBox(
                    items = selectedKitData,  // La lista de elementos
                    selectedItem = selectedItem,  // El ítem seleccionado
                    onItemSelected = { item ->
                        selectedItem = item
                        Log.d("*MAKITA*" , "selectedItem  $selectedItem")// Actualiza el ítem seleccionado
                    }
                )
            }



            if (clearRequested) {
                textoEscaneado =  TextFieldValue("")
                clearRequested = false  // Evita que se repita el efecto

            }
        }
    }
}

@Composable
fun Titulo() {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KIT-CONTROL",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00909E),
            textAlign = TextAlign.Center
        )
        Divider(
            color = Color(0xFFFF7F50),
            thickness = 2.dp,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
    }
}

fun parseCodigo(texto: String): CodigoData {
    if (texto.length < 52) {
        throw IllegalArgumentException("El código escaneado es demasiado corto: ${texto.length} caracteres")
    }

    val item = texto.substring(0, 20).trim()
    val serieInicio = texto.substring(20, 29)
    val serieHasta = texto.substring(29, 38)
    val letraFabrica = texto.substring(38, 39)
    val ean = texto.substring(39, 52)

    return CodigoData(item, serieInicio, serieHasta, letraFabrica, ean)
}

@Composable
fun MostrarDatosTabla(
    itemPadre:Boolean,
    item: String,
    serieInicio: String,
    serieFinal: String,
    ean: String,
    onItemPadreSelected: (List<KitItem>) -> Unit // Función para manejar el cambio de selección
) {

    val coroutineScope = rememberCoroutineScope()
    var apiResponse by remember { mutableStateOf<List<KitItem>>(emptyList()) } // Estado para almacenar la respuesta de la API

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical =1.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        // Agregar el checkbox para "Item Padre", alineado junto a la fila
        Checkbox(
            checked = itemPadre,
            onCheckedChange = {
                coroutineScope.launch {
                    try {

                        val response = RetrofitClient.apiService.obtenerListaKit(item)
                        onItemPadreSelected(response) // Pasar la respuesta al padre
                    } catch (e: Exception) {
                        Log.e("*ERROR API*", "Error al obtener datos: ${e.message}")
                    }
                }
            },
            modifier = Modifier
                .width(130.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterVertically), // Alinea verticalmente con el contenido de la fila
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF008686), // Color verde "Green Makita" cuando está seleccionado
                uncheckedColor = Color.Gray, // Color para cuando no está seleccionado
                checkmarkColor = Color.White // Color blanco para la marca de verificación
            )
        )

        // Definir las filas de datos (sin mostrar el "Item Padre")
        val fields = listOf<String>(
            item,
            serieInicio,
            serieFinal,
            ean
        )

        // Mostrar los campos de la fila
        fields.forEachIndexed { index, field ->
            val textColor = Color.Red
            Text(
                text = field,
                color = textColor,
                modifier = Modifier
                    .width(130.dp)
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterVertically), // Alinea verticalmente con el checkbox
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
fun MostrarListaDeCodigos(listaCodigos: List<CodigoData> , onKitSelected: (List<KitItem>) -> Unit ) {
    // Usamos un mapa para asociar el item con su estado de selección

    val selectedItemIndex = remember { mutableStateOf<Int?>(null) }
    var apiData by remember { mutableStateOf<List<KitItem>>(emptyList()) }
   LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item {
            // Mostrar las cabeceras y los datos en una fila horizontal
            Column {
                // Mostrar las cabeceras
                MostrarCabeceras()

                // Mostrar los datos de los códigos
                listaCodigos.forEachIndexed { index, codigoData ->
                    val isSelected = selectedItemIndex.value == index

                    MostrarDatosTabla(
                        itemPadre = isSelected,
                        item = codigoData.item.trim(),
                        serieInicio = codigoData.serieInicio,
                        serieFinal = codigoData.serieHasta,
                        ean = codigoData.ean,
                        onItemPadreSelected = { response ->
                            // Si seleccionamos un ítem, actualizamos el índice seleccionado
                            selectedItemIndex.value = if (isSelected) null else index
                            Log.d("*MAKITA*", "Item seleccionado: ${codigoData.item.trim()}")
                            apiData = response
                            Log.d("*MAKITA*", "apiDatao: $apiData")
                            onKitSelected(apiData)

                        }
                    )

                }
            }
        }
    }
}
@Composable
fun MostrarCabeceras() {
    // Definir las cabeceras
    val headers = listOf("Item Padre","Item", "Serie Inicio", "Serie Final", "Ean")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(6.dp))


    ) {
        headers.forEach { header ->
            Text(
                text = header,
                modifier = Modifier
                    .width(130.dp)
                    .padding(horizontal = 16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,  // Permite que el texto se divida en dos líneas
                color = Color(0xFF00909E),  // Color verde personalizado
                textAlign = TextAlign.Start  // Centra el texto

            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicComboBox(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) { // Asegura que haya espacio suficiente

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .padding(10.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = selectedItem,
                    onValueChange = {},
                    label = {
                        Text(
                            "SELECCIONE EQUIVALENCIA",
                            color = GreenMakita,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFFCCCCCC),
                        unfocusedIndicatorColor = Color(0xFFDDDDDD),
                        cursorColor = GreenMakita
                    ),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        color = Color.Red,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Menú desplegable
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth() // Asegura que ocupe todo el ancho
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                onItemSelected(item)
                                expanded = false
                            },
                            modifier = Modifier.fillMaxWidth() // Asegura que el item ocupe todo el ancho
                        )
                    }
                }
            }
        }


    }
}

fun guardarCodigoEnArchivo(context: Context, dataItem: CodigoData , selectedItem : String) {
    val linea = "${dataItem.item};${dataItem.serieInicio};${dataItem.serieHasta};${dataItem.letraFabrica};${dataItem.ean};$selectedItem\n"
    val fileName = "codigos.txt"

    try {
        val file = File(context.filesDir, fileName)
        val fos = FileOutputStream(file, true) // Modo 'true' para agregar contenido sin sobrescribir
        fos.write(linea.toByteArray())
        fos.close()
        Log.d("*MAKITA*", "Código guardado en archivo: $linea")
    } catch (e: Exception) {
        Log.e("*MAKITA*", "Error al guardar código en archivo", e)
    }
}






@Preview(showBackground = true)
@Composable
fun ProcesarSinCodigoScreenView() {
    KitControlScreen()
}
