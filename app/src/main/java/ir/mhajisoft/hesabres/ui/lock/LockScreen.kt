package ir.mhajisoft.hesabres.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.unlock_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.unlock_subtitle), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        Button(onClick = onUnlock) { Text(stringResource(R.string.unlock)) }
    }
}
