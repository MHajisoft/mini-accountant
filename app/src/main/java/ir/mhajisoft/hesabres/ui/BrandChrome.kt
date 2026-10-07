package ir.mhajisoft.hesabres.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.ui.theme.BrandBlue
import ir.mhajisoft.hesabres.ui.theme.BrandDeepBlue

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

/**
 * Compact mark + wordmark on a white plate so the asset backgrounds
 * stay clean in both light and dark themes.
 */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    showTagline: Boolean = false,
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color.White,
            border = BorderStroke(1.dp, BrandDeepBlue.copy(alpha = 0.18f)),
            shadowElevation = 1.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandMark(size = 36.dp)
                Image(
                    painter = painterResource(R.drawable.hesabres_wordmark),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.height(26.dp).widthIn(max = 148.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                )
            }
        }
        if (showTagline) {
            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BrandTopBarLogo(modifier: Modifier = Modifier) {
    BrandLockup(modifier, showTagline = true)
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
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color.White,
            border = BorderStroke(1.dp, BrandDeepBlue.copy(alpha = 0.18f)),
            shadowElevation = 2.dp,
        ) {
            BrandLogoFull(
                compact = true,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}
