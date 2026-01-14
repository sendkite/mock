package dev.study.mock.oms.stock

import jakarta.persistence.*

@Entity
@Table(name = "stocks")
@IdClass(StockId::class)
class Stock(
    @Id
    val productId: String,

    @Id
    val optionId: String,

    var availableQuantity: Int
)

data class StockId(
    val productId: String = "",
    val optionId: String = ""
) : java.io.Serializable
