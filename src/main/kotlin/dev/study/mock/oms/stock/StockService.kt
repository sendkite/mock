package dev.study.mock.oms.stock

import dev.study.mock.oms.client.MallApiClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class StockService(
    private val stockRepository: StockRepository,
    private val mallApiClient: MallApiClient
) {
    @Transactional
    fun updateStock(productId: String, optionId: String, quantity: Int): Stock {
        val stock = stockRepository.findByProductIdAndOptionId(productId, optionId)
            ?: Stock(productId, optionId, 0)

        stock.availableQuantity = quantity
        val saved = stockRepository.save(stock)

        // 자사몰에 재고 변경 전송
        sendStockToMall(saved)

        return saved
    }

    @Transactional
    fun adjustStock(productId: String, optionId: String, delta: Int): Stock {
        val stock = stockRepository.findByProductIdAndOptionId(productId, optionId)
            ?: Stock(productId, optionId, 0)

        stock.availableQuantity += delta
        if (stock.availableQuantity < 0) {
            stock.availableQuantity = 0
        }

        val saved = stockRepository.save(stock)

        // 자사몰에 재고 변경 전송
        sendStockToMall(saved)

        return saved
    }

    @Transactional(readOnly = true)
    fun getStock(productId: String, optionId: String): Stock? {
        return stockRepository.findByProductIdAndOptionId(productId, optionId)
    }

    @Transactional(readOnly = true)
    fun getAllStocks(): List<Stock> {
        return stockRepository.findAll()
    }

    fun sendStockToMall(stock: Stock) {
        try {
            mallApiClient.syncStock(
                StockSyncRequest(
                    stocks = listOf(
                        StockItem(
                            productId = stock.productId,
                            optionId = stock.optionId,
                            availableQuantity = stock.availableQuantity
                        )
                    ),
                    syncedAt = Instant.now()
                )
            )
        } catch (e: Exception) {
            // 전송 실패 시 로그만 남기고 계속 진행 (실제로는 재시도 큐 등 구현 필요)
            println("Failed to sync stock to mall: ${e.message}")
        }
    }

    fun sendAllStocksToMall() {
        val stocks = stockRepository.findAll()
        if (stocks.isEmpty()) return

        try {
            mallApiClient.syncStock(
                StockSyncRequest(
                    stocks = stocks.map {
                        StockItem(
                            productId = it.productId,
                            optionId = it.optionId,
                            availableQuantity = it.availableQuantity
                        )
                    },
                    syncedAt = Instant.now()
                )
            )
        } catch (e: Exception) {
            println("Failed to sync all stocks to mall: ${e.message}")
        }
    }
}
