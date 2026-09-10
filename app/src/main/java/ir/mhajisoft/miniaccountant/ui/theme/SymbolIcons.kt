package ir.mhajisoft.miniaccountant.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Material Symbols Rounded-style vectors for the category grid.
 * autoMirrored is only applied to directional icons (swap / back).
 */
object SymbolIcons {
    val Home = Icons.Rounded.Home
    val Receipt = roundedSymbol("receipt") { receipt() }
    val Chart = roundedSymbol("chart") { trend() }
    val More = roundedSymbol("more") { more() }
    val Add = Icons.Rounded.Add
    val Back = Icons.AutoMirrored.Rounded.ArrowBack
    val Wallet = roundedSymbol("wallet") { wallet() }
    val People = roundedSymbol("people") { people() }
    val Category = roundedSymbol("category") { category() }
    val Card = roundedSymbol("card") { card() }

    fun byKey(key: String): ImageVector = cache.getOrPut(key) {
        when (key) {
            "restaurant", "local_dining", "coffee" -> roundedSymbol("restaurant") { dining() }
            "directions_car", "fuel" -> roundedSymbol("car") { car() }
            "home" -> Home
            "bolt", "wifi" -> roundedSymbol("bolt") { bolt() }
            "health_and_safety", "spa" -> roundedSymbol("health") { health() }
            "school", "menu_book" -> roundedSymbol("school") { school() }
            "checkroom" -> roundedSymbol("checkroom") { checkroom() }
            "sports_esports" -> roundedSymbol("games") { games() }
            "shopping_bag" -> roundedSymbol("bag") { bag() }
            "flight" -> roundedSymbol("flight") { flight() }
            "family_restroom", "child_care", "people" -> People
            "policy" -> roundedSymbol("policy") { shield() }
            "volunteer_activism" -> roundedSymbol("charity") { favorite() }
            "handyman" -> roundedSymbol("repair") { build() }
            "more_horiz" -> More
            "payments", "add_card", "work" -> Card
            "storefront" -> roundedSymbol("store") { store() }
            "card_giftcard" -> roundedSymbol("gift") { gift() }
            "trending_up", "star" -> roundedSymbol("trend") { trend() }
            "swap_horiz" -> swap()
            "receipt_long" -> Receipt
            "account_balance", "wallet" -> Wallet
            "phone" -> roundedSymbol("phone") { phone() }
            "pets" -> roundedSymbol("pets") { pets() }
            "music_note" -> roundedSymbol("music") { music() }
            "water_drop" -> roundedSymbol("water") { water() }
            else -> Category
        }
    }

    val customIconKeys = listOf(
        "restaurant", "coffee", "directions_car", "fuel", "home", "bolt",
        "health_and_safety", "spa", "school", "menu_book", "checkroom",
        "sports_esports", "shopping_bag", "flight", "family_restroom",
        "child_care", "policy", "volunteer_activism", "handyman", "more_horiz",
        "payments", "work", "storefront", "card_giftcard", "trending_up",
        "star", "phone", "pets", "music_note", "water_drop", "wifi", "account_balance",
    )

    val colorPack = listOf(
        0xFF0B6E4FL, 0xFFB42318L, 0xFF1F3A5FL, 0xFFCA8A04L, 0xFF7C3AEDL,
        0xFFEA580CL, 0xFF0D9488L, 0xFFDB2777L, 0xFF0284C7L, 0xFF16A34AL,
        0xFF9A3412L, 0xFF57534EL, 0xFF4F46E5L, 0xFFE11D48L, 0xFF334155L, 0xFF0F766EL,
    )

    private val cache = mutableMapOf<String, ImageVector>()

    private fun roundedSymbol(name: String, add: ImageVector.Builder.() -> Unit): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        builder.add()
        return builder.build()
    }

    private fun ImageVector.Builder.p(d: PathBuilder.() -> Unit) {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) { d() }
    }

    private fun ImageVector.Builder.dining() = p {
        moveTo(8f, 2f); verticalLineTo(22f); horizontalLineTo(6f); verticalLineTo(2f); close()
        moveTo(18f, 6f); curveTo(16f, 6f, 14f, 8f, 14f, 12f); verticalLineTo(22f); horizontalLineTo(16f)
        verticalLineTo(12f); curveTo(16f, 10f, 17f, 8f, 18f, 8f); close()
    }

    private fun ImageVector.Builder.car() = p {
        moveTo(5f, 11f); lineTo(6.5f, 6.5f); curveTo(6.8f, 5.6f, 7.6f, 5f, 8.5f, 5f); horizontalLineTo(15.5f)
        curveTo(16.4f, 5f, 17.2f, 5.6f, 17.5f, 6.5f); lineTo(19f, 11f); verticalLineTo(19f)
        curveTo(19f, 19.6f, 18.6f, 20f, 18f, 20f); horizontalLineTo(17f); curveTo(16.4f, 20f, 16f, 19.6f, 16f, 19f)
        verticalLineTo(18f); horizontalLineTo(8f); verticalLineTo(19f); curveTo(8f, 19.6f, 7.6f, 20f, 7f, 20f)
        horizontalLineTo(6f); curveTo(5.4f, 20f, 5f, 19.6f, 5f, 19f); close()
    }

    private fun ImageVector.Builder.bolt() = p {
        moveTo(11f, 21f); lineTo(13f, 14f); horizontalLineTo(18f); lineTo(10f, 3f); horizontalLineTo(8f)
        lineTo(10f, 10f); horizontalLineTo(5f); close()
    }

    private fun ImageVector.Builder.health() = p {
        moveTo(12f, 2f); lineTo(4f, 5f); verticalLineTo(11f); curveTo(4f, 16f, 7.5f, 20.5f, 12f, 22f)
        curveTo(16.5f, 20.5f, 20f, 16f, 20f, 11f); verticalLineTo(5f); close()
        moveTo(11f, 8f); horizontalLineTo(13f); verticalLineTo(11f); horizontalLineTo(16f); verticalLineTo(13f)
        horizontalLineTo(13f); verticalLineTo(16f); horizontalLineTo(11f); verticalLineTo(13f); horizontalLineTo(8f)
        verticalLineTo(11f); horizontalLineTo(11f); close()
    }

    private fun ImageVector.Builder.school() = p {
        moveTo(12f, 3f); lineTo(1f, 9f); lineTo(12f, 15f); lineTo(21f, 10.1f); verticalLineTo(17f); horizontalLineTo(23f)
        verticalLineTo(9f); close()
    }

    private fun ImageVector.Builder.checkroom() = p {
        moveTo(12f, 2f); curveTo(10f, 2f, 8.5f, 3.5f, 8.5f, 5.5f); verticalLineTo(7f); horizontalLineTo(6f)
        verticalLineTo(21f); horizontalLineTo(18f); verticalLineTo(7f); horizontalLineTo(15.5f); verticalLineTo(5.5f)
        curveTo(15.5f, 3.5f, 14f, 2f, 12f, 2f); close()
    }

    private fun ImageVector.Builder.games() = p {
        moveTo(21f, 6f); horizontalLineTo(3f); curveTo(1.9f, 6f, 1f, 6.9f, 1f, 8f); verticalLineTo(16f)
        curveTo(1f, 17.1f, 1.9f, 18f, 3f, 18f); horizontalLineTo(21f); curveTo(22.1f, 18f, 23f, 17.1f, 23f, 16f)
        verticalLineTo(8f); curveTo(23f, 6.9f, 22.1f, 6f, 21f, 6f); close()
    }

    private fun ImageVector.Builder.bag() = p {
        moveTo(18f, 6f); horizontalLineTo(16f); curveTo(16f, 3.8f, 14.2f, 2f, 12f, 2f)
        curveTo(9.8f, 2f, 8f, 3.8f, 8f, 6f); horizontalLineTo(6f); curveTo(4.9f, 6f, 4f, 6.9f, 4f, 8f)
        verticalLineTo(20f); curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f); horizontalLineTo(18f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f); verticalLineTo(8f); curveTo(20f, 6.9f, 19.1f, 6f, 18f, 6f); close()
    }

    private fun ImageVector.Builder.flight() = p {
        moveTo(21f, 16f); verticalLineTo(14f); lineTo(13f, 9f); verticalLineTo(3.5f)
        curveTo(13f, 2.7f, 12.3f, 2f, 11.5f, 2f); curveTo(10.7f, 2f, 10f, 2.7f, 10f, 3.5f)
        verticalLineTo(9f); lineTo(2f, 14f); verticalLineTo(16f); lineTo(10f, 13.5f); verticalLineTo(19f)
        lineTo(8f, 20.5f); verticalLineTo(22f); lineTo(11.5f, 21f); lineTo(15f, 22f); verticalLineTo(20.5f)
        lineTo(13f, 19f); verticalLineTo(13.5f); close()
    }

    private fun ImageVector.Builder.shield() = p {
        moveTo(12f, 1f); lineTo(3f, 5f); verticalLineTo(11f); curveTo(3f, 16.5f, 6.8f, 21.7f, 12f, 23f)
        curveTo(17.2f, 21.7f, 21f, 16.5f, 21f, 11f); verticalLineTo(5f); close()
    }

    private fun ImageVector.Builder.favorite() = p {
        moveTo(12f, 21f); lineTo(10.6f, 19.7f); curveTo(5.4f, 15f, 2f, 11.9f, 2f, 8.2f)
        curveTo(2f, 5.2f, 4.2f, 3f, 7.2f, 3f); curveTo(8.9f, 3f, 10.5f, 3.8f, 12f, 5.1f)
        curveTo(13.5f, 3.8f, 15.1f, 3f, 16.8f, 3f); curveTo(19.8f, 3f, 22f, 5.2f, 22f, 8.2f)
        curveTo(22f, 11.9f, 18.6f, 15f, 13.4f, 19.7f); close()
    }

    private fun ImageVector.Builder.build() = p {
        moveTo(22.7f, 19f); lineTo(13.6f, 9.9f); curveTo(14.5f, 7.6f, 14f, 4.9f, 12.1f, 3f)
        curveTo(10.1f, 1f, 7.1f, 0.6f, 4.7f, 1.7f); lineTo(9f, 6f); lineTo(6f, 9f); lineTo(1.6f, 4.7f)
        curveTo(0.4f, 7.1f, 0.9f, 10.1f, 2.9f, 12.1f); curveTo(4.8f, 14f, 7.5f, 14.5f, 9.8f, 13.6f)
        lineTo(18.9f, 22.7f); curveTo(19.3f, 23.1f, 19.9f, 23.1f, 20.3f, 22.7f); lineTo(22.6f, 20.4f)
        curveTo(23.1f, 20f, 23.1f, 19.3f, 22.7f, 19f); close()
    }

    private fun ImageVector.Builder.store() = p {
        moveTo(20f, 4f); horizontalLineTo(4f); verticalLineTo(6f); horizontalLineTo(20f); close()
        moveTo(18f, 8f); horizontalLineTo(6f); lineTo(4f, 12f); verticalLineTo(14f); horizontalLineTo(5f)
        verticalLineTo(20f); horizontalLineTo(19f); verticalLineTo(14f); horizontalLineTo(20f); verticalLineTo(12f); close()
    }

    private fun ImageVector.Builder.gift() = p {
        moveTo(20f, 6f); horizontalLineTo(17.8f); curveTo(18f, 5.7f, 18f, 5.3f, 18f, 5f)
        curveTo(18f, 3.3f, 16.7f, 2f, 15f, 2f); curveTo(13.6f, 2f, 12.5f, 2.8f, 12f, 4f)
        curveTo(11.5f, 2.8f, 10.4f, 2f, 9f, 2f); curveTo(7.3f, 2f, 6f, 3.3f, 6f, 5f)
        curveTo(6f, 5.3f, 6f, 5.7f, 6.2f, 6f); horizontalLineTo(4f); verticalLineTo(20f); horizontalLineTo(20f); close()
    }

    private fun ImageVector.Builder.receipt() = p {
        moveTo(18f, 2f); horizontalLineTo(6f); curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f); verticalLineTo(22f)
        lineTo(6.5f, 20f); lineTo(8.5f, 22f); lineTo(10.5f, 20f); lineTo(12.5f, 22f); lineTo(14.5f, 20f)
        lineTo(16.5f, 22f); lineTo(19f, 20f); lineTo(20f, 22f); verticalLineTo(4f)
        curveTo(20f, 2.9f, 19.1f, 2f, 18f, 2f); close()
    }

    private fun ImageVector.Builder.wallet() = p {
        moveTo(21f, 7.3f); verticalLineTo(6f); curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f); horizontalLineTo(5f)
        curveTo(3.9f, 4f, 3.0f, 4.9f, 3f, 6f); verticalLineTo(18f); curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
        horizontalLineTo(19f); curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f); verticalLineTo(16.7f)
        curveTo(21.6f, 16.4f, 22f, 15.7f, 22f, 15f); verticalLineTo(9f)
        curveTo(22f, 8.3f, 21.6f, 7.6f, 21f, 7.3f); close()
    }

    private fun ImageVector.Builder.people() = p {
        moveTo(16f, 11f); curveTo(17.7f, 11f, 19f, 9.7f, 19f, 8f); curveTo(19f, 6.3f, 17.7f, 5f, 16f, 5f)
        curveTo(14.3f, 5f, 13f, 6.3f, 13f, 8f); curveTo(13f, 9.7f, 14.3f, 11f, 16f, 11f); close()
        moveTo(8f, 11f); curveTo(9.7f, 11f, 11f, 9.7f, 11f, 8f); curveTo(11f, 6.3f, 9.7f, 5f, 8f, 5f)
        curveTo(6.3f, 5f, 5f, 6.3f, 5f, 8f); curveTo(5f, 9.7f, 6.3f, 11f, 8f, 11f); close()
        moveTo(8f, 13f); curveTo(5.3f, 13f, 0f, 14.3f, 0f, 17f); verticalLineTo(19f); horizontalLineTo(16f)
        verticalLineTo(17f); curveTo(16f, 14.3f, 10.7f, 13f, 8f, 13f); close()
        moveTo(16f, 13f); curveTo(15.7f, 13f, 15.3f, 13f, 14.9f, 13.1f)
        curveTo(16.2f, 14f, 17f, 15.3f, 17f, 17f); verticalLineTo(19f); horizontalLineTo(24f); verticalLineTo(17f)
        curveTo(24f, 14.3f, 18.7f, 13f, 16f, 13f); close()
    }

    private fun ImageVector.Builder.category() = p {
        moveTo(12f, 2f); lineTo(6.5f, 11f); horizontalLineTo(17.5f); close()
        moveTo(17.5f, 22f); curveTo(19.4f, 22f, 21f, 20.4f, 21f, 18.5f)
        curveTo(21f, 16.6f, 19.4f, 15f, 17.5f, 15f); curveTo(15.6f, 15f, 14f, 16.6f, 14f, 18.5f)
        curveTo(14f, 20.4f, 15.6f, 22f, 17.5f, 22f); close()
        moveTo(3f, 21.5f); horizontalLineTo(11f); verticalLineTo(13.5f); horizontalLineTo(3f); close()
    }

    private fun ImageVector.Builder.card() = p {
        moveTo(20f, 4f); horizontalLineTo(4f); curveTo(2.9f, 4f, 2.0f, 4.9f, 2f, 6f); verticalLineTo(18f)
        curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f); horizontalLineTo(20f); curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
        verticalLineTo(6f); curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f); close()
        moveTo(20f, 18f); horizontalLineTo(4f); verticalLineTo(12f); horizontalLineTo(20f); close()
    }

    private fun ImageVector.Builder.phone() = p {
        moveTo(6.6f, 10.8f); curveTo(7.8f, 13.1f, 9.7f, 15f, 12f, 16.2f); lineTo(13.8f, 14.4f)
        curveTo(14f, 14.2f, 14.3f, 14.1f, 14.6f, 14.2f); curveTo(15.6f, 14.5f, 16.7f, 14.7f, 17.8f, 14.7f)
        curveTo(18.3f, 14.7f, 18.8f, 15.2f, 18.8f, 15.7f); verticalLineTo(18.6f)
        curveTo(18.8f, 19.1f, 18.3f, 19.6f, 17.8f, 19.6f); curveTo(9.6f, 19.6f, 3f, 13f, 3f, 4.8f)
        curveTo(3f, 4.3f, 3.5f, 3.8f, 4f, 3.8f); horizontalLineTo(6.9f); curveTo(7.4f, 3.8f, 7.9f, 4.3f, 7.9f, 4.8f)
        curveTo(7.9f, 5.9f, 8.1f, 7f, 8.4f, 8f); curveTo(8.5f, 8.3f, 8.4f, 8.6f, 8.2f, 8.8f); close()
    }

    private fun ImageVector.Builder.pets() = p {
        moveTo(4.5f, 9f); curveTo(5.3f, 9f, 6f, 8.3f, 6f, 7.5f); curveTo(6f, 6.7f, 5.3f, 6f, 4.5f, 6f)
        curveTo(3.7f, 6f, 3f, 6.7f, 3f, 7.5f); curveTo(3f, 8.3f, 3.7f, 9f, 4.5f, 9f); close()
        moveTo(9f, 6f); curveTo(9.8f, 6f, 10.5f, 5.3f, 10.5f, 4.5f); curveTo(10.5f, 3.7f, 9.8f, 3f, 9f, 3f)
        curveTo(8.2f, 3f, 7.5f, 3.7f, 7.5f, 4.5f); curveTo(7.5f, 5.3f, 8.2f, 6f, 9f, 6f); close()
        moveTo(15f, 6f); curveTo(15.8f, 6f, 16.5f, 5.3f, 16.5f, 4.5f); curveTo(16.5f, 3.7f, 15.8f, 3f, 15f, 3f)
        curveTo(14.2f, 3f, 13.5f, 3.7f, 13.5f, 4.5f); curveTo(13.5f, 5.3f, 14.2f, 6f, 15f, 6f); close()
        moveTo(19.5f, 9f); curveTo(20.3f, 9f, 21f, 8.3f, 21f, 7.5f); curveTo(21f, 6.7f, 20.3f, 6f, 19.5f, 6f)
        curveTo(18.7f, 6f, 18f, 6.7f, 18f, 7.5f); curveTo(18f, 8.3f, 18.7f, 9f, 19.5f, 9f); close()
        moveTo(12f, 14f); curveTo(9f, 14f, 6.5f, 16f, 6.5f, 21f); horizontalLineTo(17.5f)
        curveTo(17.5f, 16f, 15f, 14f, 12f, 14f); close()
    }

    private fun ImageVector.Builder.music() = p {
        moveTo(12f, 3f); verticalLineTo(13.6f); curveTo(11.4f, 13.2f, 10.7f, 13f, 10f, 13f)
        curveTo(8.3f, 13f, 7f, 14.3f, 7f, 16f); curveTo(7f, 17.7f, 8.3f, 19f, 10f, 19f)
        curveTo(11.7f, 19f, 13f, 17.7f, 13f, 16f); verticalLineTo(7f); horizontalLineTo(17f); verticalLineTo(3f); close()
    }

    private fun ImageVector.Builder.water() = p {
        moveTo(12f, 2f); curveTo(12f, 2f, 6f, 9f, 6f, 13f); curveTo(6f, 16.3f, 8.7f, 19f, 12f, 19f)
        curveTo(15.3f, 19f, 18f, 16.3f, 18f, 13f); curveTo(18f, 9f, 12f, 2f, 12f, 2f); close()
    }

    private fun ImageVector.Builder.trend() = p {
        moveTo(16f, 6f); lineTo(18.3f, 8.3f); lineTo(13.4f, 13.2f); lineTo(9.4f, 9.2f); lineTo(2f, 16.6f)
        lineTo(3.4f, 18f); lineTo(9.4f, 12f); lineTo(13.4f, 16f); lineTo(19.7f, 9.7f); lineTo(22f, 12f)
        verticalLineTo(6f); close()
    }

    private fun ImageVector.Builder.more() = p {
        moveTo(6f, 10f); curveTo(4.9f, 10f, 4f, 10.9f, 4f, 12f); curveTo(4f, 13.1f, 4.9f, 14f, 6f, 14f)
        curveTo(7.1f, 14f, 8f, 13.1f, 8f, 12f); curveTo(8f, 10.9f, 7.1f, 10f, 6f, 10f); close()
        moveTo(12f, 10f); curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f); curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
        curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f); curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f); close()
        moveTo(18f, 10f); curveTo(16.9f, 10f, 16f, 10.9f, 16f, 12f); curveTo(16f, 13.1f, 16.9f, 14f, 18f, 14f)
        curveTo(19.1f, 14f, 20f, 13.1f, 20f, 12f); curveTo(20f, 10.9f, 19.1f, 10f, 18f, 10f); close()
    }

    private fun swap(): ImageVector {
        val builder = ImageVector.Builder(
            name = "swap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        )
        builder.path(fill = SolidColor(Color.Black)) {
            moveTo(6.99f, 11f); lineTo(3f, 15f); lineTo(6.99f, 19f); verticalLineTo(16f); horizontalLineTo(14f)
            verticalLineTo(14f); horizontalLineTo(6.99f); close()
            moveTo(17.01f, 5f); verticalLineTo(8f); horizontalLineTo(10f); verticalLineTo(10f); horizontalLineTo(17.01f)
            verticalLineTo(13f); lineTo(21f, 9f); close()
        }
        return builder.build()
    }
}
