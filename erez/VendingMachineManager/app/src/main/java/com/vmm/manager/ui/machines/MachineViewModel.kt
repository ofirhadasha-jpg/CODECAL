package com.vmm.manager.ui.machines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmm.manager.data.db.dao.MachineDao
import com.vmm.manager.data.db.entities.Machine
import com.vmm.manager.data.db.entities.MachineStatus
import com.vmm.manager.vmc.VMCManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MachineListState(
    val machines: List<Machine> = emptyList(),
    val selectedMachine: Machine? = null,
    val showAddDialog: Boolean = false
)

@HiltViewModel
class MachineViewModel @Inject constructor(
    private val dao: MachineDao,
    private val vmcManager: VMCManager
) : ViewModel() {

    private val _state = MutableStateFlow(MachineListState())
    val state: StateFlow<MachineListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllMachines().collect { machines ->
                _state.update { it.copy(machines = machines) }
            }
        }
        observeVMCEvents()
    }

    private fun observeVMCEvents() {
        viewModelScope.launch {
            vmcManager.machineStatus.collect { status ->
                // Update first connected machine status
                _state.value.machines.firstOrNull()?.let { machine ->
                    dao.updateTemperature(machine.id, status.temperature)
                    if (status.doorOpen != machine.doorOpen) {
                        dao.updateMachine(machine.copy(doorOpen = status.doorOpen))
                    }
                }
            }
        }
    }

    fun syncMachine(machine: Machine) {
        viewModelScope.launch {
            vmcManager.syncInventory(machine.vmcAddress)
            dao.updateStatus(machine.id, MachineStatus.ONLINE)
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _state.value.machines.forEach { syncMachine(it) }
        }
    }

    fun selectMachine(machine: Machine) {
        _state.update { it.copy(selectedMachine = machine) }
    }

    fun showAddDialog() {
        _state.update { it.copy(showAddDialog = true) }
    }

    fun addMachine(name: String, location: String, serialNumber: String) {
        viewModelScope.launch {
            dao.insertMachine(Machine(name = name, location = location, serialNumber = serialNumber))
            _state.update { it.copy(showAddDialog = false) }
        }
    }
}
