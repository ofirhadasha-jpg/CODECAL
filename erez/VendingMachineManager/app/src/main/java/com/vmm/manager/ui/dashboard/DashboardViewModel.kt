package com.vmm.manager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmm.manager.data.db.entities.Machine
import com.vmm.manager.data.db.entities.Product
import com.vmm.manager.data.db.entities.Transaction
import com.vmm.manager.data.repository.ProductRepository
import com.vmm.manager.data.repository.TransactionRepository
import com.vmm.manager.vmc.MachineStatus
import com.vmm.manager.vmc.VMCManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardState(
    val todayRevenue: Double = 0.0,
    val todayTransactions: Int = 0,
    val lowStockProducts: List<Product> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val machineStatus: MachineStatus = MachineStatus(),
    val isVmcConnected: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val productRepo: ProductRepository,
    private val transactionRepo: TransactionRepository,
    private val vmcManager: VMCManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
        observeVMC()
    }

    private fun loadDashboard() {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay   = startOfDay + 86_400_000L

        viewModelScope.launch {
            combine(
                productRepo.getLowStockProducts(),
                transactionRepo.getRecentTransactions(10)
            ) { lowStock, recent -> Pair(lowStock, recent) }
                .collect { (lowStock, recent) ->
                    val revenue = transactionRepo.getTotalRevenue(startOfDay, endOfDay)
                    val count   = transactionRepo.getTransactionCount(startOfDay, endOfDay)
                    _state.update {
                        it.copy(
                            todayRevenue       = revenue,
                            todayTransactions  = count,
                            lowStockProducts   = lowStock,
                            recentTransactions = recent
                        )
                    }
                }
        }
    }

    private fun observeVMC() {
        viewModelScope.launch {
            vmcManager.machineStatus.collect { status ->
                _state.update { it.copy(machineStatus = status, isVmcConnected = true) }
            }
        }
    }

    fun connectVMC() {
        viewModelScope.launch {
            val connected = vmcManager.connect()
            _state.update { it.copy(isVmcConnected = connected) }
        }
    }

    fun syncAllMachines() {
        viewModelScope.launch {
            vmcManager.syncInventory()
        }
    }
}
