package com.vmm.manager.ui.machines

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
import com.vmm.manager.data.db.entities.Machine
import com.vmm.manager.data.db.entities.MachineStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineListScreen(viewModel: MachineViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.machines_title)) },
                actions = {
                    IconButton(onClick = { viewModel.syncAll() }) {
                        Icon(Icons.Default.Sync, stringResource(R.string.sync_all))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.machines, key = { it.id }) { machine ->
                MachineCard(
                    machine     = machine,
                    onSync      = { viewModel.syncMachine(machine) },
                    onViewSlots = { viewModel.selectMachine(machine) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MachineCard(machine: Machine, onSync: () -> Unit, onViewSlots: () -> Unit) {
    val syncLabel = stringResource(R.string.sync_label)
    Card(modifier = Modifier.fillMaxWidth(), onClick = onViewSlots) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DeviceHub,
                contentDescription = null,
                tint = when (machine.status) {
                    MachineStatus.ONLINE -> Color(0xFF4CAF50)
                    MachineStatus.ERROR  -> MaterialTheme.colorScheme.error
                    else                 -> MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(machine.name, fontWeight = FontWeight.Bold)
                Text(machine.location, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${machine.temperature.toInt()}°C  |  ${machine.machineType.name}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(machine.status)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onSync) { Text(syncLabel) }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: MachineStatus) {
    val (labelRes, color) = when (status) {
        MachineStatus.ONLINE      -> R.string.status_online       to Color(0xFF4CAF50)
        MachineStatus.OFFLINE     -> R.string.status_offline      to Color(0xFF9E9E9E)
        MachineStatus.ERROR       -> R.string.status_error        to Color(0xFFF44336)
        MachineStatus.MAINTENANCE -> R.string.status_maintenance  to Color(0xFFFF9800)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            stringResource(labelRes),
            modifier     = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color        = color,
            style        = MaterialTheme.typography.labelSmall,
            fontWeight   = FontWeight.Bold
        )
    }
}
