package com.example.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.ui.components.CustomerCard
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    currentUser: User?,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Customer Directory", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.filteredCustomers.size} of ${uiState.allCustomers.size} customers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Export to CSV button
                    FilledTonalButton(
                        onClick = { viewModel.exportAndShareCsv(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search by name, mobile, or city...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("customer_search_input")
            )

            // Date Filter Chips & Sort Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter chips horizontally scrollable
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateFilter.values().forEach { filter ->
                        FilterChip(
                            selected = uiState.activeFilter == filter,
                            onClick = { viewModel.onFilterChanged(filter) },
                            label = { Text(filter.label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown Box
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .testTag("sort_filter_button")
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort records", tint = MaterialTheme.colorScheme.primary)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.onSortChanged(option)
                                    showSortMenu = false
                                },
                                leadingIcon = if (uiState.activeSort == option) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Customer List or Empty / Loading State
            if (uiState.isLoading) {
                LoadingState(message = "Loading customer database...")
            } else if (uiState.filteredCustomers.isEmpty()) {
                if (uiState.searchQuery.isNotBlank() || uiState.activeFilter != DateFilter.ALL) {
                    EmptyState(
                        title = "No Customers Found",
                        message = "No records match '${uiState.searchQuery}' under '${uiState.activeFilter.label}'. Try resetting search or filter.",
                        icon = Icons.Default.SearchOff,
                        actionText = "Reset Filters",
                        onAction = {
                            viewModel.onSearchQueryChanged("")
                            viewModel.onFilterChanged(DateFilter.ALL)
                        }
                    )
                } else {
                    EmptyState(
                        title = "No Customers Registered",
                        message = "Your shop customer register is currently empty. Add your first customer!",
                        icon = Icons.Default.PersonOff,
                        actionText = "Register Customer",
                        onAction = onNavigateToRegister
                    )
                }
            } else {
                val purchasesByCustomer = uiState.purchases.groupBy { it.customerId }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredCustomers, key = { it.customerId }) { customer ->
                        val count = purchasesByCustomer[customer.customerId]?.size ?: 0
                        CustomerCard(
                            customer = customer,
                            purchaseCount = count,
                            onClick = { onNavigateToProfile(customer.customerId) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
