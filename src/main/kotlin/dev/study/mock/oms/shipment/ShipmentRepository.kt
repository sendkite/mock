package dev.study.mock.oms.shipment

import org.springframework.data.jpa.repository.JpaRepository

interface ShipmentRepository : JpaRepository<Shipment, String> {
    fun findByOrderId(orderId: String): List<Shipment>
}
