package ir.mhajisoft.miniaccountant.domain.ledger

import ir.mhajisoft.miniaccountant.domain.model.Category
import ir.mhajisoft.miniaccountant.domain.model.CategoryKind

object CategoryCatalog {
    fun systemCategories(): List<Category> = listOf(
        cat(SystemCategories.OPENING_BALANCE_ID, "موجودی اول دوره", "account_balance", 0xFF607D8B, CategoryKind.TRANSFER, 0),
        cat(SystemCategories.TRANSFER_ID, "جابه‌جایی", "swap_horiz", 0xFF78909C, CategoryKind.TRANSFER, 1),
        cat(SystemCategories.FEE_ID, "کارمزد", "receipt_long", 0xFF8D6E63, CategoryKind.EXPENSE, 2),
        cat("sys-food", "خوراک", "restaurant", 0xFFE65100, CategoryKind.EXPENSE, 10),
        cat("sys-transport", "حمل‌ونقل", "directions_car", 0xFF1565C0, CategoryKind.EXPENSE, 11),
        cat("sys-housing", "مسکن", "home", 0xFF6A1B9A, CategoryKind.EXPENSE, 12),
        cat("sys-bills", "قبوض", "bolt", 0xFFF9A825, CategoryKind.EXPENSE, 13),
        cat("sys-health", "سلامت", "health_and_safety", 0xFFC62828, CategoryKind.EXPENSE, 14),
        cat("sys-education", "آموزش", "school", 0xFF283593, CategoryKind.EXPENSE, 15),
        cat("sys-clothing", "پوشاک", "checkroom", 0xFFAD1457, CategoryKind.EXPENSE, 16),
        cat("sys-entertainment", "سرگرمی", "sports_esports", 0xFF00838F, CategoryKind.EXPENSE, 17),
        cat("sys-shopping", "خرید", "shopping_bag", 0xFF2E7D32, CategoryKind.EXPENSE, 18),
        cat("sys-restaurant", "رستوران", "local_dining", 0xFFEF6C00, CategoryKind.EXPENSE, 19),
        cat("sys-travel", "سفر", "flight", 0xFF0277BD, CategoryKind.EXPENSE, 20),
        cat("sys-family", "خانواده", "family_restroom", 0xFF5D4037, CategoryKind.EXPENSE, 21),
        cat("sys-insurance", "بیمه", "policy", 0xFF455A64, CategoryKind.EXPENSE, 22),
        cat("sys-tax", "مالیات", "account_balance", 0xFF37474F, CategoryKind.EXPENSE, 23),
        cat("sys-charity", "نیکوکاری", "volunteer_activism", 0xFF00695C, CategoryKind.EXPENSE, 24),
        cat("sys-repair", "تعمیرات", "handyman", 0xFF546E7A, CategoryKind.EXPENSE, 25),
        cat("sys-other-expense", "سایر هزینه‌ها", "more_horiz", 0xFF757575, CategoryKind.EXPENSE, 26),
        cat("sys-salary", "حقوق", "payments", 0xFF2E7D32, CategoryKind.INCOME, 40),
        cat("sys-business", "کسب‌وکار", "storefront", 0xFF00695C, CategoryKind.INCOME, 41),
        cat("sys-gift", "هدیه", "card_giftcard", 0xFF6A1B9A, CategoryKind.INCOME, 42),
        cat("sys-invest", "سرمایه‌گذاری", "trending_up", 0xFF1565C0, CategoryKind.INCOME, 43),
        cat("sys-other-income", "سایر درآمدها", "add_card", 0xFF558B2F, CategoryKind.INCOME, 44),
    )

    private fun cat(
        id: String,
        name: String,
        icon: String,
        color: Long,
        kind: CategoryKind,
        order: Int,
    ) = Category(
        id = id,
        name = name,
        iconKey = icon,
        color = color,
        kind = kind,
        parentId = null,
        isSystem = true,
        sortOrder = order,
    )
}
