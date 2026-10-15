package ir.mhajisoft.hesabres.domain.bank

/**
 * Drawable lookup must use the Gradle resource namespace, not [android.content.Context.getPackageName].
 *
 * Debug builds set `applicationIdSuffix = ".debug"`, so `context.packageName` is
 * `ir.mhajisoft.hesabres.debug` while every `res/drawable` entry is compiled into the
 * namespace package `ir.mhajisoft.hesabres`. `Resources.getIdentifier(name, "drawable", packageName)`
 * then returns 0. The bank *name* still appears because it comes from the BIN table, and the
 * logo row is the first thing shown once six digits identify the card — so the mark looks
 * missing exactly when the number is recognized.
 */
object BankLogoResources {
    const val NAMESPACE = "ir.mhajisoft.hesabres"
    const val UNKNOWN = "bank_unknown"

    val known = listOf(
        "bank_melli",
        "bank_mellat",
        "bank_saderat",
        "bank_tejarat",
        "bank_pasargad",
        "bank_saman",
        "bank_parsian",
        "bank_keshavarzi",
        "bank_refah",
        "bank_maskan",
        "bank_shahr",
        "bank_sina",
        "bank_en",
        "bank_ayandeh",
        "bank_iranzamin",
        "bank_middleeast",
        "bank_gardeshgari",
        "bank_sepah",
        "bank_cbi",
        "bank_post",
        "bank_toseetaavon",
        "bank_toseesaderat",
        "bank_sanat",
        "bank_mehriran",
        "bank_resalat",
        "bank_karafarin",
        "bank_sarmayeh",
        "bank_dey",
        "bank_melal",
        "bank_iranvenezuela",
        UNKNOWN,
    )

    fun lookupPackage(applicationId: String, namespace: String = NAMESPACE): String = namespace

    fun canonical(drawableName: String?): String =
        if (drawableName != null && drawableName in known) drawableName else UNKNOWN
}
