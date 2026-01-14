package dev.study.mock.oms.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, String> {
    fun existsByOrderId(orderId: String): Boolean
}
