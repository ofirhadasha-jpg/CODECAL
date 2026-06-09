package com.vmm.manager.ui.transactions

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vmm.manager.R
import com.vmm.manager.data.db.entities.Transaction
import com.vmm.manager.data.db.entities.TransactionStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(viewModel: TransactionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transactions_title)) },
                actions = {
                    Text(
                        "₪${"%.0f".format(state.periodRevenue)}",
                        modifier   = Modifier.padding(end = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SummaryItem(
                            stringResource(R.string.today_revenue),
                            "₪${"%.0f".format(state.todayRevenue)}"
                        )
                        SummaryItem(
                            stringResource(R.string.nav_transactions),
                            "${state.todayCount}"
                        )
                        SummaryItem(
                            stringResource(R.string.avg_label),
                            "₪${"%.0f".format(
                                if (state.todayCount > 0) state.todayRevenue / state.todayCount else 0.0
                            )}"
                        )
                    }
                }
            }

            items(state.transactions, key = { it.id }) { tx ->
                TransactionItem(tx, dateFormatter)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TransactionItem(tx: Transaction, fmt: SimpleDateFormat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (tx.status) {
                    TransactionStatus.COMPLETED -> Icons.Default.CheckCircle
                    TransactionStatus.FAILED    -> Icons.Default.Cancel
                    TransactionStatus.PENDING   -> Icons.Default.HourglassEmpty
                    else                        -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when (tx.status) {
                    TransactionStatus.COMPLETED -> Color(0xFF4CAF50)
                    TransactionStatus.FAILED    -> MaterialTheme.colorScheme.error
                    else                        -> MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(tx.productName, fontWeight = FontWeight.Medium)
                Text(fmt.format(Date(tx.createdAt)), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("₪${"%.2f".format(tx.totalAmount)}", fontWeight = FontWeight.Bold)
    }
    HorizontalDivider()
}
