package dev.study.mock.oms.order

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "orders")
class Order(
    @Id
    @Column(name = "order_id")
    val orderId: String,

    @Column(name = "oms_order_id", unique = true)
    val omsOrderId: String,

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.RECEIVED,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val orderLines: MutableList<OrderLine> = mutableListOf(),

    @Embedded
    val shipping: ShippingInfo,

    val totalAmount: Long,

    val orderedAt: Instant,

    val receivedAt: Instant = Instant.now()
)

@Embeddable
data class ShippingInfo(
    val recipientName: String,
    val phoneNumber: String,
    val zipCode: String,
    val address: String,
    val addressDetail: String?,
    val memo: String?,
    val entranceCode: String?
)
