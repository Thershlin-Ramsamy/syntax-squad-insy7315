package com.mcc.construction.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcc.construction.R
import kotlinx.coroutines.flow.Flow

@Composable
fun BackendReadStatus(
    failed: Flow<Boolean>
) {
    val unavailable by failed.collectAsStateWithLifecycle(
        initialValue = false
    )

    if (unavailable) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                text = stringResource(
                    R.string.backend_read_unavailable
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}