package com.ustad.personalassistant.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.ustad.personalassistant.data.AppStateRepository
import com.ustad.personalassistant.domain.AppState
import com.ustad.personalassistant.permissions.Capability
import com.ustad.personalassistant.permissions.PermissionManager
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    private val appStateRepository: AppStateRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {
    val state: StateFlow<AppState> = appStateRepository.state

    fun refresh() = appStateRepository.refresh()

    fun request(activity: Activity, capability: Capability) {
        permissionManager.requestPermission(activity, capability)
    }

    fun explain(capability: Capability): String = permissionManager.explainPermission(capability)
}
