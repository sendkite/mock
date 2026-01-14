package dev.study.mock.oms.stock

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/stocks")
class StockController(
    private val stockService: StockService
) {
    @GetMapping
    fun getAllStocks(): ResponseEntity<List<StockItem>> {
        val stocks = stockService.getAllStocks()
        return ResponseEntity.ok(stocks.map {
            StockItem(it.productId, it.optionId, it.availableQuantity)
        })
    }

    @GetMapping("/{productId}/{optionId}")
    fun getStock(
        @PathVariable productId: String,
        @PathVariable optionId: String
    ): ResponseEntity<StockItem> {
        val stock = stockService.getStock(productId, optionId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(StockItem(stock.productId, stock.optionId, stock.availableQuantity))
    }

    @PutMapping("/{productId}/{optionId}")
    fun updateStock(
        @PathVariable productId: String,
        @PathVariable optionId: String,
        @RequestBody request: UpdateStockRequest
    ): ResponseEntity<StockItem> {
        val stock = stockService.updateStock(productId, optionId, request.availableQuantity)
        return ResponseEntity.ok(StockItem(stock.productId, stock.optionId, stock.availableQuantity))
    }

    @PostMapping("/{productId}/{optionId}/adjust")
    fun adjustStock(
        @PathVariable productId: String,
        @PathVariable optionId: String,
        @RequestBody request: AdjustStockRequest
    ): ResponseEntity<StockItem> {
        val stock = stockService.adjustStock(productId, optionId, request.delta)
        return ResponseEntity.ok(StockItem(stock.productId, stock.optionId, stock.availableQuantity))
    }

    @PostMapping("/sync-to-mall")
    fun syncAllToMall(): ResponseEntity<Unit> {
        stockService.sendAllStocksToMall()
        return ResponseEntity.ok().build()
    }
}

data class UpdateStockRequest(val availableQuantity: Int)
data class AdjustStockRequest(val delta: Int)
