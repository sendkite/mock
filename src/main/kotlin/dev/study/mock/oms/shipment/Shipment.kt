package dev.study.mock.oms.shipment

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "shipments")
class Shipment(
    @Id
    @Column(name = "shipment_id")
    val shipmentId: String,

    val orderId: String,

    val carrierCode: String,

    val trackingNumber: String,

    @Enumerated(EnumType.STRING)
    var status: ShipmentStatus = ShipmentStatus.PICKED_UP,

    @ElementCollection
    @CollectionTable(name = "shipment_order_lines", joinColumns = [JoinColumn(name = "shipment_id")])
    @Column(name = "line_id")
    val orderLineIds: MutableList<String> = mutableListOf(),

    var statusUpdatedAt: Instant = Instant.now()
)
