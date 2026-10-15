package ir.mhajisoft.hesabres.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.domain.bank.BankLogoResources

/**
 * Resolves a bank mark through [R.drawable] instead of [android.content.res.Resources.getIdentifier].
 * The identifier lookup used `context.packageName`, which on debug builds is
 * `ir.mhajisoft.hesabres.debug` and does not contain the drawables. The name still
 * rendered from the BIN table, so a finished card number showed a bank with no logo.
 */
object BankLogos {
    fun resId(drawableName: String?): Int = when (BankLogoResources.canonical(drawableName)) {
        "bank_melli" -> R.drawable.bank_melli
        "bank_mellat" -> R.drawable.bank_mellat
        "bank_saderat" -> R.drawable.bank_saderat
        "bank_tejarat" -> R.drawable.bank_tejarat
        "bank_pasargad" -> R.drawable.bank_pasargad
        "bank_saman" -> R.drawable.bank_saman
        "bank_parsian" -> R.drawable.bank_parsian
        "bank_keshavarzi" -> R.drawable.bank_keshavarzi
        "bank_refah" -> R.drawable.bank_refah
        "bank_maskan" -> R.drawable.bank_maskan
        "bank_shahr" -> R.drawable.bank_shahr
        "bank_sina" -> R.drawable.bank_sina
        "bank_en" -> R.drawable.bank_en
        "bank_ayandeh" -> R.drawable.bank_ayandeh
        "bank_iranzamin" -> R.drawable.bank_iranzamin
        "bank_middleeast" -> R.drawable.bank_middleeast
        "bank_gardeshgari" -> R.drawable.bank_gardeshgari
        "bank_sepah" -> R.drawable.bank_sepah
        "bank_cbi" -> R.drawable.bank_cbi
        "bank_post" -> R.drawable.bank_post
        "bank_toseetaavon" -> R.drawable.bank_toseetaavon
        "bank_toseesaderat" -> R.drawable.bank_toseesaderat
        "bank_sanat" -> R.drawable.bank_sanat
        "bank_mehriran" -> R.drawable.bank_mehriran
        "bank_resalat" -> R.drawable.bank_resalat
        "bank_karafarin" -> R.drawable.bank_karafarin
        "bank_sarmayeh" -> R.drawable.bank_sarmayeh
        "bank_dey" -> R.drawable.bank_dey
        "bank_melal" -> R.drawable.bank_melal
        "bank_iranvenezuela" -> R.drawable.bank_iranvenezuela
        else -> R.drawable.bank_unknown
    }
}

@Composable
fun BankLogo(drawableName: String) {
    val res = BankLogos.resId(drawableName)
    key(drawableName, res) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
    }
}
