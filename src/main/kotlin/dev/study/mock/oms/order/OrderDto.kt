package dev.study.mock.oms.order

import java.time.Instant

// Request DTOs
data class CreateOrderRequest(
    val orderId: String,
    val orderedAt: Instant,
    val orderLines: List<OrderLineRequest>,
    val shipping: ShippingRequest,
    val totalAmount: Long
)

data class OrderLineRequest(
    val lineId: String,
    val productId: String,
    val optionId: String,
    val productName: String,
    val optionName: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long
)

data class ShippingRequest(
    val recipientName: String,
    val phoneNumber: String,
    val zipCode: String,
    val address: String,
    val addressDetail: String? = null,
    val memo: String? = null,
    val entranceCode: String? = null
)

// Response DTOs
data class CreateOrderResponse(
    val orderId: String,
    val omsOrderId: String,
    val status: OrderStatus,
    val receivedAt: Instant
)

data class OrderResponse(
    val orderId: String,
    val omsOrderId: String,
    val status: OrderStatus,
    val orderLines: List<OrderLineResponse>,
    val shipping: ShippingResponse,
    val totalAmount: Long,
    val orderedAt: Instant,
    val receivedAt: Instant
)

data class OrderLineResponse(
    val lineId: String,
    val productId: String,
    val optionId: String,
    val productName: String,
    val optionName: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long
)

data class ShippingResponse(
    val recipientName: String,
    val phoneNumber: String,
    val zipCode: String,
    val address: String,
    val addressDetail: String?,
    val memo: String?,
    val entranceCode: String?
)

// Error Response
data class ErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, Any>? = null
)
