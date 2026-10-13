package ir.mhajisoft.hesabres

import com.google.common.truth.Truth.assertThat
import ir.mhajisoft.hesabres.domain.ledger.CategoryCatalog
import ir.mhajisoft.hesabres.ui.theme.SymbolIcons
import org.junit.Test

class SymbolIconsTest {
    @Test
    fun byKeyIsSafeToCallTwiceForEveryKeyAndForUnknownKeys() {
        val catalogKeys = CategoryCatalog.systemCategories().map { it.iconKey }.toSet()
        assertThat(SymbolIcons.knownIconKeys).containsAtLeastElementsIn(SymbolIcons.customIconKeys)
        assertThat(SymbolIcons.knownIconKeys).containsAtLeastElementsIn(catalogKeys)

        SymbolIcons.knownIconKeys.forEach { key ->
            val first = SymbolIcons.byKey(key)
            val second = SymbolIcons.byKey(key)
            assertThat(second).isSameInstanceAs(first)
            assertThat(first.defaultWidth.value).isGreaterThan(0f)
        }

        val fallback = SymbolIcons.Category
        repeat(6) { n ->
            assertThat(SymbolIcons.byKey("not-an-icon-$n")).isSameInstanceAs(fallback)
            assertThat(SymbolIcons.byKey("")).isSameInstanceAs(fallback)
        }

        assertThat(SymbolIcons.byKey("coffee")).isSameInstanceAs(SymbolIcons.byKey("restaurant"))
        assertThat(SymbolIcons.byKey("handyman")).isSameInstanceAs(SymbolIcons.byKey("handyman"))
        assertThat(SymbolIcons.Chevron).isSameInstanceAs(SymbolIcons.Chevron)
        assertThat(SymbolIcons.Report.defaultWidth.value).isGreaterThan(0f)
    }
}
