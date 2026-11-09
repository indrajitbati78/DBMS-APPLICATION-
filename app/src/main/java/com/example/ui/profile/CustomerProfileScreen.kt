package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Customer
import com.example.data.model.User
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    viewModel: CustomerProfileViewModel,
    currentUser: User?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddPurchaseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Customer Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Edit button available to both Staff and Admin
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("profile_edit_customer_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer")
                    }

                    // Delete button visible ONLY to Admin
                    if (currentUser?.isAdmin == true) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("profile_delete_customer_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingState(message = "Loading customer profile...", modifier = Modifier.padding(paddingValues))
        } else if (uiState.customer == null) {
            EmptyState(
                title = "Customer Not Found",
                message = "This record may have been deleted.",
                icon = Icons.Default.PersonOff,
                actionText = "Go Back",
                onAction = onNavigateBack,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            val customer = uiState.customer!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card: Photo/Avatar, Name, Quick Contact Buttons
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar or Photo
                            if (customer.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = customer.photoUrl,
                                    contentDescription = customer.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = customer.name.split(" ")
                                        .mapNotNull { it.firstOrNull()?.toString() }
                                        .take(2)
                                        .joinToString("")
                                        .uppercase()
                                    Text(
                                        text = initials.ifBlank { "C" },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Registered by: ${customer.createdBy.ifBlank { "Shop Staff" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action Buttons: Call & WhatsApp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.dialCustomer(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("action_call_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call Customer", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.openWhatsApp(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("action_whatsapp_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF25D366) // WhatsApp signature green
                                    )
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Customer Details Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Contact & Location",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            DetailRow(icon = Icons.Default.Phone, label = "Mobile", value = "+91 ${customer.mobile}")
                            if (customer.email.isNotBlank()) {
                                DetailRow(icon = Icons.Default.Email, label = "Email", value = customer.email)
                            }
                            if (customer.address.isNotBlank()) {
                                DetailRow(icon = Icons.Default.Home, label = "Address", value = customer.address)
                            }
                            val loc = listOf(customer.city, customer.state, customer.pincode).filter { it.isNotBlank() }.joinToString(", ")
                            if (loc.isNotBlank()) {
                                DetailRow(icon = Icons.Default.LocationOn, label = "City & PIN", value = loc)
                            }
                            if (customer.notes.isNotBlank()) {
                                DetailRow(icon = Icons.Default.Notes, label = "Notes", value = customer.notes)
                            }
                        }
                    }
                }

                // Purchase History Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Purchase History (${uiState.purchases.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            val totalSpent = uiState.purchases.sumOf { it.price }
                            Text(
                                text = "Total Business: ${formatInr(totalSpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalButton(
                            onClick = { showAddPurchaseDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_purchase_button")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Purchase")
                        }
                    }
                }

                // Purchases List
                if (uiState.purchases.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No purchase records yet", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap 'Add Purchase' to log a smartphone sale for this customer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    items(uiState.purchases, key = { it.purchaseId }) { purchase ->
                        PurchaseCard(
                            purchase = purchase,
                            onDelete = if (currentUser?.isAdmin == true) {
                                { currentUser?.let { viewModel.deletePurchase(purchase.purchaseId, it) } }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && uiState.customer != null) {
        EditCustomerDialog(
            customer = uiState.customer!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { updated ->
                currentUser?.let { viewModel.updateCustomerDetails(updated, it) }
                showEditDialog = false
            }
        )
    }

    // Add Purchase Dialog
    if (showAddPurchaseDialog) {
        AddPurchaseDialog(
            onDismiss = { showAddPurchaseDialog = false },
            onConfirm = { brand, model, imei, price, warrantyMonths, billUri ->
                currentUser?.let {
                    viewModel.addPurchase(brand, model, imei, price, warrantyMonths, billUri, it)
                }
                showAddPurchaseDialog = false
            }
        )
    }

    // Delete Confirmation Dialog (Admin only)
    if (showDeleteConfirmDialog && uiState.customer != null) {
        ConfirmDialog(
            title = "Delete Customer Record?",
            message = "Are you sure you want to permanently delete ${uiState.customer?.name} and all associated purchase records from Firebase? This action cannot be undone.",
            confirmText = "Delete Record",
            onConfirm = {
                currentUser?.let { viewModel.deleteCustomer(it) }
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
