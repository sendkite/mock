package dev.study.mock.oms.shipment

import dev.study.mock.oms.client.MallApiClient
import dev.study.mock.oms.order.OrderRepository
import dev.study.mock.oms.order.OrderStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ShipmentService(
    private val shipmentRepository: ShipmentRepository,
    private val orderRepository: OrderRepository,
    private val mallApiClient: MallApiClient
) {
    @Transactional
    fun createShipment(request: CreateShipmentRequest): Shipment {
        val shipmentId = "SHP-${UUID.randomUUID().toString().take(8).uppercase()}"
        val shipment = Shipment(
            shipmentId = shipmentId,
            orderId = request.orderId,
            carrierCode = request.carrierCode,
            trackingNumber = request.trackingNumber,
            orderLineIds = request.orderLineIds.toMutableList()
        )

        val saved = shipmentRepository.save(shipment)

        // 주문 상태 업데이트
        orderRepository.findById(request.orderId).ifPresent { order ->
            if (order.status == OrderStatus.PACKING || order.status == OrderStatus.PREPARING) {
                order.status = OrderStatus.SHIPPED
                orderRepository.save(order)
            }
        }

        // 자사몰에 배송 정보 전송
        sendShipmentStatusToMall(saved)

        return saved
    }

    @Transactional
    fun updateShipmentStatus(shipmentId: String, newStatus: ShipmentStatus): Shipment {
        val shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow { ShipmentNotFoundException(shipmentId) }

        shipment.status = newStatus
        shipment.statusUpdatedAt = Instant.now()

        val saved = shipmentRepository.save(shipment)

        // 주문 상태 업데이트
        if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
            orderRepository.findById(shipment.orderId).ifPresent { order ->
                order.status = OrderStatus.IN_DELIVERY
                orderRepository.save(order)
            }
        } else if (newStatus == ShipmentStatus.DELIVERED) {
            // 해당 주문의 모든 shipment이 배송완료되었는지 확인
            val allShipments = shipmentRepository.findByOrderId(shipment.orderId)
            if (allShipments.all { it.status == ShipmentStatus.DELIVERED || it.status == ShipmentStatus.CONFIRMED }) {
                orderRepository.findById(shipment.orderId).ifPresent { order ->
                    order.status = OrderStatus.DELIVERED
                    orderRepository.save(order)
                }
            }
        }

        // 자사몰에 배송 상태 전송
        sendShipmentStatusToMall(saved)

        return saved
    }

    @Transactional(readOnly = true)
    fun getShipment(shipmentId: String): Shipment {
        return shipmentRepository.findById(shipmentId)
            .orElseThrow { ShipmentNotFoundException(shipmentId) }
    }

    @Transactional(readOnly = true)
    fun getShipmentsByOrderId(orderId: String): List<Shipment> {
        return shipmentRepository.findByOrderId(orderId)
    }

    private fun sendShipmentStatusToMall(shipment: Shipment) {
        try {
            mallApiClient.sendShipmentStatus(
                ShipmentStatusRequest(
                    orderId = shipment.orderId,
                    shipments = listOf(
                        ShipmentItem(
                            shipmentId = shipment.shipmentId,
                            carrierCode = shipment.carrierCode,
                            trackingNumber = shipment.trackingNumber,
                            status = shipment.status,
                            statusUpdatedAt = shipment.statusUpdatedAt,
                            orderLines = shipment.orderLineIds
                        )
                    )
                )
            )
        } catch (e: Exception) {
            println("Failed to send shipment status to mall: ${e.message}")
        }
    }
}

data class CreateShipmentRequest(
    val orderId: String,
    val carrierCode: String,
    val trackingNumber: String,
    val orderLineIds: List<String>
)

class ShipmentNotFoundException(shipmentId: String) : RuntimeException("Shipment not found: $shipmentId")
