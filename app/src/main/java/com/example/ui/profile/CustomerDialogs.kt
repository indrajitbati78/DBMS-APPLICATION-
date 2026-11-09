package com.example.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (brand: String, model: String, imei: String, price: Double, warrantyMonths: Int, billUri: Uri?) -> Unit
) {
    val popularBrands = listOf("Samsung", "Apple", "OnePlus", "Xiaomi", "Vivo", "Realme", "Motorola", "Oppo")
    var selectedBrand by remember { mutableStateOf("Samsung") }
    var model by remember { mutableStateOf("") }
    var imei by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var warrantyMonths by remember { mutableStateOf(12) }
    var billUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val billPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        billUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Device Purchase", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Text("Brand:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    popularBrands.forEach { b ->
                        FilterChip(
                            selected = selectedBrand == b,
                            onClick = { selectedBrand = b },
                            label = { Text(b) }
                        )
                    }
                }

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; error = null },
                    label = { Text("Model Name *") },
                    placeholder = { Text("e.g. Galaxy S24 Ultra") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_model_input")
                )

                OutlinedTextField(
                    value = imei,
                    onValueChange = { imei = it.filter { c -> c.isDigit() }.take(15); error = null },
                    label = { Text("IMEI (15 digits) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_imei_input"),
                    supportingText = { Text("${imei.length}/15 digits") }
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                    label = { Text("Price (₹) *") },
                    leadingIcon = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_price_input")
                )

                Text("Warranty Period:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(6, 12, 24).forEach { m ->
                        FilterChip(
                            selected = warrantyMonths == m,
                            onClick = { warrantyMonths = m },
                            label = { Text("$m Mo") }
                        )
                    }
                }

                // Bill photo attachment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            billPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (billUri != null) "Invoice Attached ✓" else "Attach Bill Photo (Optional)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        imageVector = if (billUri != null) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (model.isBlank()) {
                        error = "Enter model name"
                        return@Button
                    }
                    if (imei.length != 15) {
                        error = "IMEI must be 15 digits"
                        return@Button
                    }
                    val price = priceStr.toDoubleOrNull()
                    if (price == null || price <= 0.0) {
                        error = "Enter valid price"
                        return@Button
                    }
                    onConfirm(selectedBrand, model.trim(), imei.trim(), price, warrantyMonths, billUri)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_save_purchase_button")
            ) {
                Text("Add Purchase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (updated: Customer) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var mobile by remember { mutableStateOf(customer.mobile) }
    var email by remember { mutableStateOf(customer.email) }
    var address by remember { mutableStateOf(customer.address) }
    var city by remember { mutableStateOf(customer.city) }
    var state by remember { mutableStateOf(customer.state) }
    var pincode by remember { mutableStateOf(customer.pincode) }
    var notes by remember { mutableStateOf(customer.notes) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Customer Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter { c -> c.isDigit() }.take(10); error = null },
                    label = { Text("Mobile Number (10 digits) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { pincode = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("PIN") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Name cannot be empty"
                        return@Button
                    }
                    if (mobile.length != 10) {
                        error = "Mobile must be 10 digits"
                        return@Button
                    }
                    onConfirm(
                        customer.copy(
                            name = name.trim(),
                            mobile = mobile.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            state = state.trim(),
                            pincode = pincode.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
