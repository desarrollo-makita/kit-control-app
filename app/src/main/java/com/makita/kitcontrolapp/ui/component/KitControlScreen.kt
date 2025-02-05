package com.makita.kitcontrolapp.ui.component

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.RequiresPermission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Print
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.makita.kitcontrolapp.ui.theme.GreenMakita
import com.makita.ubiapp.EnvioCabeceraKitRequest
import com.makita.ubiapp.EnvioDatosRequest

import com.makita.ubiapp.KitItem
import com.makita.ubiapp.ResponseCabeceraKit
import com.makita.ubiapp.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID


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
    var item by rememberSaveable { mutableStateOf("") }
    var serieInicio by rememberSaveable { mutableStateOf("") }
    var serieHasta by rememberSaveable { mutableStateOf("") }
    var letraFabrica by rememberSaveable {  mutableStateOf("") }
    var ean by rememberSaveable {  mutableStateOf("") }

    var clearRequested by rememberSaveable { mutableStateOf(false) }
    var codigoInvalido by remember { mutableStateOf(false) }
    var showTable  by remember{ mutableStateOf(false) }
    var showCombo  by remember{ mutableStateOf(false) }
    var showButtonimprimir  by remember{ mutableStateOf(false) }

    val minCodigoLength = 55
    val listaCodigos = remember { mutableStateListOf<CodigoData>() }
    val listaKits = remember { mutableStateListOf<KitItem>() }
    val context = LocalContext.current
    var selectedKitData by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedItem by remember { mutableStateOf("") }
    var dataItem by remember {mutableStateOf<CodigoData?>(null)}
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    val scrollState = rememberScrollState()
    var isLoading by remember { mutableStateOf(false) } // Estado para el loading


    fun clearAll() {
        borrarArchivo(context)
        textoEscaneado = TextFieldValue("")
        clearRequested = false
        codigoInvalido = false
        showTable = false
        item = ""
        serieInicio = ""
        serieHasta = ""
        letraFabrica = ""
        ean = ""
        listaCodigos.clear()  // Limpiar la lista de códigos
        listaKits.clear()  // Limpiar la lista de kits
        showCombo = false
        selectedKitData = emptyList()
        selectedItem = ""
        dataItem = null
        showButtonimprimir= false

    }

    fun onKitSelected(response: List<KitItem>) {
        // Mapeamos la lista para obtener solo el campo 'item'
        val simplifiedResponse = response.map { it.item }
        Log.d("*MAKITA*", "simplifiedResponse: $simplifiedResponse")
        // Verificar si la lista está vacía o no
        if (simplifiedResponse.isNotEmpty()) {
            // Asignamos la lista de items
            selectedKitData = simplifiedResponse
            Log.d("*MAKITA*", "selectedKitData: $selectedKitData")
            showCombo = true  // Mostrar el Combo si hay elementos seleccionados
        } else {
            selectedKitData = emptyList()
            showCombo = false  // Ocultar el Combo si la lista está vacía
        }

        // Imprimimos solo los valores 'item' en el log
        Log.d("*MAKITA*", "Kit seleccionado ---> cargo la lista: ${simplifiedResponse.joinToString(", ")}")
    }

    // Esto solo se ejecutará cuando el texto cambie realmente
    LaunchedEffect(textoEscaneado.text) {
        if (textoEscaneado.text.isNotEmpty()) {
            codigoInvalido = textoEscaneado.text.length != minCodigoLength
            Log.d("*MAKITA*", "[KitControlScreen] Se ingresa texto: ${textoEscaneado.text}")
            if (codigoInvalido) {
                textoEscaneado = TextFieldValue("")
            }else{
                dataItem = parseCodigo(textoEscaneado.text)

                item =  dataItem!!.item
                serieInicio =  dataItem!!.serieInicio
                serieHasta =  dataItem!!.serieHasta
                letraFabrica =  dataItem!!.letraFabrica
                ean =  dataItem!!.ean

                listaCodigos.add(dataItem!!)

                showTable = true
                textoEscaneado = TextFieldValue("")

               // guardarCodigoEnArchivo(context, dataItem!!)

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

            ButtonBluetooth(
                context,
                onDeviceSelected = { device ->
                    selectedDevice = device // Actualiza el estado en el padre
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .verticalScroll(scrollState)
                    .background(Color.White, shape = RoundedCornerShape(30.dp)),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


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

                if (isLoading) {
                    LoadingIndicator()
                }
                if(showCombo){
                    ClassicComboBox(
                        items = selectedKitData,  // La lista de elementos
                        selectedItem = selectedItem,  // El ítem seleccionado
                        onItemSelected = { item ->
                            selectedItem = item
                            Log.d("*MAKITA*" , "selectedItem  $selectedItem")// Actualiza el ítem seleccionado
                            showButtonimprimir = true
                        }


                    )
                }

                if(showTable){
                    BorrarButton(onClear = { clearAll() })
                }

                if(showButtonimprimir){
                    ButtonImprimir(
                        context,
                        selectedDevice = selectedDevice,
                        listaCodigos,
                        selectedItem,
                        onClear = { clearAll() },
                        isLoading = isLoading,
                        setLoading = { isLoading = it } // Pasamos la función para cambiar el estado

                    )
                }
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
    var showDialog by remember { mutableStateOf(false) }

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
                        apiResponse = RetrofitClient.apiService.obtenerListaKit(item)
                        if (apiResponse.isEmpty()) {
                            showDialog = true // Mostrar el diálogo si no hay datos
                        } else {
                            onItemPadreSelected(apiResponse)
                        }
                    } catch (e: Exception) {
                        Log.e("*ERROR API*", "Error al obtener datos: ${e.message}")
                        showDialog = true // También mostrar el diálogo en caso de error
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Aviso") },
            text = { Text(text = "No se encontraron datos para el item proporcionado.") },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                onItemSelected(item)
                                expanded = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }


    }
}

fun guardarCodigoEnArchivo(context: Context, dataItem: CodigoData) {
    val linea = "${dataItem.item};${dataItem.serieInicio};${dataItem.serieHasta};${dataItem.letraFabrica};${dataItem.ean}\n"
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


fun borrarArchivo(context: Context) {
    val fileName = "codigos.txt"
    val file = File(context.filesDir, fileName)
    if (file.exists()) {
        val deleted = file.delete()
        if (deleted) {
            Log.d("*MAKITA*", "Archivo borrado exitosamente.")
        } else {
            Log.e("*MAKITA*", "No se pudo borrar el archivo.")
        }
    } else {
        Log.e("*MAKITA*", "El archivo no existe.")
    }
}


@Composable
fun BorrarButton(
    onClear: () -> Unit
) {
    Button(
        onClick = {  onClear()},
        colors = ButtonDefaults.buttonColors(containerColor = GreenMakita), // Color del botón GreenMakita
        modifier = Modifier
            .padding(16.dp)
            .height(50.dp)
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Clear text",
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        onClear()
                    },
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp)) // Espacio entre el texto y el ícono
            Text(
                text = "Borrar",
                color = Color.White, // Texto blanco
                style = MaterialTheme.typography.bodyLarge
            )


        }
    }
}

@Composable
fun ButtonBluetooth(
    context: Context,
    onDeviceSelected: (BluetoothDevice?) -> Unit) {

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val (printers, setPrinters) = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedPrinterName by remember { mutableStateOf("") }

    ExtendedFloatingActionButton(
        onClick = {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    (context as Activity),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    100
                )
            } else {
                showDialog = true
                startBluetoothDiscovery(context, bluetoothAdapter, setPrinters)
            }
        },
        containerColor = Color(0xFF00909E),
        contentColor = Color.White,
        icon = {
            Icon(
                Icons.Outlined.Bluetooth,
                contentDescription = "Conectarse a Bluetooth",
                modifier = Modifier.size(28.dp)
            )
        },
        text = {
            Text(
                text = "Conectarse",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier
            .height(50.dp)
            .width(200.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Diálogo de selección de dispositivos
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Seleccione una impresora") },
            text = {
                BluetoothDeviceList(
                    deviceList = printers,
                    onDeviceSelected = { device ->
                        onDeviceSelected(device)
                        selectedPrinterName = device.name ?: "Desconocida"
                        showDialog = false
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Mensaje de impresora seleccionada y botón de imprimir
    if (selectedPrinterName.isNotEmpty()) {

        Text(
            text = "IMPRESORA SELECCIONADA",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = GreenMakita // Usa el color personalizado
        )
        Text(
            text = selectedPrinterName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = GreenMakita // Usa el color personalizado
        )
    }
}

@Composable
fun BluetoothDeviceList(
    deviceList: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {

    val context = LocalContext.current
    // Verifica el permiso BLUETOOTH_CONNECT en dispositivos con Android 12 (API 31) o superior
    Log.d("ETIQUETADO-Z", "BluetoothDeviceList")

    val hasBluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

    } else {
        Log.d("ETIQUETADO-Z", "permisos OK")
        true // No se requiere permiso en versiones anteriores
    }


    // Solo muestra la lista si el permiso es otorgado o si el sistema no lo requiere
    if (hasBluetoothConnectPermission) {
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(deviceList) { device ->
                Text(
                    text = device.name ?: "Dispositivo desconocido",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceSelected(device) }
                        .padding(8.dp)
                )
            }
        }
    } else {
        // Mostrar mensaje o manejar el caso en el que no se tiene el permiso
        Log.d("ETIQUETADO-Z", " Permisos Denegados")
        Text("Permiso Bluetooth no otorgado. No se pueden mostrar los dispositivos.")
    }
}


fun startBluetoothDiscovery(
    context: Context,
    bluetoothAdapter: BluetoothAdapter?,
    setDevices: (List<BluetoothDevice>) -> Unit
): BroadcastReceiver? {
    if (bluetoothAdapter == null) {
        Log.e("BluetoothDiscovery", "El adaptador Bluetooth es nulo.")
        return null
    }

    // Lista para almacenar dispositivos encontrados
    val foundDevices = mutableListOf<BluetoothDevice>()

    // Receptor para dispositivos encontrados
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    // Manejo seguro del nombre del dispositivo
                    val deviceName: String = if (ActivityCompat.checkSelfPermission(
                            context!!,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        it.name ?: "" // Obtener el nombre si hay permiso
                    } else {
                        Log.w("BluetoothDiscovery", "Permiso BLUETOOTH_CONNECT no otorgado. Usando nombre vacío.")
                        "" // Nombre vacío si no hay permiso
                    }
                    Log.e("*MAKITA*", "isZebraPrinter. $deviceName")
                    // Filtrar impresoras Zebra
                    if (isZebraPrinter(deviceName) && !foundDevices.contains(it))
                    // if (  !foundDevices.contains(it))
                    {
                        Log.e("*MAKITA*", "isZebraPrinter. $deviceName")
                        foundDevices.add(it)
                        setDevices(foundDevices)
                    }
                }
            }
        }
    }

    // Registrar el receptor para detectar dispositivos Bluetooth
    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    context.registerReceiver(receiver, filter)

    // Manejo explícito de permisos
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        != PackageManager.PERMISSION_GRANTED
    ) {
        Log.e("BluetoothDiscovery", "Permisos insuficientes para escaneo Bluetooth.")
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
            1001
        )
        return null
    }

    try {
        if (!bluetoothAdapter.startDiscovery()) {
            Log.e("BluetoothDiscovery", "No se pudo iniciar el descubrimiento Bluetooth.")
            context.unregisterReceiver(receiver)
            return null
        }
    } catch (e: SecurityException) {
        Log.e("BluetoothDiscovery", "Error al iniciar el descubrimiento: ${e.message}")
        context.unregisterReceiver(receiver)
        return null
    }

    return receiver
}


private fun isZebraPrinter(deviceName: String): Boolean {
    return deviceName.contains("Zebra", ignoreCase = true) ||
            deviceName.startsWith("ZQ", ignoreCase = true)
}

@Composable
fun ButtonImprimir(
    context: Context,
    selectedDevice: BluetoothDevice?,
    listaCodigos: List<CodigoData> ,
    selectedItem : String,
    onClear: () -> Unit,
    isLoading: Boolean,
    setLoading: (Boolean) -> Unit

) {

    val datosKit = EnvioDatosRequest(
        selectedItem,
        listaCodigos
    )

    // Verifica el permiso BLUETOOTH_CONNECT antes de permitir la impresión
    val hasBluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // No es necesario solicitar permiso en versiones anteriores
    }

    // Mostrar un mensaje de error si no se tiene el permiso
    if (!hasBluetoothConnectPermission) {
        Log.d("ButtonImprimir", "Permiso Bluetooth no otorgado.")
        Text("Permiso Bluetooth no otorgado. No se puede imprimir.")
    }

    ExtendedFloatingActionButton(
        onClick = {
            Log.d("ButtonImprimir", "Botón Imprimir presionado.")
            setLoading(true)
            CoroutineScope(Dispatchers.IO).launch {
               val itemKitPdf  = insertarDatosKit(datosKit) // Llamada a la función suspendida

                if (itemKitPdf != null) {
                    if (itemKitPdf.status == "success") {
                        Log.d("*MAKITA001*", "Registro insertado correctamente: $itemKitPdf")
                        val armadoCodigoKitPdf417 = "${itemKitPdf.ItemKitID.padEnd(20)}${itemKitPdf.serieDesde}${itemKitPdf.serieHasta}${itemKitPdf.ean}"
                        val itemKit = itemKitPdf.ItemKitID
                        if (hasBluetoothConnectPermission) {
                            Log.d("ButtonImprimir", "Permiso Bluetooth otorgado.")

                            selectedDevice?.let { device ->
                                val printerLanguage = "ZPL" // Cambiar según el lenguaje soportado por la impresora
                                Log.d("ButtonImprimir", "Dispositivo seleccionado: ${device.name}, Dirección: ${device.address}")
                                val serieInicial =  itemKitPdf.serieDesde
                                printDataToBluetoothDevice(
                                    device,
                                    armadoCodigoKitPdf417,
                                    context,
                                    printerLanguage,
                                    onClear,
                                    setLoading,
                                    itemKit,
                                    serieInicial

                                )
                            }
                        } else {
                            Log.d("ButtonImprimir", "Permiso Bluetooth no otorgado al presionar el botón.")
                            Toast.makeText(context, "Permiso Bluetooth no otorgado. No se puede imprimir.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.d("*MAKITA001*", "Error al insertar registro: ${itemKitPdf.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "KIT ya fue ingresado ", Toast.LENGTH_LONG).show()
                            setLoading(false)
                        }
                        onClear()
                    }
                }
            }


        },
        containerColor = Color(0xFF00909E),
        contentColor = Color.White,
        icon = {
            Icon(
                Icons.Outlined.Print,
                contentDescription = "Imprimir",
                modifier = Modifier.size(28.dp)
            )
        },
        text = {
            Text(
                text = "Imprimir",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier
            .height(50.dp)
            .width(200.dp)
    )

    Spacer(modifier = Modifier.width(8.dp))
}


@RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
fun printDataToBluetoothDevice(
    device: BluetoothDevice,
    data: String,
    context: Context,
    printerLanguage: String,
    onClear: () -> Unit,
    setLoading: (Boolean) -> Unit,
    itemKit: String,
    serieInicial: String
    ) {
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    val data2 = itemKit
    val CodigoConcatenado2 = data
    val CodigocomercialNN2 = serieInicial

    CoroutineScope(Dispatchers.IO).launch {

        val bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
        bluetoothSocket.connect()

        try {
            // Conectar al dispositivo Bluetooth
           if (bluetoothSocket.isConnected) {
                Log.d("Bluetooth", "Conexión exitosa.")

                val outputStream = bluetoothSocket.outputStream

                // Enviar los datos de impresión
                Log.d("", "Enviando datos de impresión: $CodigoConcatenado2")

                if (printerLanguage == "ZPL") {
                    val linea2 = "^XA\n " +
                            "^PW354 \n" +   // Ancho de la etiqueta (3 cm = 354 dots)
                            "^LL354 \n" +
                            "^FO50,25\n " +
                            "^ADN,15,13\n " +
                            "^FD$data2^FS\n " +
                            "^FO50,70\n " +
                            "^ADN,15,12\n " +
                            //"^B7N,5,10,2,5,N\n " +
                            //"^B7N,1,30,2,30,N\n  " +
                            //"^B7N,2,10,2,30,NY\n  " +
                            //"^FD$comercial^FS\n " +
                            "^B7N,5,10,2,20,N" +
                            "^FD$CodigoConcatenado2^FS " +
                            "^FO50,190\n " +
                            "^ADN,15,13\n " +
                            "^FD$CodigocomercialNN2^FS\n " +
                            "^XZ\n"

                    outputStream.write(linea2.toByteArray(Charsets.US_ASCII))
                    outputStream.flush()
                    Log.d("Bluetooth", "Datos enviados correctamente.")


                }

                // Mensaje de éxito
                withContext(Dispatchers.Main) {
                    Log.d("ETIQUETADO-Z", "Impresión realizada con éxito.")

                    Toast.makeText(context, "Impresión Correcta", Toast.LENGTH_SHORT).show()
                    onClear()
                    setLoading(false)

                }

            } else {
                // Si no se pudo conectar
                withContext(Dispatchers.Main) {
                    Log.d("Bluetooth", "No se pudo conectar al dispositivo.")

                    Toast.makeText(context, "No se pudo conectar al dispositivo Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("Bluetooth", "Error al enviar datos: ${e.message}")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al enviar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                onClear()
                setLoading(false)
            }
        } finally {
            try {
                delay(500) // Espera antes de cerrar el socket
                bluetoothSocket?.close()
                Log.d("Bluetooth", "Socket cerrado.")

            } catch (e: IOException) {
                Log.e("Bluetooth", "Error al cerrar el socket: ${e.message}")

            }
        }
    }
}


fun prepararDatosImpresion(
    listaCodigos: List<CodigoData>,
    selectedItem: String): String{

    val itemNuevo = selectedItem.padEnd(20, '0')
    val dataPdf417 = "$itemNuevo+000000001+000000001+Y+0088381096959"
    // Creamos una variable StringBuilder para armar el texto final
    val textoFinal = StringBuilder()


    Log.d("ETIQUETADO-Z", "dataPdf417 remacterizado: $dataPdf417")

    // Agregamos las variables separadas por comas (formato CSV)
    //textoFinal.append("$textoAnterior,$serieDesde,$serieHasta,$letraFabrica,$ean,$itemEquivalente,$cargador,$bateria")
   // Log.d("ETIQUETADO-Z", "textoFinal : $textoFinal")


    // Retornamos el texto final como un String
    return (dataPdf417)
}


suspend fun insertarDatosKit(datosKit: EnvioDatosRequest): ResponseCabeceraKit? {
    try {
        val response = RetrofitClient.apiService.insertaDataDetalle(datosKit)
        Log.d("*MAKITA001*", "Datos insertados con éxito: $response")

        val datosKitCabecera = EnvioCabeceraKitRequest(
            ItemKitID =datosKit.selectedItem ,
            ean = response.itemEncontrado.ean
        )
        // Ahora llamamos a otro endpoint (segundo)
        val secondResponse = RetrofitClient.apiService.insertaDataCabecera(datosKitCabecera)
        Log.d("*MAKITA001*", "Respuesta del segundo endpoint: $secondResponse")

        return secondResponse
    } catch (e: Exception) {
        Log.e("*MAKITA001*", "Error al insertar datos: ${e.message}")
        return null
    }
}


@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()

            .wrapContentSize(Alignment.Center)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp), // Tamaño del indicador
                color = Color(0xFF00909E), // Color personalizado
                strokeWidth = 6.dp // Grosor del círculo
            )
            Spacer(modifier = Modifier.height(16.dp)) // Espaciado
            Text(
                text = "Cargando...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun ProcesarSinCodigoScreenView() {
    KitControlScreen()
}
