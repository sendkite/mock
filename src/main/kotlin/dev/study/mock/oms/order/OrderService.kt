package dev.study.mock.oms.order

import dev.study.mock.oms.stock.StockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val stockRepository: StockRepository
) {
    @Transactional
    fun createOrder(request: CreateOrderRequest): CreateOrderResponse {
        // 1. 중복 주문 체크
        if (orderRepository.existsByOrderId(request.orderId)) {
            throw DuplicateOrderException(request.orderId)
        }

        // 2. 재고 체크
        for (line in request.orderLines) {
            val stock = stockRepository.findByProductIdAndOptionId(line.productId, line.optionId)
            if (stock != null && stock.availableQuantity < line.quantity) {
                throw InsufficientStockException(
                    productId = line.productId,
                    optionId = line.optionId,
                    requested = line.quantity,
                    available = stock.availableQuantity
                )
            }
        }

        // 3. 주문 생성
        val omsOrderId = "OMS-ORD-${UUID.randomUUID().toString().take(8).uppercase()}"
        val order = Order(
            orderId = request.orderId,
            omsOrderId = omsOrderId,
            shipping = ShippingInfo(
                recipientName = request.shipping.recipientName,
                phoneNumber = request.shipping.phoneNumber,
                zipCode = request.shipping.zipCode,
                address = request.shipping.address,
                addressDetail = request.shipping.addressDetail,
                memo = request.shipping.memo,
                entranceCode = request.shipping.entranceCode
            ),
            totalAmount = request.totalAmount,
            orderedAt = request.orderedAt
        )

        // 4. 주문 라인 추가
        request.orderLines.forEach { lineRequest ->
            val orderLine = OrderLine(
                lineId = lineRequest.lineId,
                productId = lineRequest.productId,
                optionId = lineRequest.optionId,
                productName = lineRequest.productName,
                optionName = lineRequest.optionName,
                quantity = lineRequest.quantity,
                unitPrice = lineRequest.unitPrice,
                totalPrice = lineRequest.totalPrice
            )
            orderLine.order = order
            order.orderLines.add(orderLine)
        }

        // 5. 재고 차감
        for (line in request.orderLines) {
            val stock = stockRepository.findByProductIdAndOptionId(line.productId, line.optionId)
            stock?.let {
                it.availableQuantity -= line.quantity
                stockRepository.save(it)
            }
        }

        val savedOrder = orderRepository.save(order)

        return CreateOrderResponse(
            orderId = savedOrder.orderId,
            omsOrderId = savedOrder.omsOrderId,
            status = savedOrder.status,
            receivedAt = savedOrder.receivedAt
        )
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: String): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException(orderId) }

        return order.toResponse()
    }

    @Transactional
    fun updateOrderStatus(orderId: String, newStatus: OrderStatus): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException(orderId) }

        order.status = newStatus
        return orderRepository.save(order).toResponse()
    }

    private fun Order.toResponse() = OrderResponse(
        orderId = orderId,
        omsOrderId = omsOrderId,
        status = status,
        orderLines = orderLines.map { line ->
            OrderLineResponse(
                lineId = line.lineId,
                productId = line.productId,
                optionId = line.optionId,
                productName = line.productName,
                optionName = line.optionName,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                totalPrice = line.totalPrice
            )
        },
        shipping = ShippingResponse(
            recipientName = shipping.recipientName,
            phoneNumber = shipping.phoneNumber,
            zipCode = shipping.zipCode,
            address = shipping.address,
            addressDetail = shipping.addressDetail,
            memo = shipping.memo,
            entranceCode = shipping.entranceCode
        ),
        totalAmount = totalAmount,
        orderedAt = orderedAt,
        receivedAt = receivedAt
    )
}

// Exceptions
class DuplicateOrderException(orderId: String) : RuntimeException("Order already exists: $orderId")
class OrderNotFoundException(orderId: String) : RuntimeException("Order not found: $orderId")
class InsufficientStockException(
    val productId: String,
    val optionId: String,
    val requested: Int,
    val available: Int
) : RuntimeException("Not enough stock for $productId/$optionId")
