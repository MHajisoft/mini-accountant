package ir.mhajisoft.hesabres.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.ui.BrandLockup
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton

@Composable
fun LockScreen(onUnlock: () -> Unit, message: String? = null) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandLockup(showTagline = true, modifier = Modifier.padding(bottom = 20.dp))
        Text(
            stringResource(R.string.unlock_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.unlock_subtitle),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!message.isNullOrBlank()) {
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
        Column(Modifier.fillMaxWidth()) {
            PrimaryWideButton(stringResource(R.string.unlock), onClick = onUnlock)
        }
    }
}
