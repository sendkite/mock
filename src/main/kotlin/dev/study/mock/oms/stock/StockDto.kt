package dev.study.mock.oms.stock

import java.time.Instant

// Request/Response for Mall API (OMS -> Mall)
data class StockSyncRequest(
    val stocks: List<StockItem>,
    val syncedAt: Instant
)

data class StockItem(
    val productId: String,
    val optionId: String,
    val availableQuantity: Int
)
