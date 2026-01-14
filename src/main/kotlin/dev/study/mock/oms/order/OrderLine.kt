package dev.study.mock.oms.order

import jakarta.persistence.*

@Entity
@Table(name = "order_lines")
class OrderLine(
    @Id
    @Column(name = "line_id")
    val lineId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var order: Order? = null,

    val productId: String,
    val optionId: String,
    val productName: String,
    val optionName: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long
)
