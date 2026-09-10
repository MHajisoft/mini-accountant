package ir.mhajisoft.hesabres.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.ui.theme.BrandBlue

@Composable
fun BrandSplash(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(BrandBlue),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.splash_brand),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

/** Full lockup (mark + حسابرس wordmark). The PNG is already RTL Persian. */
@Composable
fun BrandLogoFull(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Image(
        painter = painterResource(R.drawable.hesabres_logo_full),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 64.dp else 88.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun BrandTopBarLogo(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        BrandLogoFull(compact = true)
    }
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Image(
        painter = painterResource(R.drawable.ic_splash_logo),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun BrandOnboardingHeader() {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BrandLogoFull(compact = false)
    }
}
