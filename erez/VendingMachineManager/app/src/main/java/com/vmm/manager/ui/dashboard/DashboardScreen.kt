package com.vmm.manager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vmm.manager.R
import com.vmm.manager.data.db.entities.Product
import com.vmm.manager.data.db.entities.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToProducts: () -> Unit,
    onNavigateToMachines: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VMM Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    VMCStatusChip(connected = state.isVmcConnected) { viewModel.connectVMC() }
                    IconButton(onClick = { viewModel.syncAllMachines() }) {
                        Icon(Icons.Default.Sync, stringResource(R.string.sync_label))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label    = stringResource(R.string.today_revenue),
                        value    = "₪${"%.0f".format(state.todayRevenue)}",
                        icon     = Icons.Default.AttachMoney,
                        color    = MaterialTheme.colorScheme.primaryContainer
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label    = stringResource(R.string.today_transactions),
                        value    = "${state.todayTransactions}",
                        icon     = Icons.Default.ShoppingCart,
                        color    = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }

            item {
                MachineStatusCard(
                    temperature    = state.machineStatus.temperature,
                    doorOpen       = state.machineStatus.doorOpen,
                    microwaveActive = state.machineStatus.microwaveActive
                )
            }

            if (state.lowStockProducts.isNotEmpty()) {
                item {
                    SectionHeader(
                        title    = "${stringResource(R.string.low_stock)} (${state.lowStockProducts.size})",
                        actionLabel = stringResource(R.string.all_products),
                        onAction = onNavigateToProducts
                    )
                }
                items(state.lowStockProducts.take(5)) { product ->
                    LowStockItem(product)
                }
            }

            item {
                SectionHeader(
                    title    = stringResource(R.string.recent_transactions),
                    actionLabel = stringResource(R.string.see_all),
                    onAction = onNavigateToTransactions
                )
            }
            items(state.recentTransactions) { tx ->
                TransactionRow(tx)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun VMCStatusChip(connected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = connected,
        onClick  = onClick,
        label    = {
            Text(
                if (connected) stringResource(R.string.vmc_connected)
                else stringResource(R.string.vmc_disconnected)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (connected) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (connected) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    )
}

@Composable
private fun KpiCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MachineStatusCard(temperature: Float, doorOpen: Boolean, microwaveActive: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.machine_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusIndicator(
                    label = stringResource(R.string.lbl_temperature),
                    value = "${temperature.toInt()}°C",
                    icon  = Icons.Default.Thermostat,
                    alert = temperature > 35f
                )
                StatusIndicator(
                    label = stringResource(R.string.lbl_door),
                    value = if (doorOpen) stringResource(R.string.door_open)
                            else stringResource(R.string.door_closed),
                    icon  = Icons.Default.DoorFront,
                    alert = doorOpen
                )
                StatusIndicator(
                    label = stringResource(R.string.lbl_microwave),
                    value = if (microwaveActive) stringResource(R.string.microwave_on)
                            else stringResource(R.string.microwave_off),
                    icon  = Icons.Default.Microwave,
                    alert = false
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(label: String, value: String, icon: ImageVector, alert: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun LowStockItem(product: Product) {
    val remaining = stringResource(R.string.remaining)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(product.name, fontWeight = FontWeight.Medium)
            }
            Text(
                "$remaining ${product.stockCount}",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(tx.productName, fontWeight = FontWeight.Medium)
            Text(
                java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(tx.createdAt)),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("₪${"%.2f".format(tx.totalAmount)}", fontWeight = FontWeight.Bold)
            Text(
                tx.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = when (tx.status.name) {
                    "COMPLETED" -> Color(0xFF4CAF50)
                    "FAILED"    -> MaterialTheme.colorScheme.error
                    else        -> MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
    HorizontalDivider()
}
