package dev.study.mock.oms.shipment

import java.time.Instant

// Request/Response for Mall API (OMS -> Mall)
data class ShipmentStatusRequest(
    val orderId: String,
    val shipments: List<ShipmentItem>
)

data class ShipmentItem(
    val shipmentId: String,
    val carrierCode: String,
    val trackingNumber: String,
    val status: ShipmentStatus,
    val statusUpdatedAt: Instant,
    val orderLines: List<String>
)
