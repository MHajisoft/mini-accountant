package ir.mhajisoft.hesabres.data.bank

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.mhajisoft.hesabres.domain.bank.BankDirectory
import ir.mhajisoft.hesabres.domain.bank.BankInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankDirectoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val directory: BankDirectory by lazy { load() }

    private fun load(): BankDirectory {
        val text = context.assets.open("banks.json").bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString(BankDirectoryDto.serializer(), text)
        return BankDirectory(
            version = parsed.version,
            sources = parsed.sources,
            banks = parsed.banks.map {
                BankInfo(
                    id = it.id,
                    nameFa = it.nameFa,
                    nameEn = it.nameEn,
                    shebaCode = it.shebaCode,
                    bins = it.bins,
                    logoDrawable = it.logoDrawable,
                    mergedInto = it.mergedInto,
                    formerNameFa = it.formerNameFa,
                )
            },
        )
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class BankDirectoryDto(
    val version: Int,
    val sources: List<String> = emptyList(),
    val banks: List<BankInfoDto>,
)

@Serializable
private data class BankInfoDto(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val shebaCode: String,
    val bins: List<String>,
    val logoDrawable: String,
    val mergedInto: String? = null,
    val formerNameFa: String? = null,
)
