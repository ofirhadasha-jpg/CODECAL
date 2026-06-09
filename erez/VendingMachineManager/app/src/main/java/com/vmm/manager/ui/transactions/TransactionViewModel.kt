package com.vmm.manager.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmm.manager.data.db.entities.Transaction
import com.vmm.manager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val todayRevenue: Double = 0.0,
    val periodRevenue: Double = 0.0,
    val todayCount: Int = 0
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    init {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val endOfDay = startOfDay + 86_400_000L

        viewModelScope.launch {
            repository.getRecentTransactions(100).collect { txList ->
                val todayRevenue = repository.getTotalRevenue(startOfDay, endOfDay)
                val todayCount   = repository.getTransactionCount(startOfDay, endOfDay)
                _state.update {
                    it.copy(
                        transactions  = txList,
                        todayRevenue  = todayRevenue,
                        periodRevenue = todayRevenue,
                        todayCount    = todayCount
                    )
                }
            }
        }
    }
}
