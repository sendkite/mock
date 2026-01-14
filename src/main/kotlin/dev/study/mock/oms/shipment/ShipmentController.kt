package dev.study.mock.oms.shipment

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/shipments")
class ShipmentController(
    private val shipmentService: ShipmentService
) {
    @PostMapping
    fun createShipment(@RequestBody request: CreateShipmentRequest): ResponseEntity<ShipmentResponse> {
        val shipment = shipmentService.createShipment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(shipment.toResponse())
    }

    @GetMapping("/{shipmentId}")
    fun getShipment(@PathVariable shipmentId: String): ResponseEntity<ShipmentResponse> {
        val shipment = shipmentService.getShipment(shipmentId)
        return ResponseEntity.ok(shipment.toResponse())
    }

    @GetMapping
    fun getShipmentsByOrderId(@RequestParam orderId: String): ResponseEntity<List<ShipmentResponse>> {
        val shipments = shipmentService.getShipmentsByOrderId(orderId)
        return ResponseEntity.ok(shipments.map { it.toResponse() })
    }

    @PatchMapping("/{shipmentId}/status")
    fun updateShipmentStatus(
        @PathVariable shipmentId: String,
        @RequestBody request: UpdateShipmentStatusRequest
    ): ResponseEntity<ShipmentResponse> {
        val shipment = shipmentService.updateShipmentStatus(shipmentId, request.status)
        return ResponseEntity.ok(shipment.toResponse())
    }

    private fun Shipment.toResponse() = ShipmentResponse(
        shipmentId = shipmentId,
        orderId = orderId,
        carrierCode = carrierCode,
        trackingNumber = trackingNumber,
        status = status,
        orderLineIds = orderLineIds,
        statusUpdatedAt = statusUpdatedAt
    )
}

data class UpdateShipmentStatusRequest(val status: ShipmentStatus)

data class ShipmentResponse(
    val shipmentId: String,
    val orderId: String,
    val carrierCode: String,
    val trackingNumber: String,
    val status: ShipmentStatus,
    val orderLineIds: List<String>,
    val statusUpdatedAt: java.time.Instant
)
