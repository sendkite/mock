package dev.study.mock.oms.stock

import org.springframework.data.jpa.repository.JpaRepository

interface StockRepository : JpaRepository<Stock, StockId> {
    fun findByProductIdAndOptionId(productId: String, optionId: String): Stock?
}
