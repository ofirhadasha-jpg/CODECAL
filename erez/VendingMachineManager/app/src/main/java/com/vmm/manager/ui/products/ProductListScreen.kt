package com.vmm.manager.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vmm.manager.R
import com.vmm.manager.data.db.entities.Product
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.products_title)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon    = { Icon(Icons.Default.Add, null) },
                text    = { Text(stringResource(R.string.add_product)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value         = state.searchQuery,
                onValueChange = viewModel::setSearch,
                modifier      = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder   = { Text(stringResource(R.string.search_product)) },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                singleLine    = true
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.products, key = { it.id }) { product ->
                    ProductCard(
                        product   = product,
                        onEdit    = { onEditProduct(product.id) },
                        onDelete  = { viewModel.deleteProduct(product.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(product: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val costPrefix  = stringResource(R.string.cost_prefix)
    val salePrefix  = stringResource(R.string.sale_prefix)
    val stockPrefix = stringResource(R.string.stock_prefix)

    Card(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (product.imageUri.isNotBlank()) {
                AsyncImage(
                    model              = File(product.imageUri),
                    contentDescription = stringResource(R.string.product_image),
                    modifier           = Modifier.size(64.dp),
                    contentScale       = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text(
                    "$costPrefix₪${"%.2f".format(product.costPrice)}  " +
                    "$salePrefix₪${"%.2f".format(product.salePrice)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = if (product.stockCount <= product.minStock)
                                   MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$stockPrefix${product.stockCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.stockCount <= product.minStock)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text(stringResource(R.string.delete_product_title)) },
            text    = { Text("${stringResource(R.string.delete_product_confirm)} ${product.name}?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
