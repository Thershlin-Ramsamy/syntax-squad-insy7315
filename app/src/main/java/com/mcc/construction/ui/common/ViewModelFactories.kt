package com.mcc.construction.ui.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.mcc.construction.MccApplication
import com.mcc.construction.di.AppContainer

/**
 * The app's [AppContainer], for use inside a `viewModelFactory { initializer { } }`
 * block. Every ViewModel gets its repositories this way:
 *
 * ```
 * companion object {
 *     val Factory = viewModelFactory {
 *         initializer { TaskListViewModel(appContainer().taskRepository) }
 *     }
 * }
 * ```
 */
fun CreationExtras.appContainer(): AppContainer =
    (checkNotNull(this[APPLICATION_KEY]) as MccApplication).container
