package com.mcc.construction.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.ui.common.appContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where the splash screen hands over to. */
sealed interface SplashDestination {
    data object SignIn : SplashDestination
    data class Home(val role: UserRole) : SplashDestination
}

/**
 * Holds the splash for long enough to register the brand, then decides where to
 * go: straight to the role's home if a session is already active, otherwise to
 * sign-in.
 */
class SplashViewModel(
    private val authRepository: AuthRepository,
    private val displayMillis: Long = DISPLAY_MILLIS
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)

    /** Null while the splash is showing; set once it is time to move on. */
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            delay(displayMillis)
            val session = authRepository.session.value
            _destination.value = if (session != null) {
                SplashDestination.Home(session.role)
            } else {
                SplashDestination.SignIn
            }
        }
    }

    companion object {
        /** Long enough for the progress bar to fill, short enough not to annoy. */
        const val DISPLAY_MILLIS = 1_400L

        val Factory = viewModelFactory {
            initializer { SplashViewModel(appContainer().authRepository) }
        }
    }
}
